package com.hirain.aiagent.test.eval.protocol;

/** 结果文件只能沿 PENDING、RUNNING、TERMINAL 方向变化。 */
public enum EvalBridgeState { PENDING, RUNNING, TERMINAL }
