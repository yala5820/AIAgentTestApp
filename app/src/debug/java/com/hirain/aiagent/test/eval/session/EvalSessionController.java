package com.hirain.aiagent.test.eval.session;

import com.google.gson.JsonObject;
import com.hirain.aiagent.AIAgent;
import com.hirain.aiagent.ConversationOperationResult;
import com.hirain.aiagent.ConversationRequest;
import com.hirain.aiagent.test.eval.protocol.EvalAction;

/** 使用命令明确给定的 userId/sessionId，不依赖 MainActivity 当前选择。 */
public final class EvalSessionController {
    public JsonObject execute(EvalAction action, JsonObject payload) {
        String userId = payload.get("userId").getAsString(); String sessionId = payload.get("sessionId").getAsString();
        ConversationOperationResult raw;
        if (action == EvalAction.CREATE_SESSION) { ConversationRequest request = new ConversationRequest(); request.setUserId(userId); request.setSessionId(sessionId); request.setPersonaId(payload.has("personaId") ? payload.get("personaId").getAsString() : null); request.setTitle(payload.has("title") ? payload.get("title").getAsString() : null); request.setSourceApp("com.hirain.aiagent.test"); request.setTimestamp(System.currentTimeMillis()); raw = AIAgent.getInstance().createConversation(request); }
        else if (action == EvalAction.SWITCH_SESSION) raw = AIAgent.getInstance().switchConversation(userId, sessionId);
        else raw = AIAgent.getInstance().deleteConversation(userId, sessionId);
        JsonObject result = new JsonObject(); result.addProperty("success", raw != null && raw.isSuccess()); result.addProperty("status", raw == null ? "SERVICE_UNAVAILABLE" : (raw.getOperation() == null ? "UNKNOWN" : raw.getOperation())); if (raw != null && raw.getErrorDetail() != null) result.addProperty("error", raw.getErrorDetail()); JsonObject data = new JsonObject(); data.addProperty("sessionId", sessionId); data.addProperty("operationStatus", result.get("status").getAsString()); result.add("data", data); return result;
    }
}
