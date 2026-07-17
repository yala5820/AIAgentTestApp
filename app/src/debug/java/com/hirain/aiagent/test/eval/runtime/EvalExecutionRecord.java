package com.hirain.aiagent.test.eval.runtime;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hirain.aiagent.test.eval.protocol.EvalAction;
import com.hirain.aiagent.test.eval.protocol.EvalBridgeState;
import com.hirain.aiagent.test.eval.protocol.EvalCommand;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ScheduledFuture;

/** 单命令生命周期事实；终态 CAS 防止 callback 与 watchdog 覆盖彼此。 */
public final class EvalExecutionRecord {
    public final String correlationId; public final EvalAction action;
    public volatile EvalBridgeState bridgeState = EvalBridgeState.PENDING;
    public volatile String requestId; public volatile String clientMessageId;
    /** SEND_TEXT 在发送前、回调后采集的证据，避免结果只剩自然语言回复。 */
    public volatile JsonObject beforeVehicleState; public volatile JsonObject afterVehicleState;
    public volatile JsonObject versionFingerprint; public volatile JsonElement agentResponse;
    public volatile EvalCommand.ProtocolVersion protocolVersion;
    public final long createdAtMs = System.currentTimeMillis(); public volatile long completedAtMs;
    public volatile boolean callbackReceived; public final AtomicBoolean terminal = new AtomicBoolean();
    public volatile ScheduledFuture<?> watchdog;
    public EvalExecutionRecord(String correlationId, EvalAction action) { this.correlationId = correlationId; this.action = action; }
}
