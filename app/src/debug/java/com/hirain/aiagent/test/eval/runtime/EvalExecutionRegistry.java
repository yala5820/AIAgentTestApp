package com.hirain.aiagent.test.eval.runtime;

import java.util.concurrent.ConcurrentHashMap;

/** correlationId 到 TestApp 生成 requestId 的环境生命周期关联表。 */
public final class EvalExecutionRegistry {
    private final ConcurrentHashMap<String, EvalExecutionRecord> records = new ConcurrentHashMap<>();
    public boolean register(EvalExecutionRecord record) { return records.putIfAbsent(record.correlationId, record) == null; }
    public EvalExecutionRecord get(String correlationId) { return records.get(correlationId); }
    public void clear() { records.clear(); }
}
