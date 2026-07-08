# 计划文档审查报告

**审查对象：** `docs/plan/2026-07-07-conversation-sidebar-user-persona-cancel-plan.md`

**审查方式：** 对照 AIAgent 项目实际代码逐项验证

---

## 一、确认无误的依赖

对 AIAgent 实际代码验证后，以下计划假设成立：

| 断言 | 来源 |
|------|------|
| `AgentRequest` 有 `userId/personaId/clientMessageId` | AgentRequest.java:20-22 ✅ |
| `AgentResponse` 有 `userId/personaId/status/errorDetail/clientMessageId` | AgentResponse.java:14-18 ✅ |
| `IAIAgentAidlInterface` 有 6 个新方法 | Generated stub: 行 365-370 ✅ |
| `ConversationRequest` 有 `userId/sessionId/personaId/title/sourceApp/timestamp` | ConversationRequest.java:8-13 ✅ |
| `ConversationInfo` 有 `getTitle()/getPersonaId()/getSessionId()/isActive()/getMessageCount()` | ConversationInfo.java:60-81 ✅ |
| `ConversationListResponse` 有 `getConversations()` | ConversationListResponse.java:74 ✅ |
| `ConversationOperationResult` 有 `isSuccess()/getConversationInfo()/getErrorType()/getErrorDetail()` | ConversationOperationResult.java:63-72 ✅ |
| `CancelRequestResult` 有 `getStatus()` 返回 `ACCEPTED/NOT_FOUND/ALREADY_FINISHED` 和 `getRequestId()` | CancelRequestResult.java:8-10, 80-83 ✅ |
| `IAIAgentAidlListener` 接口未变，`onAIResponse(AgentResponse)` 签名一致 | IAIAgentAidlListener.java:109 ✅ |

---

## 二、需要修正/补充的问题

### P0：DrawerLayout 与语音条上滑取消手势冲突

**位置：** Phase 2, Task 2.2

当前语音条交互逻辑在 `OnTouchListener` 中监听 `ACTION_MOVE`，通过 Y 轴偏移判断上滑取消。`DrawerLayout` 默认会拦截左边缘滑动手势。语音条位于底部输入区，上滑取消时手指向左上方移动，可能触发 DrawerLayout 的边缘拖拽。

**建议：** 在 Task 2.2 明确加上 `drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)` 当语音模式激活时，防止滑动手势冲突。

---

### P1：`requestTtsMap` 未清理取消请求的残留条目

**位置：** Phase 4, Task 4.3

取消响应（`status=CANCELLED`）不会触发 TTS，但代码没有从 `requestTtsMap` 中 `remove` 被取消的 `requestId`。如果用户多次取消，map 可能积累无用的条目。

**建议：** Task 4.3 增加一行：收到 `CANCELLED/TIMEOUT/EXCEPTION` 状态时，`requestTtsMap.remove(response.getRequestId())`。

---

### P1：`activeRequestId` 超时保护缺失

**位置：** Phase 4, Task 4.2

如果 `cancelAgentRequest` 返回 `ACCEPTED`，但 AIAgent 侧回调迟迟不到（进程被 kill、网络断开等），`isRequestProcessing` 将永远为 `true`，用户无法继续操作。

**建议：** 在 `setRequestProcessing(true)` 时启动一个定时器（如 30s），超时后强制 `setRequestProcessing(false, null, null)` + Toast "请求超时"。

---

### P1：DrawerLayout 切换会话后聊天区清空，旧消息无法恢复

**位置：** Phase 3, Task 3.2

计划明确不做本地存储，切换后聊天区清空只显示"已切换到：xxx"。从用户角度，切回之前的对话看不到历史消息。这不是代码 bug，但存在 UX 风险。

**建议：** 如能接受则保留。但切换到当前 active 会话时至少应清空提示改为显示会话信息（如 messageCount、updatedAt），让用户感知这个会话存在。

---

### P2：IAIAgentAidlListener.java 未明确在同步清单中

**位置：** Phase 1, Task 1.1

同步清单列出了 `IAIAgentAidlInterface.java` 但未列出 `IAIAgentAidlListener.java`。虽然当前接口未变，但 AIDL 生成的 `Stub/Proxy` 内部实现有 `TRANSACTION_*` 常量，若 AIAgent 端 AIDL 有更新而 TestApp 没同步，跨进程调用会用错的 `transaction code`。

**建议：** 同步清单中增加 `IAIAgentAidlListener.java`，从 AIAgent build 输出复制。

---

### P2：`voice_mode` 下取消按钮的交互细节未明确

**位置：** Phase 2, Task 2.2 / Phase 4, Task 4.2

Task 2.2 说"语音模式下不再隐藏右侧动作按钮"，Task 4.2 说"右侧动作按钮根据 isRequestProcessing 切换发送/停止"。但语音模式下右侧按钮紧贴语音条右侧，中间没有间距，视觉效果可能不佳。

**建议：** 明确语音模式下右侧按钮的间距和图标（默认图标？停止图标？），以及在语音条触摸中（`ACTION_DOWN` 后）右侧按钮是否禁用。

---

### P2：`ConversationAdapter` 空状态未明确

**位置：** Phase 2, Task 2.3

当用户首次使用且无历史会话时，侧边栏 RecyclerView 为空。计划未提空状态显示。

**建议：** 侧边栏无会话时显示一个 `TextView` 提示"暂无对话"（包含在 drawer layout 中），RecyclerView 不可见。

---

### P2：`setRequestProcessing` 禁用上下文操作但未包含自身的停止按钮

**位置：** Phase 3, Task 3.1

计划说"processing=true 时上下文切换按钮禁用，右侧按钮显示停止"。如果禁用了所有按钮而停止按钮就是右侧按钮，这个按钮显然不能禁用自己。

**建议：** Task 3.1 描述改为"除停止按钮外，所有上下文操作按钮禁用"。

---

### P3：`ConversationListResponse` 的 `getConversations()` 返回类型

**位置：** Phase 1, Task 1.2 Facade 方法

`listConversations()` 返回 `ConversationListResponse`，其中 `getConversations()` 返回 `ArrayList<ConversationInfo>`。计划需要明确 facade 中 `listConversations()` 是否透传 `ConversationListResponse` 对象，还是只提取 `List<ConversationInfo>`。

**建议：** 直接返回 `ConversationListResponse`，让 Activity 自己调用 `getConversations().getUserId()`。

---

### P3：DrawerLayout 在 activity_main.xml 中的根布局需注意 `android:fitsSystemWindows`

**位置：** Phase 2, Task 2.2

DrawerLayout 作为根布局时，`fitsSystemWindows` 会影响状态栏和导航栏的绘制区域。如果 Toolbar 设置了 `fitsSystemWindows=true`，可能导致状态栏双重 padding。

**建议：** Task 2.2 增加备注：`DrawerLayout` 设 `fitsSystemWindows="true"`，`Toolbar` 不设，避免冲突。

---

## 三、小结

| 等级 | 数量 | 说明 |
|------|------|------|
| P0 | 1 | DrawerLayout 与语音条手势冲突需提前处理 |
| P1 | 3 | requestTtsMap 残留、超时保护、消息恢复 UX |
| P2 | 5 | AIDL 同步遗漏、取消交互细节、空状态等 |
| P3 | 2 | 返回类型、布局兼容性 |

整体计划方向正确，AIDL 接口依赖已验证无误。P0 和 P1 问题建议在实现前修正。
