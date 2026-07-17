package com.hirain.aiagent.test.eval.recovery;

import com.hirain.aiagent.test.eval.protocol.EvalBridgeError;
import com.hirain.aiagent.test.eval.protocol.EvalBridgeState;
import com.hirain.aiagent.test.eval.protocol.EvalProtocolCodec;
import com.hirain.aiagent.test.eval.protocol.EvalResultEnvelope;
import com.hirain.aiagent.test.eval.store.EvalResultStore;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;

/** 新进程将遗留 PENDING/RUNNING 收敛为确定终态，不尝试恢复已失效的内存租约。 */
public final class EvalBridgeRecovery {
    public void recover(EvalResultStore store, EvalProtocolCodec codec) {
        File[] files = store.directory().listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;
        for (File file : files) try {
            EvalResultEnvelope result = codec.decodeResult(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
            if (result.bridgeState == EvalBridgeState.PENDING || result.bridgeState == EvalBridgeState.RUNNING) { result.bridgeState = EvalBridgeState.TERMINAL; result.agentResponse = null; result.operationResult = null; result.bridgeError = new EvalBridgeError("BRIDGE_PROCESS_RESTARTED", "RECOVERY", "Eval bridge 进程已重启"); result.timestamps.completedAt = Instant.now().toString(); store.write(result); }
        } catch (Exception ignored) { }
    }
}
