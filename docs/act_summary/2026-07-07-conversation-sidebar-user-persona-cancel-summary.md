# AIAgentTestApp 功能增强实施总结

## 概述

本轮为 AIAgentTestApp 引入了 6 项核心功能：会话管理、侧边栏导航、用户切换、AI 性格切换、请求取消与 Watchdog、TTS 与语音输入增强。整个实施分为 5 个 Phase，覆盖 AIDL 协议同步、UI 布局重构、业务状态接入、交互控制逻辑和最终验证。

---

## 一、总体做了什么

### Phase 1：AIDL 客户端协议同步

从 AIAgent 项目同步了 11 个文件，让 TestApp 能够编译期调用 AIAgent 新增的 AIDL 方法：

| 操作 | 文件 | 说明 |
|------|------|------|
| 修改 | `AgentRequest.java` | 同步新字段：userId、personaId、clientMessageId |
| 修改 | `AgentResponse.java` | 同步新字段：userId、personaId、status、errorDetail、clientMessageId |
| 修改 | `AIAgent.java` | 新增 6 个 facade 方法（会话管理 + 取消请求） |
| 新增 | `ConversationRequest.java` | 会话请求体 Parcelable |
| 新增 | `ConversationInfo.java` | 会话信息 Parcelable |
| 新增 | `ConversationListResponse.java` | 会话列表响应 Parcelable |
| 新增 | `ConversationOperationResult.java` | 会话操作结果 Parcelable |
| 新增 | `CancelRequestResult.java` | 取消请求结果 Parcelable |
| 修改 | `IAIAgentAidlInterface.java` | 同步 AIDL generated stub |
| 修改 | `IAIAgentAidlListener.java` | 同步 AIDL generated stub |

**关键设计：**
- 每个 facade 方法都做了 `m_service == null` 保护 + `RemoteException` 捕获 + Log 记录
- `listConversations()` 直接透传 `ConversationListResponse`，Activity 负责读取
- `processAgentRequest()` 保持返回 `int`，不改变调用方语义

### Phase 2：主界面与侧边栏布局

| 操作 | 文件 | 说明 |
|------|------|------|
| 修改 | `gradle/libs.versions.toml` | 新增 `drawerlayout = "1.2.0"` |
| 修改 | `app/build.gradle.kts` | 新增 `implementation(libs.drawerlayout)` |
| 修改 | `activity_main.xml` | DrawerLayout 重构 |
| 新增 | `item_conversation.xml` | 会话列表 item 布局 |
| 新增 | `ConversationAdapter.java` | 会话列表适配器 |
| 修改 | `MainActivity.java` | Drawer 打开 + 连接状态替换 + 侧边栏控件初始化 |
| 修改 | `strings.xml` | 10 条新增字符串 |
| 修改 | `colors.xml` | 新增 `divider` 颜色 |
| 新增 | `ic_stop.xml` | 停止图标 |

**布局改造要点：**
- 根布局改为 `DrawerLayout`，`android:fitsSystemWindows="true"`
- 标题使用独立 `TextView` 居中，不再依赖 Toolbar `app:title`，消除重叠问题
- 连接状态使用标题下方独立 `TextView` 小字号显示
- 侧边栏 300dp，顶部新建按钮 → 中部会话列表 + 空状态 → 底部用户/AI 设置按钮
- 右侧动作按钮空闲=发送、处理中=停止，语音模式保留可见但 disabled

**修复的验收问题：**
- P0: `Gravity.START` → `GravityCompat.START` 通过 lint
- P0: 侧边栏控件和 ConversationAdapter 接线
- P1: 右侧按钮语 模模式下保留可见并 disabled
- P1: 语音条录音时锁定/恢复 DrawerLayout 手势

### Phase 3：会话、用户、AI 性格状态接入

所有状态字段存储在 `MainActivity` 中，通过 `SharedPreferences` 持久化：

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `currentUserId` | String | "default_user" | 当前用户 |
| `currentPersonaId` | String | "chat" | 当前 AI 性格 |
| `currentSessionId` | String | null | 当前会话 ID |
| `currentConversation` | ConversationInfo | null | 当前会话元数据 |
| `activeRequestId` | String | null | 处理中的请求 ID |
| `isRequestProcessing` | boolean | false | 请求处理中标识 |

**接入的功能：**

| 功能 | 实现方式 |
|------|----------|
| 会话列表 | `loadConversationsForCurrentUser()` + `initActiveConversation()` |
| 新建会话 | `createConversation(ConversationRequest)` |
| 切换会话 | `switchConversation(userId, sessionId)` |
| 删除会话 | `deleteConversation(userId, sessionId)` + 确认弹窗 |
| 用户切换 | 预置三用户单选弹窗（default_user / test_user_1 / test_user_2） |
| AI 性格切换 | 三选项弹窗（chat / friendly / concise）+ TTS 设置入口 |

**修复的验收问题：**
- P1: `loadConversations()` 不覆盖用户已选择会话
- P1: `sendTextRequest()` 无 sessionId 时检查并阻止发送

### Phase 4：发送/停止请求与响应归属

**请求构造扩展：**
- 发送 `AgentRequest` 时写入 `clientMessageId`、`requestId` 本地变量
- 发送成功后调用 `setRequestProcessing(true, requestId)` 进入处理状态
- 失败时 `requestTtsMap.remove()` 并 Toast

**取消请求：**
- `btnSend` 双态路由：`isRequestProcessing=false` → 发送，`true` → 取消
- 取消调用 `cancelAgentRequest(activeRequestId, "cancelled_by_client")`
- 状态处理：`ACCEPTED` → 保持 processing + 按钮 disabled，等待回调；`NOT_FOUND` / `ALREADY_FINISHED` → 恢复
- 暂停状态引用 `CancelRequestResult.STATUS_*` 常量而非硬编码字符串

**Watchdog 超时保护：**
- `setRequestProcessing(true)` 时 `postDelayed(20000L)` 启动
- 超时时恢复 UI + Toast "请求等待超时，请检查 AIAgent 服务状态"
- `onDestroy` / `onDisconnected` / 正常响应回调时清理

**响应归属与 TTS：**
- `onAIResponse()` 根据 `response.requestId` 匹配 `activeRequestId`
- 当前请求 → `setRequestProcessing(false)` + TTS 判断
- TTS 条件：`wasVoiceEntry=true && isSuccess() && status ∉ {CANCELLED,TIMEOUT,EXCEPTION}`
- 迟到响应 → 显示"（迟到响应）"前缀，不改变 processing
- 取消/超时响应不播报 TTS

### Phase 5：构建验证

| 验证项 | 结果 |
|--------|------|
| `compileDebugJavaWithJavac` | ✅ BUILD SUCCESSFUL |
| `assembleDebug` | ✅ BUILD SUCCESSFUL |
| `lintDebug` | ✅ 0 error |

---

## 二、新增功能清单

| 功能 | 入口 | 说明 |
|------|------|------|
| **会话管理** | 侧边栏 | 新建、切换、删除会话，AIAgent 服务端管理 |
| **用户切换** | 侧边栏 → 用户按钮 | 三预置用户，切换后清空会话列表 |
| **AI 性格切换** | 侧边栏 → AI 设置 | chat / friendly / concise 三种人格 |
| **请求取消** | 右侧按钮（处理中） | 停止当前请求，异步等待取消确认 |
| **Watchdog 超时** | 自动 | 20 秒无响应自动恢复 UI |
| **侧边栏导航** | 左上角按钮 | 300dp DrawerLayout + 会话列表 |
| **标题居中** | 顶部栏 | 独立 TextView 居中，连接状态小字 |
| **发送/停止双态按钮** | 输入区右侧 | 文本模式可发送，语音模式 disabled，处理中停止 |

## 三、技术债务

1. `setRequestProcessing()` 签名接受了用户评审建议，但 `clientMessageId` 参数暂未使用（保留给后续 Phase 需要）
2. 侧边栏活跃会话高亮使用固定颜色 `0x1A6750A4`，后续可改为 theme attribute
3. 未接入本地会话消息持久化（已在计划边界内声明不做）
4. AI 性格切换器 `AgentResponse` 没有对应的常规定义，直接字符串与 AIAgent 端约定一致

## 四、目录结构变更

```
AIAgentTestApp/
├── app/src/main/java/com/hirain/aiagent/           # 同步文件（+5 新增, +3 修改）
│   ├── AgentRequest.java        (修改)
│   ├── AgentResponse.java       (修改)
│   ├── AIAgent.java             (修改)
│   ├── ConversationRequest.java (新增)
│   ├── ConversationInfo.java    (新增)
│   ├── ConversationListResponse.java (新增)
│   ├── ConversationOperationResult.java (新增)
│   └── CancelRequestResult.java (新增)
├── app/src/main/java/com/hirain/aiagent/test/
│   ├── adapter/
│   │   ├── ChatAdapter.java     (不变)
│   │   └── ConversationAdapter.java (新增)
│   ├── activity/
│   │   └── MainActivity.java    (大幅修改，~580 行)
├── app/src/main/res/
│   ├── drawable/
│   │   └── ic_stop.xml          (新增)
│   ├── layout/
│   │   ├── activity_main.xml    (DrawerLayout 重构)
│   │   └── item_conversation.xml (新增)
│   └── values/
│       ├── strings.xml          (新增 10 条)
│       └── colors.xml           (新增 divider)
├── docs/
│   ├── plan/2026-07-07-conversation-sidebar-user-persona-cancel-plan.md
│   ├── review/2026-07-07-phase*.md
│   └── act_summary/2026-07-07-conversation-sidebar-user-persona-cancel-summary.md
└── build.gradle.kts / libs.versions.toml    (修改：DrawerLayout 依赖)
```
