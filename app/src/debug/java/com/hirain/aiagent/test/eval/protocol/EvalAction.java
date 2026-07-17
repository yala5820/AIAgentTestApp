package com.hirain.aiagent.test.eval.protocol;

/** 与电脑端共享 Schema 完全一致的动作名。 */
public enum EvalAction {
    ACQUIRE_ENVIRONMENT, RESET_STATE, APPLY_STATE, READ_STATE, SEND_TEXT,
    CANCEL_REQUEST, CREATE_SESSION, SWITCH_SESSION, DELETE_SESSION, GET_VERSION, RELEASE_ENVIRONMENT
}
