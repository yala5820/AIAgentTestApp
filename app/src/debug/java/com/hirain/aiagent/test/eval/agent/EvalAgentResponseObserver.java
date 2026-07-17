package com.hirain.aiagent.test.eval.agent;

import com.hirain.aiagent.AgentResponse;
import com.hirain.aiagent.IAIAgentServiceListener;

/** Binder 回调只立即复制引用并转交 manager，不在 Binder 线程写文件或读取状态。 */
public final class EvalAgentResponseObserver implements IAIAgentServiceListener {
    public interface Listener { void onResponse(AgentResponse response); }
    private final Listener listener;
    public EvalAgentResponseObserver(Listener listener) { this.listener = listener; }
    @Override public void onAIAgentServiceConnected() { }
    @Override public void onAIAgentServiceDisconnected() { }
    @Override public void onAIResponse(AgentResponse response) { if (response != null) listener.onResponse(response); }
}
