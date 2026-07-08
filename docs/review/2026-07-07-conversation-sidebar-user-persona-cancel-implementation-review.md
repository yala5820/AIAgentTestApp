# 会话侧边栏、用户/性格切换与取消请求实施验收报告

> 验收对象：`docs/plan/2026-07-07-conversation-sidebar-user-persona-cancel-plan.md` 全阶段实现，以及 `docs/act_summary/2026-07-07-conversation-sidebar-user-persona-cancel-summary.md` 中声明的完成结果。  
> 验收时间：2026-07-07  
> 验收结论：构建与 lint 已通过，但功能验收暂不通过；存在会话边界、active 会话同步和语音模式恢复相关问题。

## 1. 验证结果

### 1.1 已执行命令

```powershell
.\gradlew.bat :app:compileDebugJavaWithJavac
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:testDebugUnitTest
```

### 1.2 命令结果

- `:app:compileDebugJavaWithJavac`：通过，`BUILD SUCCESSFUL`。
- `:app:assembleDebug`：通过，`BUILD SUCCESSFUL`。
- `:app:lintDebug`：通过，`BUILD SUCCESSFUL`，当前没有 lint error。
- `:app:testDebugUnitTest`：通过，但结果为 `NO-SOURCE`，当前没有 JVM 单元测试实际覆盖这些状态流。

### 1.3 AIDL 同步核验

已将 TestApp 当前 AIDL 客户端副本与 AIAgent 当前源文件或 generated stub 做哈希对比，以下文件一致：

- `AgentRequest.java`
- `AgentResponse.java`
- `CancelRequestResult.java`
- `ConversationInfo.java`
- `ConversationListResponse.java`
- `ConversationOperationResult.java`
- `ConversationRequest.java`
- `IAIAgentAidlInterface.java`
- `IAIAgentAidlListener.java`

协议同步未发现漂移。

## 2. 必须整改问题

### P1：切换/新建/删除/切用户后没有清空聊天区，破坏会话边界

位置：

- `app/src/main/java/com/hirain/aiagent/test/activity/MainActivity.java:159`
- `app/src/main/java/com/hirain/aiagent/test/activity/MainActivity.java:177`
- `app/src/main/java/com/hirain/aiagent/test/activity/MainActivity.java:211`
- `app/src/main/java/com/hirain/aiagent/test/activity/MainActivity.java:239`
- `app/src/main/java/com/hirain/aiagent/test/adapter/ChatAdapter.java:21`

问题：

计划明确要求切换会话、新建会话、删除当前会话、切换用户后清空聊天气泡并显示一条系统提示。但当前 `ChatAdapter` 只有 `addMessage()`，没有清空数据的方法；`MainActivity` 在这些路径里也只是继续 `chatAdapter.addMessage(...)`。

影响：

- 用户切换到新会话后，旧会话消息仍显示在当前会话 UI 中。
- 用户切换后，旧用户的聊天内容仍显示在新用户下，测试 userId 隔离时会产生误判。
- 新建会话后的第一轮对话前，界面仍残留旧上下文气泡，与“本轮不恢复历史气泡”的产品边界相冲突。

建议：

- 在 `ChatAdapter` 增加 `clearMessages()` 或 `submitMessages()`。
- 在新建会话、切换会话、删除当前会话、切换用户成功后先清空，再添加一条系统提示。
- 清空后注意 `scrollToBottom()` 不要对空列表调用 `smoothScrollToPosition(-1)`。

### P1：active 会话同步不完整，可能继续使用本地过期 sessionId 发送请求

位置：

- `app/src/main/java/com/hirain/aiagent/test/activity/MainActivity.java:280`
- `app/src/main/java/com/hirain/aiagent/test/activity/MainActivity.java:605`
- `app/src/main/java/com/hirain/aiagent/test/activity/MainActivity.java:691`
- `app/src/main/java/com/hirain/aiagent/test/activity/MainActivity.java:703`

问题：

当前实现从 `SharedPreferences` 恢复 `current_session_id` 后，`initActiveConversation()` 会直接返回，不向 AIAgent 校验这个 session 是否仍属于当前用户、是否仍存在、是否仍是 active。`loadConversationsForCurrentUser()` 也只提交列表，不校验 `resp.isSuccess()`、`resp.getUserId()`，不调用 `getActiveConversation()`，也不在列表不包含当前 session 时清空本地 `currentSessionId`。

具体风险：

- AIAgent 端会话被删除、用户切换或 active 会话变化后，TestApp 仍可能用本地旧 `currentSessionId` 发送请求。
- 用户切换时虽然本地清空了 session，但只调用 `loadConversationsForCurrentUser()`，没有再调用 `getActiveConversation()`；如果该用户服务端已有 active 会话，TestApp 不会自动恢复。
- 删除当前会话时先 `loadConversationsForCurrentUser()` 再清空 `currentSessionId`，且清空后没有 `refreshHeaderState()`，顶部状态可能继续显示已删除 session 的后 8 位。

影响：

- 会话列表高亮、顶部状态和实际发送 session 可能不一致。
- 手动验收“删除当前会话后不会继续用旧 sessionId 发送消息”无法稳定通过。
- 用户切换后的会话恢复行为与计划不一致。

建议：

- `loadConversationsForCurrentUser()` 成功后校验 `resp.isSuccess()` 和 `resp.getUserId()`。
- 每次刷新列表后获取服务端 active 会话，或至少校验当前 `currentSessionId` 是否存在于列表中。
- 如果本地 session 不存在于列表，应清空 `currentSessionId/currentConversation` 并移除持久化值。
- 删除当前会话后先清空本地状态，再刷新列表和 header。

### P1：持久化语音模式恢复后没有初始化讯飞 SDK，语音条会显示可用但录音立即失败

位置：

- `app/src/main/java/com/hirain/aiagent/test/activity/MainActivity.java:277`
- `app/src/main/java/com/hirain/aiagent/test/activity/MainActivity.java:359`
- `app/src/main/java/com/hirain/aiagent/test/activity/MainActivity.java:433`
- `app/src/main/java/com/hirain/aiagent/test/asr/ASRManager.java:132`

问题：

进入语音模式时会通过 `asrManager.getPermissionInit(...)` 初始化 SDK，并在成功后保存 `voice_mode=true`。但 Activity 重建后，`initViews()` 直接读取 `voice_mode=true` 并调用 `applyInputMode()` 展示语音条，没有再次调用 `getPermissionInit()`。

`ASRManager` 的 `isAuth` 默认是 `false`，只有 `SDKInit()` 成功后才会变为 true。用户在恢复出的语音模式中长按语音条时，`startAsr()` 会因为 `!isAuth` 直接触发 `onError()`，表现为语音条看似可用但实际不可用。

影响：

- 用户上次停留在语音模式，重新打开 app 后第一次语音输入必失败。
- 这会让“语音输入与会话管理一起回归可用”的验收结果不稳定。

建议：

- 恢复 `voice_mode=true` 时，不要直接显示可录音状态；应先初始化 SDK 并检查录音权限。
- 或者只持久化 UI 偏好，不自动进入语音模式；需要用户重新点击语音切换按钮完成初始化。
- `ACTION_DOWN` 前也应检查 SDK ready 状态，未 ready 时提示并阻止进入录音状态。

## 3. 需要修正或确认的问题

### P2：`onAIResponse()` 在 `response.text == null` 时直接返回，终态恢复逻辑不够稳健

位置：`app/src/main/java/com/hirain/aiagent/test/activity/MainActivity.java:623`

当前 AIAgent TEXT 路径的 `RuntimeResponseMapper` 会给 `TIMEOUT/CANCELLED/EXCEPTION` 设置非空文本，因此这不是当前主路径必现问题。但 TestApp 的响应归属逻辑应以 `requestId/status/errorType` 为准，而不是先要求 `text != null`。

如果未来 AIAgent 某个错误路径返回了有 `requestId/status` 但无 `text` 的终态响应，当前 UI 会直接 return，`isRequestProcessing` 不会恢复，TTS 映射也不会清理，只能等 20 秒 Watchdog。

建议：

- 先处理 `requestId/status/errorType`、清理 `requestTtsMap`、恢复 processing。
- 展示文本为空时再使用 fallback 文案，例如 `errorDetail` 或 `系统: 请求已结束`。

### P2：`setRequestProcessing()` 未保存 `clientMessageId`，与计划中的响应归属模型不完全一致

位置：

- `app/src/main/java/com/hirain/aiagent/test/activity/MainActivity.java:541`
- `app/src/main/java/com/hirain/aiagent/test/activity/MainActivity.java:659`

当前请求构造已经写入 `clientMessageId`，但 Activity 没有 `activeClientMessageId` 字段，`setRequestProcessing()` 也只保存 `requestId`。在当前 AIAgent 会稳定回传 requestId 的情况下可用，但计划要求区分 `requestId/sessionId/personaId/clientMessageId`，并明确 `setRequestProcessing(boolean processing, String requestId, String clientMessageId)`。

建议：

- 增加 `activeClientMessageId` 字段并随请求状态保存。
- 响应归属仍优先用 `requestId`，但可以在调试日志和异常路径中保留 `clientMessageId`，避免后续排查归属问题时缺少客户端消息维度。

### P2：处理中状态没有真正禁用上下文操作控件

位置：`app/src/main/java/com/hirain/aiagent/test/activity/MainActivity.java:659`

当前新建、切换、删除、用户切换、AI 设置都在点击回调里检查 `isRequestProcessing` 并 Toast，但控件本身没有禁用。计划要求 processing=true 时除停止按钮外，上下文操作按钮禁用。

这不是数据正确性的核心阻断项，因为回调中做了保护；但 UI 会给用户“可点击”的错误反馈，且长按删除仍会弹出确认框之前才进入操作逻辑，体验上不干净。

建议：

- 在 `setRequestProcessing()` 中统一设置 `mBtnNewConversation`、`mBtnDrawerUser`、`mBtnDrawerAiSettings`、`conversationAdapter` 交互状态。
- Adapter 可以增加 `setInteractionEnabled(false)`，避免处理中仍触发 item 点击/长按。

## 4. 已通过项

- 上轮指出的 `Gravity.START` lint error 已修复为 `GravityCompat.START`。
- 侧边栏控件已初始化，`ConversationAdapter` 已挂载。
- 右侧动作按钮已支持发送/停止双态，语音模式下保持可见并禁用。
- 语音条录音期间已加入 DrawerLayout 临时锁定/恢复。
- 发送请求时已写入 `requestId`、`clientMessageId`、`userId`、`sessionId`、`personaId`。
- `cancelAgentRequest()` 已接入，`ACCEPTED/NOT_FOUND/ALREADY_FINISHED` 三类返回有基本处理。
- AIDL 客户端副本与 AIAgent 当前版本一致。
- Java 编译、debug 打包和 lint 均通过。

## 5. 验收结论

当前实现不能直接通过功能验收。构建层面已经达标，但会话边界和状态同步仍有问题：切换会话/用户不清空聊天区会造成跨会话内容混淆；本地持久化 session 不校验会导致使用过期 session；持久化语音模式恢复后未初始化 SDK 会造成语音输入首用失败。

建议整改后重新执行：

```powershell
.\gradlew.bat :app:compileDebugJavaWithJavac
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:lintDebug
```

并补充真机/模拟器人工验收：

1. 新建、切换、删除当前会话后聊天区只保留当前提示。
2. 切换用户后旧用户消息不再显示，且发送请求携带新用户 active session。
3. 删除当前会话后不能继续用旧 sessionId 发送。
4. app 停留语音模式退出重进后，第一次长按语音输入可正常触发 SDK 或明确提示未初始化。
5. 请求处理中上下文操作入口不可用，停止按钮可用。
