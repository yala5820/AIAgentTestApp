package com.hirain.aiagent.test.eval.store;

import com.hirain.aiagent.test.eval.protocol.EvalBridgeState;
import com.hirain.aiagent.test.eval.protocol.EvalProtocolCodec;
import com.hirain.aiagent.test.eval.protocol.EvalResultEnvelope;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;

/** Demo 保留策略：最多 200 条、最长 7 天，且只删除可确认已终态的文件。 */
public final class EvalResultFilePolicy {
    public static final int MAX_RESULTS = 200;
    public static final long MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000;
    private final EvalProtocolCodec codec;
    public EvalResultFilePolicy(EvalProtocolCodec codec) { this.codec = codec; }
    public void cleanup(File directory, long nowMs) {
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;
        Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        int extra = Math.max(0, files.length - MAX_RESULTS);
        for (File file : files) {
            if (extra == 0 && nowMs - file.lastModified() <= MAX_AGE_MS) continue;
            if (isTerminal(file) && file.delete()) extra = Math.max(0, extra - 1);
        }
    }
    private boolean isTerminal(File file) {
        try {
            EvalResultEnvelope result = codec.decodeResult(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
            return result != null && result.bridgeState == EvalBridgeState.TERMINAL;
        } catch (Exception ignored) { return false; }
    }
}
