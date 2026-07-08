# Phase 3 验收审查报告

**审查对象：** Phase 3：会话、用户、AI 性格状态接入

---

## 审查结论：有条件通过 — 2 个阻塞问题 + 3 个待修复

| Task | 状态 | 说明 |
|------|------|------|
| 3.1 状态模型 | ✅ 通过 | 字段全面，SharedPreferences 保存/恢复完整 |
| 3.2 会话列表 | ✅ 通过 | 列出现状、切换、删除、空状态均实现 |
| 3.3 新建会话 | ✅ 通过 | 含 processing 检查，sessionId 由 AIAgent 分配 |
| 3.4 用户切换 | ✅ 通过 | 预置用户列表，切换后清空会话 |
| 3.5 AI 性格 | ✅ 通过 | 三种性格 + 保留 TTS 设置入口 |

---

## P1: `loadConversationsForCurrentUser()` 会覆盖用户已选择的会话

**位置：** `MainActivity.java:619-624`

`loadConversationsForCurrentUser()` 在刷新列表后调用了 `getActiveConversation()` 并直接用结果覆盖 `currentSessionId`：

```java
ConversationInfo active = AIAgent.getInstance().getActiveConversation(currentUserId);
if (active != null) {
    currentSessionId = active.getSessionId();   // ← 覆盖用户选择
    currentConversation = active;
```

复现场景：用户切换到会话 B（非 active）→ 触发列表刷新（如删除后自动调用 loadConversations）→ `currentSessionId` 被改回 A。

**建议：** `loadConversationsForCurrentUser()` 只刷新列表和高亮，不应改变 `currentSessionId`。`getActiveConversation()` 的调用应放在服务刚连接时（`onConnected`）初始化 session 用。

---

## P1: `sendTextRequest()` 未检查 `currentSessionId`

**位置：** `MainActivity.java:496-527`

请求构造中 `req.setSessionId(currentSessionId)` 在无会话时直接传 `null`。AIAgent 可能无法处理无 sessionId 的请求。

**建议：** 在 `sendTextRequest` 开头加判断：
```java
if (currentSessionId == null) {
    Toast.makeText(this, "请先新建或选择会话", Toast.LENGTH_SHORT).show();
    return;
}
```

> 注：该项属于 Phase 4 Task 4.1 的要求（“发送前检查 currentSessionId，如果为空，Toast 不发送”），但代码已写到 Phase 3 则建议一并补齐。

---

## P2: `setRequestProcessing()` 签名与计划不一致

**位置：** `MainActivity.java:588`

计划要求：
```java
setRequestProcessing(boolean processing, String requestId, String clientMessageId)
```

实际：
```java
setRequestProcessing(boolean processing, String requestId)
```

缺少 `clientMessageId` 参数。后续 Phase 4 需要用它时可能再改签名。

**建议：** 如果 Phase 4 不需要 `clientMessageId`，不需要修改；如果需要，到时候改一次即可。不影响当前功能。

---

## P2: `setRequestProcessing()` 目前未被任何地方调用

`isRequestProcessing` 永远为 `false`，导致侧边栏所有 `if (isRequestProcessing)` 处理中检查**形同虚设**。

这属于 Phase 4 范畴（Task 4.1 负责调用），但 Phase 3 定义了方法却不调用可能导致审核时误判。如确认 Phase 4 会接管，可接受。

---

## ✅ 验证通过项

| 检查项 | 状态 |
|--------|------|
| `currentUserId` 持久化 | ✅ `SharedPreferences` |
| `currentPersonaId` 持久化 | ✅ `SharedPreferences` |
| `currentSessionId` 持久化 | ✅ |
| `voice_mode` / `tts_mode` 保留 | ✅ |
| `isRequestProcessing` 字段 | ✅ |
| `createConversation()` API 调用含 personaId | ✅ |
| `switchConversation()` 成功后更新 local + 关闭 drawer | ✅ |
| `deleteConversation()` 成功后从侧边栏移除 + 清空当前 | ✅ |
| 用户切换后清空 `currentSessionId` + 刷新列表 | ✅ |
| AI 性格切换后立即写入 `currentPersonaId` | ✅ |
| AI 设置弹窗保留 TTS 设置入口 | ✅ |
| 连接状态显示用户 + sessionId 缩略 | ✅ |
| `btn_settings` 打开 DrawerLayout | ✅ |
| 语音模式锁定/恢复 DrawerLayout 手势 | ✅ |
| `ConversationInfo` 字段正确处理（title/subtitle/active） | ✅ |
| `ic_stop.xml` 存在 | ✅ |
| AIAgent.java facade 6 方法全部实现 | ✅ |
| 编译 `BUILD SUCCESSFUL` | ✅ |

---

## 汇总

| 等级 | 数量 | 说明 |
|------|------|------|
| P1 | 2 | `getActiveConversation` 覆盖 sessionId；`sendTextRequest` 缺 sessionId 检查 |
| P2 | 2 | `setRequestProcessing` 签名不一致（如 Phase 4 需改则改）；从未被调用（Phase 4 范畴） |

两个 P1 建议修复后再进入 Phase 4。P2 可保留到 Phase 4 统一处理。
