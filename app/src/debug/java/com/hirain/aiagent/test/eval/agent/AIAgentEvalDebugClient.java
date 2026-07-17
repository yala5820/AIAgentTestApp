package com.hirain.aiagent.test.eval.agent;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hirain.aiagent.eval.IAIAgentEvalDebug;
import com.hirain.aiagent.test.eval.protocol.EvalAction;
import java.util.concurrent.Executor;

/** AIAgent Debug Service 的最小异步客户端，所有 Binder execute 均在后台 executor 执行。 */
public final class AIAgentEvalDebugClient {
    public interface Callback { void onResult(JsonObject response); void onFailure(String code, String detail); }
    public interface ConnectionObserver { void onConnectionChanged(boolean connected); }
    private final Context context;
    private final Executor binderExecutor;
    private ConnectionObserver observer;
    private IAIAgentEvalDebug service;
    private boolean bound;
    private final EvalDebugServiceConnection connection;

    public AIAgentEvalDebugClient(Context context, Executor binderExecutor, ConnectionObserver observer) {
        this.context = context.getApplicationContext();
        this.binderExecutor = binderExecutor;
        this.observer = observer;
        this.connection = new EvalDebugServiceConnection(new EvalDebugServiceConnection.Listener() {
            @Override public void onConnected(IAIAgentEvalDebug connected) { service = connected; observer.onConnectionChanged(true); }
            @Override public void onDisconnected() { service = null; observer.onConnectionChanged(false); }
        });
    }
    public void setConnectionObserver(ConnectionObserver observer) { this.observer = observer; }
    public boolean connect() {
        if (bound) return service != null;
        Intent intent = new Intent().setComponent(new ComponentName("com.hirain.aiagent", "com.hirain.aiagent.eval.EvalDebugService"));
        bound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
        return bound;
    }
    public void disconnect() { if (bound) context.unbindService(connection); bound = false; service = null; observer.onConnectionChanged(false); }
    public boolean isConnected() { return service != null; }
    public void execute(EvalAction action, String correlationId, String leaseToken, Long ttlMs, JsonObject statePatch, Callback callback) {
        IAIAgentEvalDebug target = service;
        if (target == null) { callback.onFailure("EVAL_SERVICE_UNAVAILABLE", "Debug Eval Service 未连接"); return; }
        JsonObject request = new JsonObject();
        JsonObject version = new JsonObject(); version.addProperty("major", 1); version.addProperty("minor", 0); request.add("protocolVersion", version);
        request.addProperty("operation", action.name()); request.addProperty("correlationId", correlationId);
        if (leaseToken != null) request.addProperty("leaseToken", leaseToken);
        if (ttlMs != null) request.addProperty("ttlMs", ttlMs);
        if (statePatch != null) request.add("statePatch", statePatch);
        binderExecutor.execute(() -> {
            try { callback.onResult(JsonParser.parseString(target.execute(request.toString())).getAsJsonObject()); }
            catch (RemoteException e) { callback.onFailure("EVAL_SERVICE_UNAVAILABLE", e.getClass().getSimpleName()); }
            catch (RuntimeException e) { callback.onFailure("INTERNAL_BRIDGE_ERROR", "非法 Debug 响应"); }
        });
    }
}
