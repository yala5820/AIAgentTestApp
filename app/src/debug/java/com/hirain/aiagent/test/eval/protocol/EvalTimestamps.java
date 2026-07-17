package com.hirain.aiagent.test.eval.protocol;

/** 外层所有时间字段使用 ISO-8601 UTC 字符串，避免与 AgentResponse epoch timestamp 混用。 */
public final class EvalTimestamps {
    public String commandReceivedAt;
    public String startedAt;
    public String agentDispatchedAt;
    public String callbackReceivedAt;
    public String completedAt;
}
