package com.hirain.aiagent.test.eval.runtime;

import android.content.Context;
import com.google.gson.JsonObject;
import com.hirain.aiagent.test.eval.agent.AIAgentEvalDebugClient;
import com.hirain.aiagent.test.eval.agent.EvalDebugProtocolMapper;
import com.hirain.aiagent.test.eval.agent.EvalAgentClient;
import com.hirain.aiagent.test.eval.agent.EvalAgentRequestFactory;
import com.hirain.aiagent.test.eval.agent.EvalAgentResponseMapper;
import com.hirain.aiagent.test.eval.environment.EvalEnvironmentManager;
import com.hirain.aiagent.test.eval.environment.EvalEnvironmentState;
import com.hirain.aiagent.test.eval.protocol.EvalAction;
import com.hirain.aiagent.test.eval.protocol.EvalBridgeError;
import com.hirain.aiagent.test.eval.protocol.EvalBridgeState;
import com.hirain.aiagent.test.eval.protocol.EvalCommand;
import com.hirain.aiagent.test.eval.protocol.EvalCommandValidator;
import com.hirain.aiagent.test.eval.protocol.EvalOperationResult;
import com.hirain.aiagent.test.eval.protocol.EvalProtocolCodec;
import com.hirain.aiagent.test.eval.protocol.EvalResultEnvelope;
import com.hirain.aiagent.test.eval.store.EvalResultStore;
import com.hirain.aiagent.test.eval.session.EvalSessionController;
import com.hirain.aiagent.test.eval.recovery.EvalBridgeRecovery;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Application 单例：Activity 只提交命令，执行器、租约和结果文件不随旋转丢失。 */
public final class EvalBridgeManager {
    public interface Observer { void onUpdate(EvalResultEnvelope result, String path); }
    private static volatile EvalBridgeManager instance;
    public static EvalBridgeManager get(Context context) {
        if (instance == null) synchronized (EvalBridgeManager.class) { if (instance == null) instance = new EvalBridgeManager(context.getApplicationContext()); }
        return instance;
    }
    private final ExecutorService serial = Executors.newSingleThreadExecutor();
    private final ExecutorService binder = Executors.newCachedThreadPool();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final EvalProtocolCodec codec = new EvalProtocolCodec();
    private final EvalCommandValidator validator = new EvalCommandValidator();
    private final EvalResultStore store;
    private final EvalExecutionRegistry registry = new EvalExecutionRegistry();
    private final EvalDebugProtocolMapper mapper = new EvalDebugProtocolMapper();
    private final EvalEnvironmentManager environment;
    private final EvalAgentClient agent;
    private final EvalAgentRequestFactory requestFactory = new EvalAgentRequestFactory();
    private final EvalAgentResponseMapper responseMapper = new EvalAgentResponseMapper();
    private final EvalSessionController sessions = new EvalSessionController();
    private volatile Observer observer;
    private volatile boolean commandRunning;
    private EvalBridgeManager(Context app) {
        store = new EvalResultStore(app.getFilesDir(), codec);
        new EvalBridgeRecovery().recover(store, codec);
        AIAgentEvalDebugClient client = new AIAgentEvalDebugClient(app, binder, connected -> { });
        environment = new EvalEnvironmentManager(client);
        client.setConnectionObserver(connected -> serial.execute(() -> environment.onConnectionChanged(connected)));
        agent = new EvalAgentClient(response -> serial.execute(() -> onAgentResponse(response)));
    }
    public void setObserver(Observer observer) { this.observer = observer; }
    public EvalEnvironmentState environmentState() { return environment.getState(); }
    public void connect() { serial.execute(environment::connect); }
    public void submit(String json) {
        serial.execute(() -> {
            EvalProtocolCodec.DecodedCommand decoded;
            try { decoded = codec.decodeCommand(json); validator.validate(decoded); }
            catch (Exception e) { return; } // 无安全 correlationId 时不能生成无法关联的伪文件。
            EvalCommand command = decoded.command;
            if (commandRunning) { terminal(command, new EvalBridgeError(EvalBridgeErrorCodes.BRIDGE_BUSY, "DISPATCH", "已有命令运行")); return; }
            EvalExecutionRecord record = new EvalExecutionRecord(command.correlationId, command.action);
            if (!registry.register(record)) { terminal(command, new EvalBridgeError(EvalBridgeErrorCodes.BRIDGE_BUSY, "DISPATCH", "correlationId 已存在")); return; }
            commandRunning = true;
            EvalResultEnvelope pending = envelope(command, EvalBridgeState.PENDING); pending.timestamps.commandReceivedAt = now(); write(pending);
            record.bridgeState = EvalBridgeState.RUNNING;
            EvalResultEnvelope running = envelope(command, EvalBridgeState.RUNNING); running.timestamps.commandReceivedAt = pending.timestamps.commandReceivedAt; running.timestamps.startedAt = now(); write(running);
            dispatch(command, record, running);
        });
    }
    private void dispatch(EvalCommand command, EvalExecutionRecord record, EvalResultEnvelope running) {
        if (command.action == EvalAction.ACQUIRE_ENVIRONMENT) environment.acquire(command.correlationId, callback(command, record, running));
        else if (command.action == EvalAction.RELEASE_ENVIRONMENT) environment.release(command.correlationId, callback(command, record, running));
        else if (command.action == EvalAction.RESET_STATE || command.action == EvalAction.APPLY_STATE || command.action == EvalAction.READ_STATE || command.action == EvalAction.GET_VERSION) environment.operate(command.action, command.correlationId, command.action == EvalAction.APPLY_STATE ? command.payload : null, callback(command, record, running));
        else if (command.action == EvalAction.SEND_TEXT) {
            if (!environment.isReady()) { terminal(command, new EvalBridgeError(EvalBridgeErrorCodes.ENVIRONMENT_NOT_READY, "SEND_TEXT", "尚未持有有效 Eval 环境")); return; }
            com.hirain.aiagent.AgentRequest request = requestFactory.create(command.payload, command.correlationId);
            record.requestId = request.getRequestId(); record.clientMessageId = command.correlationId;
            com.hirain.aiagent.test.EvalModeStateStore.registerEvalClientMessageId(command.correlationId);
            if (agent.send(request) != 0) terminal(command, new EvalBridgeError("AGENT_SERVICE_UNAVAILABLE", "SEND_TEXT", "正式 AIAgent Service 未连接"));
            else scheduleWatchdog(command, record, command.timeoutMs == null ? 40_000L : Math.max(35_000L, Math.min(60_000L, command.timeoutMs)));
        } else if (command.action == EvalAction.CANCEL_REQUEST) {
            EvalExecutionRecord target = registry.get(command.payload.get("targetCorrelationId").getAsString());
            if (target == null || target.requestId == null) { terminal(command, new EvalBridgeError("REQUEST_MAPPING_MISSING", "CANCEL", "未找到目标请求")); return; }
            com.hirain.aiagent.CancelRequestResult cancel = agent.cancel(target.requestId, "cancelled_by_eval");
            EvalResultEnvelope result = envelope(command, EvalBridgeState.TERMINAL); result.timestamps.completedAt = now(); result.operationResult = new EvalOperationResult(); result.operationResult.action = EvalAction.CANCEL_REQUEST; result.operationResult.success = cancel != null && cancel.isSuccess(); result.operationResult.status = cancel == null ? "SERVICE_UNAVAILABLE" : cancel.getStatus(); result.operationResult.data = new JsonObject(); result.operationResult.data.addProperty("targetCorrelationId", target.correlationId); result.operationResult.data.addProperty("accepted", cancel != null && cancel.isSuccess()); result.operationResult.data.addProperty("status", result.operationResult.status); terminal(record, result);
        } else if (command.action == EvalAction.CREATE_SESSION || command.action == EvalAction.SWITCH_SESSION || command.action == EvalAction.DELETE_SESSION) {
            JsonObject raw = sessions.execute(command.action, command.payload); EvalResultEnvelope result = envelope(command, EvalBridgeState.TERMINAL); result.timestamps.completedAt = now(); result.operationResult = new EvalOperationResult(); result.operationResult.action = command.action; result.operationResult.success = raw.get("success").getAsBoolean(); result.operationResult.status = raw.get("status").getAsString(); result.operationResult.error = raw.has("error") ? raw.get("error").getAsString() : null; result.operationResult.data = raw.getAsJsonObject("data"); terminal(record, result);
        } else terminal(command, new EvalBridgeError("COMMAND_NOT_AVAILABLE", "DISPATCH", "未知动作"));
    }
    private EvalEnvironmentManager.Callback callback(EvalCommand command, EvalExecutionRecord record, EvalResultEnvelope running) {
        return new EvalEnvironmentManager.Callback() {
            @Override public void onResponse(JsonObject response) { serial.execute(() -> { EvalResultEnvelope result = envelope(command, EvalBridgeState.TERMINAL); result.timestamps = running.timestamps; result.timestamps.completedAt = now(); result.operationResult = mapper.operation(command.action, response); if (response.has("versionFingerprint")) result.versionFingerprint = response.getAsJsonObject("versionFingerprint"); terminal(record, result); }); }
            @Override public void onFailure(String code, String detail) { serial.execute(() -> terminal(command, new EvalBridgeError(code, "ENVIRONMENT", detail))); }
        };
    }
    private void terminal(EvalCommand command, EvalBridgeError error) { EvalResultEnvelope result = envelope(command, EvalBridgeState.TERMINAL); result.timestamps.completedAt = now(); result.bridgeError = error; write(result); commandRunning = false; }
    private void terminal(EvalExecutionRecord record, EvalResultEnvelope result) { if (!record.terminal.compareAndSet(false, true)) return; if (record.watchdog != null) record.watchdog.cancel(false); record.bridgeState = EvalBridgeState.TERMINAL; record.completedAtMs = System.currentTimeMillis(); write(result); commandRunning = false; }
    private EvalResultEnvelope envelope(EvalCommand command, EvalBridgeState state) { EvalResultEnvelope result = new EvalResultEnvelope(); result.protocolVersion = command.protocolVersion; result.correlationId = command.correlationId; result.action = command.action; result.bridgeState = state; return result; }
    private void write(EvalResultEnvelope result) { try { store.write(result); Observer target = observer; if (target != null) target.onUpdate(result, store.fileFor(result.correlationId).getAbsolutePath()); } catch (Exception ignored) { } }
    private static String now() { return Instant.now().toString(); }
    private void onAgentResponse(com.hirain.aiagent.AgentResponse response) {
        if (response == null) return;
        EvalExecutionRecord record = registry.get(response.getClientMessageId());
        if (record == null || record.requestId == null || !record.requestId.equals(response.getRequestId())) return;
        record.callbackReceived = true;
        EvalResultEnvelope result = new EvalResultEnvelope(); result.correlationId = record.correlationId; result.action = EvalAction.SEND_TEXT; result.bridgeState = EvalBridgeState.TERMINAL; result.requestId = record.requestId; result.agentResponse = responseMapper.map(response); result.timestamps.callbackReceivedAt = now(); result.timestamps.completedAt = now(); terminal(record, result);
    }
    private void scheduleWatchdog(EvalCommand command, EvalExecutionRecord record, long timeoutMs) {
        record.watchdog = scheduler.schedule(() -> serial.execute(() -> {
            if (record.terminal.get()) return;
            agent.cancel(record.requestId, "eval_bridge_watchdog");
            scheduler.schedule(() -> serial.execute(() -> {
                if (!record.terminal.get()) { EvalResultEnvelope result = envelope(command, EvalBridgeState.TERMINAL); result.requestId = record.requestId; result.timestamps.completedAt = now(); result.bridgeError = new EvalBridgeError("AGENT_RESPONSE_MISSING", "WATCHDOG", "取消宽限期后仍未收到真实 AgentResponse"); terminal(record, result); }
            }), 2, TimeUnit.SECONDS);
        }), timeoutMs, TimeUnit.MILLISECONDS);
    }
}
