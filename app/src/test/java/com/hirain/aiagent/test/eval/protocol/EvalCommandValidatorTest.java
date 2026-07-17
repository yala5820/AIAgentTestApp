package com.hirain.aiagent.test.eval.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;

public class EvalCommandValidatorTest {
    @Test public void rejectsUnsafeIdAndRequestId() {
        EvalProtocolCodec codec = new EvalProtocolCodec();
        try {
            new EvalCommandValidator().validate(codec.decodeCommand("{\"protocolVersion\":{\"major\":1,\"minor\":0},\"correlationId\":\"../unsafe\",\"action\":\"SEND_TEXT\",\"payload\":{\"userId\":\"u\",\"sessionId\":\"s\",\"text\":\"t\",\"requestId\":\"forbidden\"}}"));
            fail("非法 correlationId 必须拒绝");
        } catch (EvalCommandValidator.ValidationException expected) { assertEquals("COMMAND_INVALID", expected.code); }
    }
    @Test public void rejectsUnsupportedVersion() {
        try {
            new EvalCommandValidator().validate(new EvalProtocolCodec().decodeCommand("{\"protocolVersion\":{\"major\":2,\"minor\":0},\"correlationId\":\"case-1\",\"action\":\"READ_STATE\",\"payload\":{}}"));
            fail("不支持版本必须拒绝");
        } catch (EvalCommandValidator.ValidationException expected) { assertEquals("PROTOCOL_VERSION_UNSUPPORTED", expected.code); }
    }
}
