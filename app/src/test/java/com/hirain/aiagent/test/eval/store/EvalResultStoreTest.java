package com.hirain.aiagent.test.eval.store;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.hirain.aiagent.test.eval.protocol.EvalAction;
import com.hirain.aiagent.test.eval.protocol.EvalBridgeError;
import com.hirain.aiagent.test.eval.protocol.EvalBridgeState;
import com.hirain.aiagent.test.eval.protocol.EvalProtocolCodec;
import com.hirain.aiagent.test.eval.protocol.EvalResultEnvelope;
import java.io.File;
import java.nio.file.Files;
import org.junit.Test;

public class EvalResultStoreTest {
    @Test public void terminalCannotBeOverwritten() throws Exception {
        File root = Files.createTempDirectory("eval-store").toFile();
        EvalResultStore store = new EvalResultStore(root, new EvalProtocolCodec());
        assertTrue(store.write(result(EvalBridgeState.PENDING)));
        assertTrue(store.write(result(EvalBridgeState.RUNNING)));
        assertTrue(store.write(result(EvalBridgeState.TERMINAL)));
        assertFalse(store.write(result(EvalBridgeState.TERMINAL)));
    }
    private static EvalResultEnvelope result(EvalBridgeState state) {
        EvalResultEnvelope value = new EvalResultEnvelope();
        value.correlationId = "case-1";
        value.action = EvalAction.READ_STATE;
        value.bridgeState = state;
        if (state == EvalBridgeState.TERMINAL) value.bridgeError = new EvalBridgeError("COMMAND_INVALID", "TEST", "test");
        return value;
    }
}
