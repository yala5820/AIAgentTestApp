package com.hirain.aiagent.test.eval.runtime;

import com.hirain.aiagent.test.eval.protocol.EvalAction;
import com.hirain.aiagent.test.eval.protocol.EvalBridgeState;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ScheduledFuture;

/** 单命令生命周期事实；终态 CAS 防止 callback 与 watchdog 覆盖彼此。 */
public final class EvalExecutionRecord {
    public final String correlationId; public final EvalAction action;
    public volatile EvalBridgeState bridgeState = EvalBridgeState.PENDING;
    public volatile String requestId; public volatile String clientMessageId;
    public final long createdAtMs = System.currentTimeMillis(); public volatile long completedAtMs;
    public volatile boolean callbackReceived; public final AtomicBoolean terminal = new AtomicBoolean();
    public volatile ScheduledFuture<?> watchdog;
    public EvalExecutionRecord(String correlationId, EvalAction action) { this.correlationId = correlationId; this.action = action; }
}
