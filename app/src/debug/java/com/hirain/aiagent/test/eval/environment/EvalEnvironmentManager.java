package com.hirain.aiagent.test.eval.environment;

import com.google.gson.JsonObject;
import com.hirain.aiagent.test.EvalModeStateStore;
import com.hirain.aiagent.test.eval.agent.AIAgentEvalDebugClient;
import com.hirain.aiagent.test.eval.protocol.EvalAction;

/** 进程内唯一租约持有者。token/expiry 只在内存中保存且不会交给 UI 或结果模型。 */
public final class EvalEnvironmentManager {
    public interface Callback { void onResponse(JsonObject response); void onFailure(String code, String detail); }
    private final AIAgentEvalDebugClient client;
    private EvalEnvironmentState state = EvalEnvironmentState.IDLE;
    private String leaseToken;
    private Long leaseExpiresAtEpochMs;
    public EvalEnvironmentManager(AIAgentEvalDebugClient client) { this.client = client; }
    public EvalEnvironmentState getState() { return state; }
    public boolean isReady() { return state == EvalEnvironmentState.READY; }
    public void connect() { state = EvalEnvironmentState.CONNECTING; if (!client.connect()) state = EvalEnvironmentState.FAILED; }
    public void onConnectionChanged(boolean connected) { if (!connected && state != EvalEnvironmentState.IDLE) { state = EvalEnvironmentState.FAILED; clearLease(); } }
    public void acquire(String correlationId, Callback callback) {
        if (state == EvalEnvironmentState.READY) { callback.onFailure("BRIDGE_BUSY", "环境租约已持有"); return; }
        state = EvalEnvironmentState.ACQUIRING;
        call(EvalAction.ACQUIRE_ENVIRONMENT, correlationId, null, response -> {
            if (success(response) && response.has("leaseToken")) { leaseToken = response.get("leaseToken").getAsString(); leaseExpiresAtEpochMs = response.has("leaseExpiresAtEpochMs") ? response.get("leaseExpiresAtEpochMs").getAsLong() : null; state = EvalEnvironmentState.READY; EvalModeStateStore.setEvalActive(true); callback.onResponse(response); }
            else { state = EvalEnvironmentState.FAILED; callback.onResponse(response); }
        }, callback);
    }
    public void operate(EvalAction action, String correlationId, JsonObject patch, Callback callback) {
        if (!isReady()) { callback.onFailure("ENVIRONMENT_NOT_READY", "尚未持有有效 Eval 环境"); return; }
        call(action, correlationId, patch, response -> { refreshExpiry(response); callback.onResponse(response); }, callback);
    }
    public void release(String correlationId, Callback callback) {
        if (state == EvalEnvironmentState.IDLE) { callback.onFailure("ENVIRONMENT_NOT_READY", "没有可释放的环境"); return; }
        state = EvalEnvironmentState.RELEASING;
        call(EvalAction.RELEASE_ENVIRONMENT, correlationId, null, response -> { clearLease(); state = EvalEnvironmentState.IDLE; EvalModeStateStore.clear(); callback.onResponse(response); }, callback);
    }
    private void call(EvalAction action, String correlationId, JsonObject patch, java.util.function.Consumer<JsonObject> success, Callback callback) {
        client.execute(action, correlationId, leaseToken, null, patch, new AIAgentEvalDebugClient.Callback() {
            @Override public void onResult(JsonObject response) { success.accept(response); }
            @Override public void onFailure(String code, String detail) { state = EvalEnvironmentState.FAILED; clearLease(); callback.onFailure(code, detail); }
        });
    }
    private void refreshExpiry(JsonObject response) { if (response.has("leaseExpiresAtEpochMs") && !response.get("leaseExpiresAtEpochMs").isJsonNull()) leaseExpiresAtEpochMs = response.get("leaseExpiresAtEpochMs").getAsLong(); }
    private static boolean success(JsonObject response) { return response.has("success") && response.get("success").getAsBoolean(); }
    private void clearLease() { leaseToken = null; leaseExpiresAtEpochMs = null; }
}
