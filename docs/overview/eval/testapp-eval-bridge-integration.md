# TestApp Eval Bridge：三端协作与调试说明

本文是 `AIAgentTestApp` 侧的事实性接口说明，供以下两个项目的开发/调试 Agent 使用：

- AIAgent：`D:\code\android\AndroidStudioProjects\AIAgent`
- 电脑端 Eval：`D:\code\android\AndroidStudioProjects\AIAgent_Eval`

本桥接层只存在于 **TestApp Debug 变体**。它不替代 AIAgent 正式请求链路，也不是电脑端直接调用 AIAgent 的第二套协议。

## 1. 三端职责与数据流

```text
电脑端 Eval 系统
  │  EvalCommand JSON（ADB intent，URL-safe Base64）
  ▼
TestApp Debug / EvalBridgeActivity
  │  外层协议校验、串行状态机、结果落盘
  ├─► AIAgent Debug Eval Service（内部 AIDL）
  │      获取租约 / 重置、读写虚拟车辆状态 / 读取版本
  └─► AIAgent 正式 Service（既有 Facade/AIDL）
         SEND_TEXT / cancel / 会话操作 / AgentResponse
  │
  ├─► Debug ContentProvider（只读结果 JSON） ─► 电脑端 Eval
  └─► AIAgent Trace ─► Phoenix（按 requestId、clientMessageId 核验）
```

职责边界：

| 端 | 负责内容 | 不负责内容 |
|---|---|---|
| 电脑端 Eval | 生成合法外层命令、拉取结果、Schema 校验、规则/LLM Judge、Phoenix 查询 | 不生成 requestId、不持有环境 token、不伪造 AgentResponse |
| TestApp Debug | 协议边界、租约生命周期、调用两类 AIAgent Service、关联 ID、结果原子写入 | 不实现车辆状态本体、不直接修改 Phoenix 数据 |
| AIAgent | Debug 环境控制、正式 Agent 主链路、取消、真实回调、状态工具执行、Trace | 不依赖电脑端文件路径、不暴露 token 到结果 |

## 2. 版本、包名与启用条件

- TestApp applicationId：`com.hirain.aiagent.test`
- AIAgent Debug Eval Service 显式组件：`com.hirain.aiagent/com.hirain.aiagent.eval.EvalDebugService`
- 内部 AIDL：`com.hirain.aiagent.eval.IAIAgentEvalDebug`，唯一方法为 `String execute(String requestJson)`。
- 该 AIDL 是 **TestApp ↔ AIAgent 内部 Debug 合约**；不能当作电脑端 `EvalCommand/EvalResultEnvelope` Schema。
- Debug 变体包含 Eval Activity、AIDL、结果 Provider、Gson；Release 不包含这些组件。
- 当前外层协议版本：`protocolVersion = { "major": 1, "minor": 0, "schemaHash": "..." }`。TestApp 当前拒绝非 `1.0`。

执行真实用例前，AIAgent 与 TestApp 都必须是已运行的 Debug APK；TestApp 会自动尝试 bind Debug Service，但 AIAgent Debug Eval Service 未导出/未运行会导致 `EVAL_SERVICE_UNAVAILABLE`。

## 3. 电脑端到 TestApp：命令入口

入口 Activity：

```text
com.hirain.aiagent.test/com.hirain.aiagent.test.eval.ui.EvalBridgeActivity
```

使用 URL-safe、无 `=` padding 的 Base64，将 UTF-8 JSON 放入 `eval_command_b64`；传 `eval_auto_execute=true` 时自动执行。

```powershell
$adb = 'D:\code\android\forSdk\Sdk\platform-tools\adb.exe'
$commandB64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($commandJson)).TrimEnd('=').Replace('+','-').Replace('/','_')
& $adb -s emulator-5554 shell am start --user 10 `
  -n com.hirain.aiagent.test/com.hirain.aiagent.test.eval.ui.EvalBridgeActivity `
  --es eval_command_b64 $commandB64 `
  --ez eval_auto_execute true
```

`--user 10` 是当前 Automotive 模拟器实际使用的 Android 用户。应先用 `adb shell am get-current-user` 确认；不要假设是 user 0。

### 3.1 外层 EvalCommand

```json
{
  "protocolVersion": { "major": 1, "minor": 0, "schemaHash": "dev" },
  "correlationId": "case-001-send-text",
  "action": "SEND_TEXT",
  "payload": {
    "userId": "eval_user",
    "sessionId": "S_eval_case_001",
    "personaId": "chat",
    "text": "打开空调",
    "inputType": "TEXT"
  },
  "createdAt": "2026-07-17T03:25:00Z",
  "timeoutMs": 40000
}
```

约束：

- 顶层只允许 `protocolVersion`、`correlationId`、`action`、`payload`、`createdAt`、`timeoutMs`、`metadata`。
- `correlationId` 必须匹配 `^[A-Za-z0-9][A-Za-z0-9._:-]{0,127}$`，同时是结果文件名和 Trace 关联值。
- `SEND_TEXT` 必须有 `userId`、`sessionId`、`text`；可选 `personaId`；不得包含 `requestId`。
- `CANCEL_REQUEST.payload.targetCorrelationId` 必填，且不能等于自身 correlationId。
- `timeoutMs` 仅覆盖 TestApp watchdog，限于 35–60 秒；不会改变 AIAgent 内部 30 秒 deadline。

支持动作：`ACQUIRE_ENVIRONMENT`、`RESET_STATE`、`APPLY_STATE`、`READ_STATE`、`SEND_TEXT`、`CANCEL_REQUEST`、`CREATE_SESSION`、`SWITCH_SESSION`、`DELETE_SESSION`、`GET_VERSION`、`RELEASE_ENVIRONMENT`。

## 4. TestApp 到 AIAgent：两条不同链路

### 4.1 Debug 环境链路

TestApp 经显式 bind 的 `IAIAgentEvalDebug.execute(requestJson)` 调用 AIAgent。内部请求含：

```json
{
  "protocolVersion": { "major": 1, "minor": 0 },
  "operation": "READ_STATE",
  "correlationId": "case-001-send-text",
  "leaseToken": "仅进程内传递，不落盘",
  "ttlMs": null,
  "statePatch": null
}
```

`leaseToken` 由 `ACQUIRE_ENVIRONMENT` 返回，TestApp 仅保存在内存，禁止进入 UI、Provider 或外层结果 JSON。环境状态为 `READY` 才允许 RESET/APPLY/READ/SEND_TEXT；成功 release 后 TestApp 清理 token、执行记录和 Eval 模式。

### 4.2 正式 Agent 主链路

`SEND_TEXT` 不经 Debug Service 伪造响应。TestApp 创建正式 `AgentRequest` 并调用已有 AIAgent Facade：

- `requestId`：TestApp 生成 UUID。
- `clientMessageId`：**等于 command.correlationId**。
- `userId`、`sessionId`、`personaId`、`text`：来自 payload。
- `sourceApp`：`com.hirain.aiagent.test`。

TestApp 仅在 callback 的 `requestId` 与执行记录匹配、且 `clientMessageId` 命中 correlationId 时接受 AgentResponse。MainActivity 发现 Eval 所有权 ID 后会跳过 UI 和 TTS，防止人工聊天和 Eval 相互污染。

## 5. 关联模型与取消

```text
电脑端 correlationId
       │
       ├── TestApp 结果文件名：<correlationId>.json
       ├── AgentRequest.clientMessageId
       └── CANCEL_REQUEST.targetCorrelationId

TestApp 生成 requestId
       ├── AgentRequest.requestId
       ├── EvalResultEnvelope.requestId
       └── Phoenix agent.request 属性 request.id
```

取消流程：电脑端新发一个带自身 `correlationId` 的 `CANCEL_REQUEST`，payload 指向目标 correlationId。TestApp 从运行记录解析真实 `requestId` 后调用 AIAgent `cancelAgentRequest(requestId, "cancelled_by_eval")`。取消命令与目标 `SEND_TEXT` 各自生成独立结果文件；目标最终状态仍以真实 AgentResponse 或 watchdog 为准。

## 6. 结果结构与读取方式

外层结果对应电脑端 canonical `eval-result.schema.json`。关键字段如下：

```json
{
  "protocolVersion": { "major": 1, "minor": 0, "schemaHash": "dev" },
  "correlationId": "case-001-send-text",
  "bridgeState": "TERMINAL",
  "action": "SEND_TEXT",
  "requestId": "uuid-generated-by-testapp",
  "agentResponse": { "success": true, "status": "SUCCESS", "clientMessageId": "case-001-send-text" },
  "beforeVehicleState": { "environmentRevision": 1, "systems": {} },
  "afterVehicleState": { "environmentRevision": 2, "systems": {} },
  "versionFingerprint": {
    "aiagentVersion": "1.0",
    "apkBuildId": "1",
    "model": "qwen-turbo",
    "promptDigest": "...",
    "stateSchemaVersion": "1.0",
    "traceMode": "FULL_DEBUG"
  },
  "timestamps": {},
  "bridgeError": null
}
```

注意：AIAgent 内部版本字段会由 TestApp 映射为 canonical 名称：`versionName → aiagentVersion`、`versionCode → apkBuildId`、`textModel → model`、`promptHash → promptDigest`、`traceContentMode → traceMode`。电脑端不应依赖内部字段名。

对于环境和会话动作，结果使用 `operationResult`，而不是把 AIAgent 原始 Map 直接透传。错误采用结构化 `bridgeError`：至少包含 `code`、`stage`、`message`，可选 `diagnostic`。

### 6.1 Automotive 设备上的结果读取

计划原本的 `adb run-as` 在 Automotive secondary user 场景不可用；shell 对 app-specific 外部目录也无直接读取权限。因此 Debug 包提供只读 Provider：

```powershell
& $adb -s emulator-5554 shell content read --user 10 `
  --uri content://com.hirain.aiagent.test.eval.results/case-001-send-text
```

Provider 仅接受安全 correlationId、只允许 `r` 模式、只读 `eval-results/<correlationId>.json`，不支持 insert/update/delete/call；其声明仅在 `app/src/debug/AndroidManifest.xml`，Release 不含此通道。

## 7. SEND_TEXT 的状态机与证据

1. TestApp 写 `PENDING`，再写 `RUNNING`。
2. 在发送正式请求前，通过 Debug Service 读取 `beforeVehicleState` 与版本指纹。
3. 调用正式 AIAgent Service，立即释放串行执行器，等待真实 callback。
4. 收到 callback 后读取 `afterVehicleState`；读取失败时短暂异步重试最多 2 次。
5. 原子写出 `TERMINAL` 结果。watchdog 默认 40 秒，先发送 cancel，再给 2 秒 grace；仍无真实响应才写 `AGENT_RESPONSE_MISSING`。

不能将模型文本中的“已打开/已关闭”作为车控成功依据；电脑端必须比较 before/after 状态或检查对应工具 Trace。

## 8. Phoenix Trace 核验

Phoenix 应按这两个字段交叉查找同一条 Trace：

- `request.id == EvalResultEnvelope.requestId`
- `client_message.id == EvalResultEnvelope.correlationId`

当前本地 Phoenix GraphQL 端点为 `http://localhost:6006/graphql`。成功车控 Trace 应至少有 `agent.request`、`agent.loop`、`context.*`、`tool.execute`、`response.dispatch` 等 span；普通聊天未必有 `tool.execute`。

已验证的真实车控示例：

- correlationId：`phoenix-car-001`
- requestId：`70dd7e48-2e55-40a6-b8b0-94d1d985e8cf`
- traceId：`cb04d443d7351047b9b235b0abe6f517`
- 结果：45 个 span、0 error，`tool.execute(set_ac_status)` 成功；随后 READ_STATE 显示 `acStatus=true`。

## 9. 推荐用例顺序与故障定位

推荐顺序：`ACQUIRE_ENVIRONMENT → RESET_STATE → APPLY_STATE/READ_STATE → CREATE_SESSION → SEND_TEXT 普通问答 → SEND_TEXT 车控 → CANCEL_REQUEST → GET_VERSION → DELETE_SESSION（按策略）→ RELEASE_ENVIRONMENT`。

| 现象/错误 | 优先检查 |
|---|---|
| `ENVIRONMENT_NOT_READY` | 是否已 acquire；TestApp 是否因安装/重启而丢失进程内 token；AIAgent 是否仍持有旧租约 |
| `EVAL_SERVICE_UNAVAILABLE` | AIAgent Debug APK、`EvalDebugService` 组件名/export/bind 状态 |
| `BRIDGE_BUSY` | 是否有未终态命令；只有 CANCEL 可与运行中的 SEND_TEXT 并发 |
| `REQUEST_MAPPING_MISSING` | targetCorrelationId 是否正确；目标 SEND_TEXT 是否已生成 requestId |
| 结果读不到 | 检查 `--user`；使用 ContentProvider，不要对 Automotive secondary user 使用 `run-as` |
| Schema 拒绝版本对象 | 检查是否使用 TestApp 的 canonical `versionFingerprint` 映射，而非 AIAgent 原始字段 |
| Phoenix 查不到 | 先按 `request.id` 与 `client_message.id` 查询；检查 AIAgent Trace exporter 与 Phoenix 端口转发 |

## 10. 代码定位

- 外层命令/结果模型：`app/src/debug/java/com/hirain/aiagent/test/eval/protocol/`
- 状态机与正式请求编排：`.../eval/runtime/EvalBridgeManager.java`
- 环境租约与 Debug AIDL 客户端：`.../eval/environment/`、`.../eval/agent/AIAgentEvalDebugClient.java`
- 结果原子写入与 Debug-only Provider：`.../eval/store/`
- ADB 半自动入口：`.../eval/ui/EvalBridgeActivity.java`
- Debug Manifest：`app/src/debug/AndroidManifest.xml`
- 完整实施计划：`docs/plan/06-testapp-eval-bridge-implementation-plan.md`
