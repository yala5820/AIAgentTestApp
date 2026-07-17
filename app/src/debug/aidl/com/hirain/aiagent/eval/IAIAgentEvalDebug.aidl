package com.hirain.aiagent.eval;

/** TestApp 与 AIAgent 之间的内部 Debug 环境控制契约，不是电脑端 Eval 协议。 */
interface IAIAgentEvalDebug {
    String execute(String requestJson);
}
