package com.hirain.aiagent.test.eval.protocol;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Eval 外层 JSON 的唯一编解码入口，先保留原始对象供严格字段校验。 */
public final class EvalProtocolCodec {
    private final Gson gson = new Gson();
    public DecodedCommand decodeCommand(String json) {
        JsonObject raw = JsonParser.parseString(json).getAsJsonObject();
        return new DecodedCommand(gson.fromJson(raw, EvalCommand.class), raw);
    }
    public EvalResultEnvelope decodeResult(String json) { return gson.fromJson(json, EvalResultEnvelope.class); }
    public String encode(EvalResultEnvelope value) { return gson.toJson(value); }
    public static final class DecodedCommand {
        public final EvalCommand command;
        public final JsonObject raw;
        DecodedCommand(EvalCommand command, JsonObject raw) { this.command = command; this.raw = raw; }
    }
}
