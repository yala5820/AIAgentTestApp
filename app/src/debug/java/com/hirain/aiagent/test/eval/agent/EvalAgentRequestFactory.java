package com.hirain.aiagent.test.eval.agent;

import com.google.gson.JsonObject;
import com.hirain.aiagent.AgentRequest;
import java.util.UUID;

/** 仅构造真实 TEXT 请求；requestId 始终由 TestApp 生成。 */
public final class EvalAgentRequestFactory {
    public AgentRequest create(JsonObject payload, String correlationId) {
        AgentRequest request = new AgentRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setClientMessageId(correlationId);
        request.setUserId(payload.get("userId").getAsString()); request.setSessionId(payload.get("sessionId").getAsString());
        if (payload.has("personaId")) request.setPersonaId(payload.get("personaId").getAsString());
        request.setText(payload.get("text").getAsString()); request.setInputType("TEXT"); request.setSourceApp("com.hirain.aiagent.test"); request.setTimestamp(System.currentTimeMillis());
        return request;
    }
}
