package com.hirain.aiagent.test.eval.agent;

import com.hirain.aiagent.AIAgent;
import com.hirain.aiagent.AgentRequest;
import com.hirain.aiagent.AgentResponse;
import com.hirain.aiagent.CancelRequestResult;

/** 复用既有 AIAgent Facade 和多 listener，不建立第二套正式服务绑定。 */
public final class EvalAgentClient {
    public interface Callback { void onResponse(AgentResponse response); }
    private final EvalAgentResponseObserver observer;
    public EvalAgentClient(Callback callback) { observer = new EvalAgentResponseObserver(callback::onResponse); AIAgent.getInstance().registerAIAgentLisener(observer); }
    public int send(AgentRequest request) { return AIAgent.getInstance().processAgentRequest(request); }
    public CancelRequestResult cancel(String requestId, String reason) { return AIAgent.getInstance().cancelAgentRequest(requestId, reason); }
}
