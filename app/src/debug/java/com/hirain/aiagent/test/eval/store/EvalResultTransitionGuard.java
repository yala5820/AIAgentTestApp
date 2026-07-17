package com.hirain.aiagent.test.eval.store;

import com.hirain.aiagent.test.eval.protocol.EvalBridgeState;

/** 将 callback、watchdog 与重试竞争收敛成严格单向的状态转换。 */
public final class EvalResultTransitionGuard {
    public boolean canTransition(EvalBridgeState from, EvalBridgeState to) {
        return from == EvalBridgeState.PENDING && (to == EvalBridgeState.RUNNING || to == EvalBridgeState.TERMINAL)
                || from == EvalBridgeState.RUNNING && to == EvalBridgeState.TERMINAL;
    }
}
