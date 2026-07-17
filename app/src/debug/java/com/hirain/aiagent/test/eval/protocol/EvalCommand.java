package com.hirain.aiagent.test.eval.protocol;

import com.google.gson.JsonObject;

/** 电脑端到 TestApp 的外层命令，不包含 TestApp 生成的 requestId。 */
public final class EvalCommand {
    public ProtocolVersion protocolVersion = new ProtocolVersion();
    public String correlationId;
    public EvalAction action;
    public JsonObject payload;
    public String createdAt;
    public Long timeoutMs;
    public JsonObject metadata;

    public static final class ProtocolVersion {
        public int major = 1;
        public int minor = 0;
        public String schemaHash;
    }
}
