package com.hirain.aiagent.test.eval.protocol;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Debug 结果文件的唯一外层模型；内部 leaseToken 不在此模型中出现。 */
public final class EvalResultEnvelope {
    public EvalCommand.ProtocolVersion protocolVersion = new EvalCommand.ProtocolVersion();
    public String correlationId;
    public EvalBridgeState bridgeState;
    public EvalAction action;
    public String requestId;
    public JsonElement agentResponse;
    public EvalOperationResult operationResult;
    public JsonObject beforeVehicleState;
    public JsonObject afterVehicleState;
    public JsonObject versionFingerprint;
    public EvalTimestamps timestamps = new EvalTimestamps();
    public EvalBridgeError bridgeError;
    public JsonObject metadata;
}
