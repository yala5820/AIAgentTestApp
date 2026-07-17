package com.hirain.aiagent.test.eval.agent;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import com.hirain.aiagent.eval.IAIAgentEvalDebug;

/** 仅转发 Binder 生命周期；租约归 application-scoped manager 管理。 */
final class EvalDebugServiceConnection implements ServiceConnection {
    interface Listener { void onConnected(IAIAgentEvalDebug service); void onDisconnected(); }
    private final Listener listener;
    EvalDebugServiceConnection(Listener listener) { this.listener = listener; }
    @Override public void onServiceConnected(ComponentName name, IBinder binder) { listener.onConnected(IAIAgentEvalDebug.Stub.asInterface(binder)); }
    @Override public void onServiceDisconnected(ComponentName name) { listener.onDisconnected(); }
    @Override public void onBindingDied(ComponentName name) { listener.onDisconnected(); }
    @Override public void onNullBinding(ComponentName name) { listener.onDisconnected(); }
}
