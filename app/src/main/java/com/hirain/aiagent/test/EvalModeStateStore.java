package com.hirain.aiagent.test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Eval 与人工交互共用正式 AIAgent 回调时的进程级隔离状态。
 * 不持久化令牌、命令或输出，进程重启即自动回到普通模式。
 */
public final class EvalModeStateStore {
    private static final Set<String> EVAL_CLIENT_MESSAGE_IDS = ConcurrentHashMap.newKeySet();
    private static volatile boolean evalActive;

    private EvalModeStateStore() { }

    public static void setEvalActive(boolean active) { evalActive = active; }
    public static boolean isEvalActive() { return evalActive; }
    public static void registerEvalClientMessageId(String clientMessageId) {
        if (clientMessageId != null && !clientMessageId.isEmpty()) EVAL_CLIENT_MESSAGE_IDS.add(clientMessageId);
    }
    public static boolean ownsClientMessageId(String clientMessageId) { return clientMessageId != null && EVAL_CLIENT_MESSAGE_IDS.contains(clientMessageId); }
    public static void clear() { EVAL_CLIENT_MESSAGE_IDS.clear(); evalActive = false; }
}
