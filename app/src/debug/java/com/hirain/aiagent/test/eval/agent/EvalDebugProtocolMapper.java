package com.hirain.aiagent.test.eval.agent;

import com.google.gson.JsonObject;
import com.hirain.aiagent.test.eval.protocol.EvalAction;
import com.hirain.aiagent.test.eval.protocol.EvalOperationResult;

/** 将内层 Debug 响应映射为外层 operationResult，并在边界处剥离 leaseToken/expiry。 */
public final class EvalDebugProtocolMapper {
    public EvalOperationResult operation(EvalAction action, JsonObject response) {
        EvalOperationResult result = new EvalOperationResult();
        result.action = action;
        result.success = response.has("success") && response.get("success").getAsBoolean();
        result.status = response.has("status") ? response.get("status").getAsString() : "INVALID_RESPONSE";
        result.error = response.has("errorCode") && !response.get("errorCode").isJsonNull() ? response.get("errorCode").getAsString() : null;
        JsonObject data = new JsonObject();
        if (action == EvalAction.ACQUIRE_ENVIRONMENT || action == EvalAction.RELEASE_ENVIRONMENT) {
            data.addProperty("leaseActive", action == EvalAction.ACQUIRE_ENVIRONMENT && result.success);
        } else if (action == EvalAction.GET_VERSION && response.has("versionFingerprint")) {
            data.add("versionFingerprint", response.get("versionFingerprint"));
        } else if (response.has("snapshot") && !response.get("snapshot").isJsonNull()) {
            data.add("snapshot", response.get("snapshot"));
        }
        result.data = data;
        return result;
    }
}
