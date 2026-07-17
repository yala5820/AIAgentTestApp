package com.hirain.aiagent.test.eval.protocol;

/**
 * 桥接失败的稳定、机器可读事实；不会记录令牌、完整 Prompt 或完整模型输出。
 * 该对象是已确认将由电脑端同步的协议定义。
 */
public final class EvalBridgeError {
    public String code;
    public String stage;
    public String message;
    public String diagnostic;
    public EvalBridgeError() { }
    public EvalBridgeError(String code, String stage, String message) {
        this.code = code;
        this.stage = stage;
        this.message = message;
    }
}
