package com.hirain.aiagent.test.eval.agent;

import com.google.gson.JsonObject;
import com.hirain.aiagent.AgentResponse;

/** 保留 AIAgent 业务响应原意，只做外层字段名映射。 */
public final class EvalAgentResponseMapper {
    public JsonObject map(AgentResponse r) {
        JsonObject value = new JsonObject();
        value.addProperty("requestId", r.getRequestId()); value.addProperty("sessionId", r.getSessionId()); value.addProperty("success", r.isSuccess()); value.addProperty("text", r.getText()); value.addProperty("timestamp", r.getTimestamp());
        add(value, "userId", r.getUserId()); add(value, "personaId", r.getPersonaId()); add(value, "status", r.getStatus()); add(value, "errorType", r.getErrorType()); add(value, "errorDetail", r.getErrorDetail()); add(value, "clientMessageId", r.getClientMessageId());
        return value;
    }
    private static void add(JsonObject object, String key, String value) { if (value != null) object.addProperty(key, value); }
}
