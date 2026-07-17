package com.hirain.aiagent.test.eval.protocol;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class EvalProtocolCodecTest {
    @Test public void decodesKnownCommandWithoutEnumDowngrade() {
        EvalProtocolCodec.DecodedCommand decoded = new EvalProtocolCodec().decodeCommand("{\"protocolVersion\":{\"major\":1,\"minor\":0},\"correlationId\":\"case-1\",\"action\":\"READ_STATE\",\"payload\":{}}");
        assertEquals(EvalAction.READ_STATE, decoded.command.action);
    }
}
