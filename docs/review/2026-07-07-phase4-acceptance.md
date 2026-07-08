# Phase 4 验收审查报告

**审查对象：** Phase 4：发送/停止请求与响应归属

---

## 审查结论：通过 — 无阻塞问题

| Task | 状态 | 说明 |
|------|------|------|
| 4.1 sendTextRequest 改造 | ✅ | sessionId 检查、processing 检查、clientMessageId、setRequestProcessing 调用 |
| 4.2 停止按钮行为 | ✅ | 双态切换、cancelAgentRequest 调用、三种返回码处理 |
| 4.3 响应归属与 UI 恢复 | ✅ | requestId 匹配、终态恢复、TTS 过滤、迟到响应处理、20s Watchdog |

---

## ✅ 每项检查

### Task 4.1 — `sendTextRequest` 改造

| 要求 | 实现位置 | 状态 |
|------|---------|------|
| 发送前检查 `currentSessionId == null` | 行 529-532 | ✅ Toast + early return |
| 发送前检查 `isRequestProcessing` | 行 533-536 | ✅ Toast + early return |
| 设置 `clientMessageId` | 行 542, 546 | ✅ UUID 生成 |
| 设置 `requestId` | 行 541, 545 | ✅ UUID 生成 |
| 设置 `sessionId/userId/personaId` | 行 547-549 | ✅ |
| 成功调用 `setRequestProcessing(true, requestId)` | 行 572 | ✅ |
| 失败时清理 TTS map | 行 569 | ✅ |

### Task 4.2 — 停止按钮行为

| 要求 | 实现位置 | 状态 |
|------|---------|------|
| `isRequestProcessing=true` → 显示停止图标 | `setRequestProcessing` 行 670 | ✅ `ic_stop` |
| 点击停止调用 `cancelAgentRequest(requestId, reason)` | 行 490-491 | ✅ |
| 返回 `ACCEPTED` → 禁用按钮 + "取消中…" | 行 492-495 | ✅ |
| 返回 `NOT_FOUND`/`ALREADY_FINISHED` → 恢复 | 行 497-501 | ✅ |
| Binder 失败 → 恢复 + Toast | 行 503-505 | ✅ |
| `isRequestProcessing=false` → 显示发送图标 | `setRequestProcessing` 行 674 | ✅ `ic_send` |

### Task 4.3 — 响应归属与 UI 恢复

| 要求 | 实现位置 | 状态 |
|------|---------|------|
| 空响应/空文本检查 | 行 624 | ✅ |
| `response.requestId` 与 `activeRequestId` 匹配 | 行 628 | ✅ |
| 匹配当前后 `setRequestProcessing(false, null)` | 行 635 | ✅ |
| TTS map 清理所有响应（含迟到） | 行 631 | ✅ |
| TTS 仅 success 且非 CANCELLED/TIMEOUT/EXCEPTION | 行 641-648 | ✅ |
| 迟到响应加前缀不改变 processing | 行 651-654 | ✅ |
| 20s Watchdog 超时保护 | 行 666, 86-90 | ✅ |
| Watchdog onDestroy 清理 | 行 750 | ✅ |
| 服务断开时自动恢复 processing | 行 615-618 | ✅ |

---

## ✅ 回归验证

| 已有功能 | 状态 |
|---------|------|
| 文字发送 | ✅ `sendTextRequest` 逻辑完整 |
| 语音条（长按/发送/取消） | ✅ 手势加 DrawerLayout 锁定恢复 |
| TTS auto/on/off | ✅ 不受新逻辑影响 |
| 侧边栏会话/用户/性格 | ✅ processing 检查挡在 sendTextRequest 外 |
| AIDL Facade 全部 6 方法 | ✅ |
| 编译 | ✅ `BUILD SUCCESSFUL` |

---

## P3 建议

| 建议 | 严重度 | 说明 |
|------|--------|------|
| 取消状态字符串应使用常量 | P3 | `"ACCEPTED"` / `"NOT_FOUND"` / `"ALREADY_FINISHED"` 应引用 `CancelRequestResult.STATUS_ACCEPTED` 等常量，而非硬编码字符串。多个位置引用时更安全 |

---

## 汇总

| 等级 | 数量 |
|------|------|
| 阻塞 | 0 |
| P1 | 0 |
| P2 | 0 |
| P3 | 1（常量建议） |

Phase 4 实现完整，所有取消、归属、超时、恢复路径均已闭环。`initActiveConversation()` 已解决 Phase 3 的 sessionId 覆盖问题。没有需要阻塞的 Bug，可以进入 Phase 5 收尾验证。
