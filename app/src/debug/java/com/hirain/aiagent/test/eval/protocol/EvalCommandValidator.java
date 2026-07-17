package com.hirain.aiagent.test.eval.protocol;

import com.google.gson.JsonObject;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/** 对齐 Schema/Pydantic 的严格校验，拒绝未知顶层字段和隐式 requestId。 */
public final class EvalCommandValidator {
    private static final Pattern SAFE_CORRELATION = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._:-]{0,127}$");
    private static final Set<String> ROOT_KEYS = new HashSet<>(Arrays.asList("protocolVersion", "correlationId", "action", "payload", "createdAt", "timeoutMs", "metadata"));
    public void validate(EvalProtocolCodec.DecodedCommand decoded) { validate(decoded.command, decoded.raw); }
    public void validate(EvalCommand command, JsonObject raw) {
        for (String key : raw.keySet()) require(ROOT_KEYS.contains(key), "未知顶层字段: " + key);
        require(command != null && command.protocolVersion != null, "缺少 protocolVersion");
        if (command.protocolVersion.major != 1 || command.protocolVersion.minor != 0) throw new ValidationException("PROTOCOL_VERSION_UNSUPPORTED", "不支持的 protocolVersion");
        require(command.correlationId != null && SAFE_CORRELATION.matcher(command.correlationId).matches(), "correlationId 必须是安全文件名");
        require(command.action != null && command.payload != null, "缺少 action 或 payload");
        if (command.createdAt != null) try { OffsetDateTime.parse(command.createdAt); } catch (RuntimeException e) { throw invalid("createdAt 必须带 UTC 时区"); }
        if (command.timeoutMs != null) require(command.timeoutMs > 0, "timeoutMs 必须大于 0");
        require(!command.payload.has("requestId") && !command.payload.has("request_id"), "电脑端不得传递 requestId");
        switch (command.action) {
            case SEND_TEXT:
                required(command.payload, "userId", "sessionId", "text");
                if (command.payload.has("inputType")) require("TEXT".equals(command.payload.get("inputType").getAsString()), "SEND_TEXT 只支持 TEXT");
                break;
            case CANCEL_REQUEST:
                required(command.payload, "targetCorrelationId");
                require(!command.correlationId.equals(command.payload.get("targetCorrelationId").getAsString()), "取消目标不得是自身");
                break;
            default: break;
        }
    }
    private static void required(JsonObject object, String... fields) { for (String field : fields) require(object.has(field) && !object.get(field).isJsonNull() && !object.get(field).getAsString().trim().isEmpty(), "payload 缺少 " + field); }
    private static void require(boolean value, String message) { if (!value) throw invalid(message); }
    private static ValidationException invalid(String message) { return new ValidationException("COMMAND_INVALID", message); }
    public static final class ValidationException extends IllegalArgumentException { public final String code; public ValidationException(String code, String message) { super(message); this.code = code; } }
}
