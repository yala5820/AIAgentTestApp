package com.hirain.aiagent.test.eval.protocol;

import com.google.gson.JsonObject;

/** 非 SEND_TEXT 动作的结构化事实；data 仅由对应动作 mapper 构造。 */
public final class EvalOperationResult {
    public EvalAction action;
    public boolean success;
    public String status;
    public JsonObject data;
    public String error;
}
