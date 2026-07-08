# AIAgentTestApp 会话侧边栏、用户/性格切换与取消请求 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 AIAgentTestApp 中接入 AIAgent 新增的会话管理、用户 ID、AI 性格和取消请求能力，并把当前预留设置入口改造成简单可用的左侧会话侧边栏。

**Architecture:** TestApp 继续作为 AIAgent 的独立测试客户端，AIAgent 中枢仍然是会话、用户、人格和取消请求的真实能力提供方。TestApp 只维护当前 UI 状态：当前用户、当前会话、当前性格、当前进行中的 requestId，以及侧边栏会话列表；本阶段不做本地历史消息存储。

**Tech Stack:** Java、AndroidX AppCompat、RecyclerView、ConstraintLayout、DrawerLayout、AIDL 跨进程调用、SharedPreferences。

---

## 1. 已确认产品边界

1. 会话历史采用轻量测试版：侧边栏只显示 AIAgent 返回的会话列表，切换会话后聊天区清空并显示当前会话提示，不恢复历史气泡。
2. 用户切换采用预置测试用户列表：`default_user`、`test_user_1`、`test_user_2`。
3. AI 性格切换立即影响当前会话后续请求：`chat`、`friendly`、`concise`。
4. 请求处理采用单请求进行中模型：同一时间只允许一个未完成请求。
5. 请求处理中禁止切换用户、切换会话、新建会话、删除会话、切换 AI 性格，只允许点击停止按钮取消当前请求。
6. 侧边栏使用外层 `DrawerLayout`，左侧内容使用 `ConstraintLayout`，不使用 `NavigationView`。
7. 新建会话标题使用本地生成的简单标题：`新对话 MM-dd HH:mm`。
8. 当前不修改 AIAgent 项目代码；仅在 TestApp 同步 AIDL 客户端副本并接入 UI。

## 2. 工作边界

### 本轮包含

- 修复顶部标题与左侧按钮重叠问题，使 `AI 测试` 居中显示。
- 增加左侧会话侧边栏。
- 接入 AIAgent 新增会话 AIDL：创建、列出、切换、删除、获取当前会话。
- 接入 `AgentRequest.userId`、`personaId`、`clientMessageId`。
- 接入 `AgentResponse.userId`、`personaId`、`status`、`errorDetail`、`clientMessageId`。
- 接入 `cancelAgentRequest(requestId, reason)`。
- 增加用户切换弹窗和 AI 设置弹窗。
- 调整输入区右侧按钮为发送/停止双态。

### 本轮不包含

- 不做本地会话消息持久化。
- 不恢复已切换会话的历史气泡。
- 不设计复杂 UI 视觉风格。
- 不新增会话重命名能力，因为当前 AIDL 没有 rename 接口。
- 不支持多请求并发。
- 不修改 AIAgent 中枢能力实现。

## 3. 文件结构规划

### AIDL 客户端同步

- Modify: `app/src/main/java/com/hirain/aiagent/AgentRequest.java`
  - 同步 AIAgent 新字段：`userId`、`personaId`、`clientMessageId`。
- Modify: `app/src/main/java/com/hirain/aiagent/AgentResponse.java`
  - 同步 AIAgent 新字段：`userId`、`personaId`、`status`、`errorDetail`、`clientMessageId`。
- Modify: `app/src/main/java/com/hirain/aiagent/IAIAgentAidlInterface.java`
  - 同步新版 AIDL generated stub，增加会话管理和取消请求方法。
- Modify: `app/src/main/java/com/hirain/aiagent/IAIAgentAidlListener.java`
  - 从 AIAgent generated stub 同步，保持 listener 侧 Binder 代码与服务端同源。
- Add: `app/src/main/java/com/hirain/aiagent/ConversationRequest.java`
- Add: `app/src/main/java/com/hirain/aiagent/ConversationInfo.java`
- Add: `app/src/main/java/com/hirain/aiagent/ConversationListResponse.java`
- Add: `app/src/main/java/com/hirain/aiagent/ConversationOperationResult.java`
- Add: `app/src/main/java/com/hirain/aiagent/CancelRequestResult.java`
- Modify: `app/src/main/java/com/hirain/aiagent/AIAgent.java`
  - 增加同步 facade 方法，隐藏 `RemoteException`，保持 MainActivity 调用简单。

### UI 与业务状态

- Modify: `app/src/main/java/com/hirain/aiagent/test/activity/MainActivity.java`
  - 管理当前用户、当前会话、当前性格、进行中请求、Drawer 打开关闭、会话操作结果、取消请求结果。
- Add: `app/src/main/java/com/hirain/aiagent/test/adapter/ConversationAdapter.java`
  - 侧边栏会话列表适配器。
- Modify: `app/src/main/res/layout/activity_main.xml`
  - 根布局改为 `DrawerLayout`。
  - 主内容保留聊天界面。
  - 左侧 drawer 内容用 `ConstraintLayout`。
  - 右侧输入动作按钮支持发送/停止。
- Add: `app/src/main/res/layout/item_conversation.xml`
  - 会话列表 item。
- Modify: `app/src/main/res/values/strings.xml`
  - 增加会话、用户、AI 设置、取消请求、空状态等文本。
- Modify: `gradle/libs.versions.toml`
  - 增加 `drawerlayout` 版本和 library 坐标。
- Modify: `app/build.gradle.kts`
  - 增加 `implementation(libs.drawerlayout)`。

### 暂不修改

- `SettingsActivity.java` 本轮不作为入口使用，但不删除，避免扩大变更面。
- 当前 ASR/TTS 逻辑继续保留，只在发送请求和处理响应时接入新的状态字段与取消状态。

## 4. 阶段拆分

## Phase 1：同步 AIDL 客户端协议与 facade

**目标：** 让 TestApp 能编译期调用 AIAgent 新增 AIDL 方法，并能在请求/响应中传递 userId、personaId、clientMessageId。

### Task 1.1 同步 Parcelable 与 AIDL Stub

**Files:**

- Modify: `app/src/main/java/com/hirain/aiagent/AgentRequest.java`
- Modify: `app/src/main/java/com/hirain/aiagent/AgentResponse.java`
- Modify: `app/src/main/java/com/hirain/aiagent/IAIAgentAidlInterface.java`
- Add: `app/src/main/java/com/hirain/aiagent/ConversationRequest.java`
- Add: `app/src/main/java/com/hirain/aiagent/ConversationInfo.java`
- Add: `app/src/main/java/com/hirain/aiagent/ConversationListResponse.java`
- Add: `app/src/main/java/com/hirain/aiagent/ConversationOperationResult.java`
- Add: `app/src/main/java/com/hirain/aiagent/CancelRequestResult.java`

- [ ] 从 `D:\code\android\AndroidStudioProjects\AIAgent\app\src\main\java\com\hirain\aiagent\` 同步 `AgentRequest.java`、`AgentResponse.java` 和新增结果类。
- [ ] 从 `D:\code\android\AndroidStudioProjects\AIAgent\app\build\generated\aidl_source_output_dir\debug\out\com\hirain\aiagent\` 同步新版 `IAIAgentAidlInterface.java`。
- [ ] 从同一 generated 目录同步 `IAIAgentAidlListener.java`，即使当前签名未变化，也保持 generated Binder 代码同源。
- [ ] 确认 `AgentRequest.writeToParcel()` 与 `AgentResponse.writeToParcel()` 字段顺序和 AIAgent 端完全一致。
- [ ] 确认所有新增 Parcelable 的 `CREATOR`、`writeToParcel()` 和读取顺序与 AIAgent 端完全一致。

**验证：**

```powershell
.\gradlew.bat :app:compileDebugJavaWithJavac
```

预期：Java 编译通过，不出现 `method not found`、`BadParcelableException` 相关编译问题。

### Task 1.2 扩展 AIAgent Facade

**Files:**

- Modify: `app/src/main/java/com/hirain/aiagent/AIAgent.java`

- [ ] 增加以下 public 方法：
  - `ConversationOperationResult createConversation(ConversationRequest request)`
  - `ConversationListResponse listConversations(String userId)`
  - `ConversationOperationResult deleteConversation(String userId, String sessionId)`
  - `ConversationOperationResult switchConversation(String userId, String sessionId)`
  - `ConversationInfo getActiveConversation(String userId)`
  - `CancelRequestResult cancelAgentRequest(String requestId, String reason)`
- [ ] `listConversations(String userId)` 直接透传 `ConversationListResponse`，不要在 facade 中提取 `List<ConversationInfo>`；Activity 负责读取 `response.getUserId()` 和 `response.getConversations()`。
- [ ] 每个方法先检查 `m_service == null`，未连接时返回失败结果或 `null`，并记录 Log。
- [ ] 每个方法捕获 `RemoteException`，返回失败结果或 `null`，不让 Activity 直接处理 Binder 异常。
- [ ] `processAgentRequest()` 保持现有返回 `int` 的风格，不改变调用方语义。

**验证：**

```powershell
.\gradlew.bat :app:compileDebugJavaWithJavac
```

预期：Facade 新方法可被 Activity 调用，旧文字/语音发送路径仍可编译。

## Phase 2：主界面与侧边栏布局

**目标：** 修复顶部标题重叠，把设置入口改为左侧侧边栏，并提供会话列表、用户按钮、AI 设置按钮的基础 UI 容器。

### Task 2.1 增加 DrawerLayout 依赖

**Files:**

- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

- [ ] 在 version catalog 增加：
  - version：`drawerlayout = "1.2.0"`
  - library：`drawerlayout = { group = "androidx.drawerlayout", name = "drawerlayout", version.ref = "drawerlayout" }`
- [ ] 在 app 模块 dependencies 增加：
  - `implementation(libs.drawerlayout)`

**验证：**

```powershell
.\gradlew.bat :app:compileDebugJavaWithJavac
```

预期：依赖解析和 Java 编译通过。

### Task 2.2 重构 activity_main.xml 为 DrawerLayout

**Files:**

- Modify: `app/src/main/res/layout/activity_main.xml`
- Add: `app/src/main/res/layout/item_conversation.xml`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] 根布局改为 `androidx.drawerlayout.widget.DrawerLayout`。
- [ ] `DrawerLayout` 作为根节点时设置 `android:fitsSystemWindows="true"`；内部 Toolbar/标题容器不要重复设置 `fitsSystemWindows`，避免状态栏 padding 叠加。
- [ ] 主内容区放入一个 `ConstraintLayout`，保留当前 toolbar、消息列表和输入区。
- [ ] 顶部栏中：
  - 左侧按钮作为 drawer 打开按钮。
  - 标题使用独立 `TextView` 居中约束，不再使用 Toolbar `app:title`，避免与左侧按钮重叠。
  - 连接状态使用标题下方独立 `TextView` 小字号显示，不使用 Toolbar 原生 subtitle，避免与居中标题布局互相影响。
- [ ] 左侧 drawer 使用 `ConstraintLayout`，宽度固定为 `300dp`；平板专项适配不在本轮范围内。
- [ ] 左侧 drawer 子 View 必须设置 `android:layout_gravity="start"`。
- [ ] drawer 顶部放置“新建对话”按钮。
- [ ] drawer 中间放置 `RecyclerView` 显示会话列表。
- [ ] drawer 中间同时放一个空状态 `TextView`，无会话时显示“暂无对话”，并隐藏 RecyclerView。
- [ ] drawer 底部左侧放“用户”按钮，右侧放“AI 设置”按钮。
- [ ] 输入区右侧按钮保留一个动作按钮：空闲时发送，处理中停止。
- [ ] 语音模式下不再隐藏右侧动作按钮：
  - 未处理 AI 请求时显示发送图标但置为 disabled/低透明度，因为语音输入由“松开发送”触发。
  - ASR 正在录音或识别中时保持 disabled，避免用户同时点击右侧按钮破坏录音状态。
  - AIAgent 请求处理中时切换为停止图标并启用，用于取消当前请求。
- [ ] 语音条 `ACTION_DOWN` 开始录音时，调用 `drawerLayout.requestDisallowInterceptTouchEvent(true)`，并临时 `setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)`；`ACTION_UP/ACTION_CANCEL` 和 ASR 回调恢复 `LOCK_MODE_UNLOCKED`。不要在整个语音模式期间锁死 drawer，避免用户无法通过按钮打开侧边栏。

**验证：**

```powershell
.\gradlew.bat :app:compileDebugJavaWithJavac
.\gradlew.bat :app:assembleDebug
```

预期：资源编译和 APK 打包通过；标题不再依赖 Toolbar 原生 title。

### Task 2.3 增加 ConversationAdapter

**Files:**

- Add: `app/src/main/java/com/hirain/aiagent/test/adapter/ConversationAdapter.java`
- Add: `app/src/main/res/layout/item_conversation.xml`

- [ ] Adapter 数据源使用 `List<ConversationInfo>`。
- [ ] item 显示：
  - title：`ConversationInfo.getTitle()`，为空时显示 sessionId 后 8 位。
  - subtitle：优先显示 `personaId + " · " + messageCount + " 条"`；如果 messageCount 为 0，则显示 personaId。
  - active 状态：当前 sessionId 高亮。
- [ ] Adapter 暴露 `submitList(List<ConversationInfo> conversations, String activeSessionId)`，统一刷新列表和 active 高亮。
- [ ] item 点击回调：请求切换会话。
- [ ] item 长按回调：弹确认框后请求删除会话；本轮不在 item 内增加单独删除图标，减少侧边栏控件密度。

**验证：**

```powershell
.\gradlew.bat :app:compileDebugJavaWithJavac
```

预期：Adapter 与 `ConversationInfo` 类型匹配，布局资源可绑定。

## Phase 3：会话、用户、AI 性格状态接入

**目标：** MainActivity 能以当前 userId/sessionId/personaId 发送请求，并能通过侧边栏完成新建、切换、删除会话。

### Task 3.1 建立 MainActivity UI 状态模型

**Files:**

- Modify: `app/src/main/java/com/hirain/aiagent/test/activity/MainActivity.java`

- [ ] 增加状态字段：
  - `currentUserId = "default_user"`
  - `currentPersonaId = "chat"`
  - `currentSessionId`
  - `currentConversation`
  - `activeRequestId`
  - `activeClientMessageId`
  - `isRequestProcessing`
- [ ] 用 SharedPreferences 保存并恢复：
  - `current_user_id`
  - `current_persona_id`
  - `current_session_id`
  - `voice_mode`
  - 现有 `tts_mode`
- [ ] 增加统一方法 `setRequestProcessing(boolean processing, String requestId, String clientMessageId)`：
  - processing=true：输入框/语音条可保留显示；除右侧停止按钮外，所有上下文操作按钮禁用；右侧按钮显示停止。
  - processing=false：恢复上下文操作，右侧按钮显示发送。
- [ ] 增加主线程 `Handler` 和请求 UI watchdog：
  - `private static final long REQUEST_UI_TIMEOUT_MS = 20000L;`
  - 在 `setRequestProcessing(true, ...)` 中启动 watchdog。
  - 在收到任意终态响应、取消返回 `NOT_FOUND/ALREADY_FINISHED`、发送失败、Activity 销毁时移除 watchdog。
  - watchdog 触发时只恢复 TestApp UI 并 Toast：`请求等待超时，请检查 AIAgent 服务状态`；不伪造 AIAgent 响应。
- [ ] 增加统一方法 `refreshHeaderState()`：
  - 更新标题/副标题，展示当前用户、当前会话或连接状态。

**验证：**

```powershell
.\gradlew.bat :app:compileDebugJavaWithJavac
```

预期：新增状态字段不破坏现有文字、语音、TTS 编译路径。

### Task 3.2 接入会话列表与 active 会话

**Files:**

- Modify: `app/src/main/java/com/hirain/aiagent/test/activity/MainActivity.java`
- Modify: `app/src/main/java/com/hirain/aiagent/test/adapter/ConversationAdapter.java`

- [ ] 在服务连接成功后调用 `loadConversationsForCurrentUser()`。
- [ ] `loadConversationsForCurrentUser()` 调用 `AIAgent.getInstance().listConversations(currentUserId)`。
- [ ] 如果返回成功：
  - 更新 Adapter。
  - 调用 `getActiveConversation(currentUserId)`。
  - 如果 active 存在，更新 `currentSessionId/currentConversation`。
  - 如果 active 不存在，保留空会话状态，聊天区显示“请新建对话”提示。
- [ ] 如果返回失败：
  - Toast 显示错误。
  - 侧边栏显示空列表和“暂无对话”空状态。
- [ ] 切换会话时调用 `switchConversation(currentUserId, targetSessionId)`。
- [ ] 切换成功后：
  - 更新 `currentSessionId/currentConversation`。
  - 清空聊天气泡。
  - 添加一条系统提示气泡：`已切换到：<title>，消息数：<messageCount>`。
  - 关闭 drawer。
- [ ] 删除会话时调用 `deleteConversation(currentUserId, sessionId)`。
- [ ] 删除当前会话后：
  - 清空 `currentSessionId/currentConversation`。
  - 清空聊天气泡并提示“当前会话已删除，请新建或选择其他会话”。
  - 刷新会话列表。

**验证：**

```powershell
.\gradlew.bat :app:compileDebugJavaWithJavac
```

人工验证：

1. 服务连接后能刷新会话列表。
2. 切换会话后聊天区清空并显示切换提示。
3. 删除当前会话后不会继续用旧 sessionId 发送消息。

### Task 3.3 新建会话

**Files:**

- Modify: `app/src/main/java/com/hirain/aiagent/test/activity/MainActivity.java`

- [ ] 新建按钮点击时，如果 `isRequestProcessing=true`，Toast：`当前请求处理中，请先停止或等待完成`。
- [ ] 创建 `ConversationRequest`：
  - `userId = currentUserId`
  - `personaId = currentPersonaId`
  - `title = "新对话 " + 当前 MM-dd HH:mm`
  - `sourceApp = "aiagent_test"`
  - `timestamp = System.currentTimeMillis()`
  - `sessionId` 不主动填写，交由 AIAgent 创建。
- [ ] 调用 `createConversation(request)`。
- [ ] 创建成功后：
  - 更新 `currentSessionId/currentConversation`。
  - 清空聊天气泡。
  - 添加一条系统提示气泡：`已新建对话：<title>`。
  - 刷新会话列表。
  - 关闭 drawer。
- [ ] 创建失败时 Toast 显示 `errorType/errorDetail`。

**验证：**

```powershell
.\gradlew.bat :app:compileDebugJavaWithJavac
```

人工验证：

1. 新建会话出现在侧边栏。
2. 新建后的第一条请求使用新 sessionId。

### Task 3.4 用户切换

**Files:**

- Modify: `app/src/main/java/com/hirain/aiagent/test/activity/MainActivity.java`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] 用户按钮弹出单选列表：
  - `default_user`
  - `test_user_1`
  - `test_user_2`
- [ ] 如果 `isRequestProcessing=true`，禁止弹窗并 Toast。
- [ ] 用户确认切换后：
  - 更新 `currentUserId` 并保存。
  - 清空 `currentSessionId/currentConversation`。
  - 清空聊天气泡。
  - 添加系统提示气泡：`已切换用户：<userId>`。
  - 调用 `loadConversationsForCurrentUser()`。
- [ ] 切换用户不自动创建会话；如果该用户没有 active 会话，提示用户新建。

**验证：**

人工验证：

1. 不同用户下会话列表互不混淆。
2. 切换用户后发送请求携带新的 `userId`。

### Task 3.5 AI 性格设置

**Files:**

- Modify: `app/src/main/java/com/hirain/aiagent/test/activity/MainActivity.java`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] AI 设置按钮弹出简单设置对话框。
- [ ] 对话框包含 AI 性格单选项：
  - `chat`：默认
  - `friendly`：友好
  - `concise`：简洁
- [ ] 对话框保留现有 TTS 播报设置入口或合并现有 TTS 单选项，避免丢失上一阶段功能。
- [ ] 如果 `isRequestProcessing=true`，禁止打开设置并 Toast。
- [ ] 性格切换后：
  - 更新 `currentPersonaId` 并保存。
  - 不调用会话修改接口。
  - 后续 `AgentRequest.personaId` 立即使用新值。
  - 聊天区可添加系统提示：`AI 性格已切换为：<label>`。

**验证：**

人工验证：

1. 选择 `friendly` 后发送请求，AIAgent 日志中 `personaId=friendly`。
2. 选择 `concise` 后发送请求，AIAgent 日志中 `personaId=concise`。
3. 设置 TTS 不被本次改造破坏。

## Phase 4：发送/停止请求与响应归属

**目标：** 输入区支持发送/停止双态，所有请求携带当前 userId/sessionId/personaId，并正确处理取消、超时和普通响应。

### Task 4.1 修改 sendTextRequest 请求构造

**Files:**

- Modify: `app/src/main/java/com/hirain/aiagent/test/activity/MainActivity.java`

- [ ] 发送前检查 `currentSessionId`：
  - 如果为空，Toast：`请先新建或选择会话`，不发送。
- [ ] 发送前检查 `isRequestProcessing`：
  - 如果为 true，右侧按钮应该是停止，不允许再次发送。
- [ ] 构造 `AgentRequest` 时写入：
  - `requestId = UUID.randomUUID().toString()`
  - `clientMessageId = UUID.randomUUID().toString()`
  - `userId = currentUserId`
  - `sessionId = currentSessionId`
  - `personaId = currentPersonaId`
  - `sourceApp = "aiagent_test"`
  - `inputType = "TEXT"`
  - `text = 用户输入或 ASR 识别文本`
  - `timestamp = System.currentTimeMillis()`
- [ ] `requestTtsMap` 继续以 requestId 为 key。
- [ ] `processAgentRequest(req)` 返回 0 后调用 `setRequestProcessing(true, requestId, clientMessageId)`。
- [ ] 发送失败时：
  - 从 `requestTtsMap` 删除 requestId。
  - 不进入 processing。
  - Toast：`AIAgent 服务未连接`。

**验证：**

人工验证：

1. 未选择会话时不能发送。
2. 发送后按钮变为停止。
3. 发送后上下文切换入口禁用。

### Task 4.2 增加停止按钮行为

**Files:**

- Modify: `app/src/main/java/com/hirain/aiagent/test/activity/MainActivity.java`
- Add: `app/src/main/res/drawable/ic_stop.xml`

- [ ] 右侧动作按钮根据 `isRequestProcessing` 切换：
  - false 且文本模式：发送图标，输入框非空时启用，执行发送。
  - false 且语音模式：发送图标但禁用，语音发送仍由语音条松手触发。
  - true：停止图标，执行取消。
- [ ] 点击停止时调用：
  - `AIAgent.getInstance().cancelAgentRequest(activeRequestId, "cancelled_by_client")`
- [ ] 如果返回 `ACCEPTED`：
  - 保持 processing=true，等待 AIAgent 回调 `status=CANCELLED`，避免 UI 提前恢复导致旧响应归属混乱。
  - 将停止按钮置为 disabled，并把 contentDescription/text 状态更新为“取消中”，避免重复调用取消。
- [ ] 如果返回 `NOT_FOUND` 或 `ALREADY_FINISHED`：
  - 调用 `setRequestProcessing(false, null, null)`。
  - Toast 显示取消结果。
- [ ] 如果 Binder 调用失败：
  - 调用 `setRequestProcessing(false, null, null)`。
  - Toast：`取消请求失败`。

**验证：**

人工验证：

1. 请求中点击停止，AIAgent 返回取消响应后按钮恢复发送。
2. 取消后不播报普通成功响应。
3. 取消后可以发送下一条。

### Task 4.3 响应归属与 UI 恢复

**Files:**

- Modify: `app/src/main/java/com/hirain/aiagent/test/activity/MainActivity.java`

- [ ] `onAIResponse(response)` 中先检查空响应和空文本。
- [ ] 如果 `activeRequestId != null` 且 response.requestId 等于 activeRequestId：
  - 根据 `response.status/errorType` 判断终态。
  - `SUCCESS/TIMEOUT/CANCELLED/EXCEPTION` 都调用 `setRequestProcessing(false, null, null)`。
  - 对所有终态响应先执行 `Boolean shouldSpeak = requestTtsMap.remove(response.getRequestId())`，确保成功、取消、超时、异常都会清理 TTS 映射。
- [ ] 如果 response.requestId 不是当前 activeRequestId：
  - 对本轮单请求模型来说这是迟到响应。
  - 不改变当前 processing 状态。
  - 执行 `requestTtsMap.remove(response.getRequestId())` 清理残留映射。
  - 可显示为普通系统消息，但不触发 TTS。
- [ ] TTS 播报条件增加：
  - `requestTtsMap.remove(response.getRequestId())` 返回的 shouldSpeak=true。
  - `response.isSuccess()==true`。
  - `response.status` 不是 `CANCELLED/TIMEOUT/EXCEPTION`。
- [ ] 取消响应 `系统: 请求已取消` 显示气泡，但不播报。

**验证：**

人工验证：

1. 普通成功响应恢复发送按钮。
2. 超时响应恢复发送按钮。
3. 取消响应恢复发送按钮且不 TTS。
4. 非当前 requestId 响应不会错误恢复当前请求状态。

## Phase 5：收尾验证与回归

**目标：** 确认新会话/UI/取消能力和已有文字、语音、TTS 能力一起可用。

### Task 5.1 构建验证

**Files:** 无业务文件修改。

- [ ] 执行 Java 编译：

```powershell
.\gradlew.bat :app:compileDebugJavaWithJavac
```

预期：`BUILD SUCCESSFUL`。

- [ ] 执行 APK 打包：

```powershell
.\gradlew.bat :app:assembleDebug
```

预期：`BUILD SUCCESSFUL`。

- [ ] 执行 lint：

```powershell
.\gradlew.bat :app:lintDebug
```

预期：没有 error。若仍有 warning，区分本轮引入和历史 warning，不用 baseline 掩盖权限类 error。

### Task 5.2 人工验收清单

- [ ] 顶部 `AI 测试` 居中，不与左侧按钮重叠。
- [ ] 点击左侧按钮打开侧边栏，点击遮罩或返回键关闭侧边栏。
- [ ] 语音条长按上滑取消时，DrawerLayout 不抢占触摸；松手后 drawer 手势恢复。
- [ ] 服务连接成功后会话列表能刷新。
- [ ] 无会话时侧边栏显示“暂无对话”。
- [ ] 新建会话成功后，聊天区清空并显示新建提示。
- [ ] 切换会话后，聊天区清空并显示切换提示。
- [ ] 删除会话后，侧边栏列表刷新。
- [ ] 切换用户后，会话列表按新 userId 刷新。
- [ ] 切换 AI 性格后，下一条请求携带新的 personaId。
- [ ] 文本输入发送后按钮变停止，响应后恢复发送。
- [ ] 语音模式空闲时右侧发送按钮不可点，AI 处理中变为停止按钮。
- [ ] 请求处理中不能切换用户、会话、AI 性格或删除会话。
- [ ] 请求处理中点击停止，AIAgent 收到 `cancelAgentRequest`。
- [ ] cancel 返回 `ACCEPTED` 后如果回调迟迟不到，TestApp UI watchdog 能恢复操作并提示检查服务状态。
- [ ] 取消响应显示为取消结果，不触发 TTS。
- [ ] 语音识别结果仍以 `inputType="TEXT"` 发送。
- [ ] TTS auto/on/off 行为不被破坏。

## 6. 风险与处理

### 风险 1：TestApp 手工维护 generated AIDL stub，容易和 AIAgent 漂移

处理：本轮明确从 AIAgent 当前 generated stub 同步。后续如果 AIDL 继续频繁变化，建议改成 TestApp 也放 `.aidl` 文件并由 Gradle 生成 stub，减少手工复制风险。

### 风险 2：当前 MainActivity 已经承载过多职责

处理：本轮只拆出 `ConversationAdapter` 和 item 布局，不做大规模架构重写。若后续继续增加图片、工具测试、详细日志面板，再考虑拆 `ConversationController`、`InputController`、`VoiceController`。

### 风险 3：切换 persona 立即影响当前会话，但 AIAgent 会话元信息未必同步更新

处理：计划中明确 TestApp 后续请求使用新 personaId；侧边栏显示的 `ConversationInfo.personaId` 仍以 AIAgent 返回为准，不在客户端篡改。

### 风险 4：取消请求是协作式取消

处理：点击停止后不立即假定请求完全结束；只有收到 `CANCELLED` 响应，或 cancel 返回 `NOT_FOUND/ALREADY_FINISHED`，才恢复发送状态。

## 7. 推荐提交节奏

1. Commit 1：同步 AIDL 客户端协议与 `AIAgent` facade。
2. Commit 2：接入 DrawerLayout 布局与会话列表 Adapter。
3. Commit 3：接入会话/用户/性格状态流。
4. Commit 4：接入发送/停止和响应归属。
5. Commit 5：验证修复与文档补充。

## 8. 自检结果

- 已覆盖用户提出的 6 项任务。
- 已记录讨论确认的产品边界。
- 已限制本轮不做本地历史消息存储和复杂 UI。
- 已明确需要同步的 AIAgent 新 AIDL 类型和 facade 方法。
- 已为每个 phase 给出编译或人工验证方式。
