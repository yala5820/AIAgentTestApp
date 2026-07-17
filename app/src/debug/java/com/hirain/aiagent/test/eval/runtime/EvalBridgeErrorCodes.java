package com.hirain.aiagent.test.eval.runtime;

/** 电脑端可稳定判断的桥接错误码。 */
public final class EvalBridgeErrorCodes {
    public static final String COMMAND_INVALID = "COMMAND_INVALID";
    public static final String BRIDGE_BUSY = "BRIDGE_BUSY";
    public static final String ENVIRONMENT_NOT_READY = "ENVIRONMENT_NOT_READY";
    public static final String EVAL_SERVICE_UNAVAILABLE = "EVAL_SERVICE_UNAVAILABLE";
    public static final String INTERNAL_BRIDGE_ERROR = "INTERNAL_BRIDGE_ERROR";
    private EvalBridgeErrorCodes() { }
}
