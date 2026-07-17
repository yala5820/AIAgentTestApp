package com.hirain.aiagent.test.eval.store;

import android.system.Os;
import com.hirain.aiagent.test.eval.protocol.EvalProtocolCodec;
import com.hirain.aiagent.test.eval.protocol.EvalResultEnvelope;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 将结构化结果持久化为 files/eval-results/<correlationId>.json。
 * 临时文件 fsync 后同目录原子替换，任何失败都不会破坏旧的正式结果。
 */
public final class EvalResultStore {
    private final File directory;
    private final EvalProtocolCodec codec;
    private final EvalResultTransitionGuard transitionGuard = new EvalResultTransitionGuard();
    private final EvalResultFilePolicy policy;

    public EvalResultStore(File filesDir, EvalProtocolCodec codec) {
        this.directory = new File(filesDir, "eval-results");
        this.codec = codec;
        this.policy = new EvalResultFilePolicy(codec);
    }
    public synchronized boolean write(EvalResultEnvelope result) throws IOException {
        if (result == null || result.correlationId == null || !result.correlationId.matches("^[A-Za-z0-9][A-Za-z0-9._:-]{0,127}$")) throw new IOException("unsafe correlationId");
        if (!directory.exists() && !directory.mkdirs()) throw new IOException("cannot create eval-results");
        File target = fileFor(result.correlationId);
        EvalResultEnvelope previous = read(target);
        if (previous != null && !transitionGuard.canTransition(previous.bridgeState, result.bridgeState)) return false;
        byte[] body = codec.encode(result).getBytes(StandardCharsets.UTF_8);
        File temporary = new File(directory, "." + result.correlationId + "." + UUID.randomUUID() + ".tmp");
        try (FileOutputStream output = new FileOutputStream(temporary)) {
            output.write(body);
            output.flush();
            output.getFD().sync();
        }
        atomicReplace(temporary, target);
        policy.cleanup(directory, System.currentTimeMillis());
        return true;
    }
    public synchronized EvalResultEnvelope read(String correlationId) throws IOException { return read(fileFor(correlationId)); }
    public File fileFor(String correlationId) { return new File(directory, correlationId + ".json"); }
    public File directory() { return directory; }
    private EvalResultEnvelope read(File target) throws IOException {
        if (!target.exists()) return null;
        try { return codec.decodeResult(new String(Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8)); }
        catch (RuntimeException error) { throw new IOException("invalid stored result", error); }
    }
    private static void atomicReplace(File temporary, File target) throws IOException {
        try {
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            return;
        } catch (Exception ignored) { /* 文件系统可能不支持 ATOMIC_MOVE，继续使用同目录 rename。 */ }
        try { Os.rename(temporary.getAbsolutePath(), target.getAbsolutePath()); }
        catch (Exception error) { throw new IOException("atomic result rename failed", error); }
    }
}
