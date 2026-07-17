package com.hirain.aiagent.test.eval.store;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.hirain.aiagent.test.eval.protocol.EvalBridgeState;
import org.junit.Test;

public class EvalResultTransitionGuardTest {
    @Test public void acceptsOnlyForwardTransitions() {
        EvalResultTransitionGuard guard = new EvalResultTransitionGuard();
        assertTrue(guard.canTransition(EvalBridgeState.PENDING, EvalBridgeState.RUNNING));
        assertTrue(guard.canTransition(EvalBridgeState.PENDING, EvalBridgeState.TERMINAL));
        assertTrue(guard.canTransition(EvalBridgeState.RUNNING, EvalBridgeState.TERMINAL));
        assertFalse(guard.canTransition(EvalBridgeState.TERMINAL, EvalBridgeState.TERMINAL));
    }
}
