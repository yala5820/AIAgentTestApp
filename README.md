# AIAgentTestApp — AIAgent 功能测试应用

## 项目概述

AIAgentTestApp 是 [AIAgent](https://github.com/yala5820/AIAgent)（Android 车机 AI 语音助手引擎）的**独立测试客户端**。通过 AIDL 跨进程连接 AIAgentService，提供聊天界面式的交互入口，用于验证 AIAgent 的各项功能（文字对话、语音、图片问答、状态查询等）。

**与 Launcher 的区别：** Launcher 是车机主界面 App，集成语音唤醒、TTS、场景联动等完整车机交互。AIAgentTestApp 是纯功能测试工具，去除了角色前缀、场景联动等业务逻辑，专注于验证 AIAgent 中枢本身的可用性。

### 关键特征

- 纯聊天界面，RecyclerView 气泡式消息列表
- 通过 AIDL 跨进程调用 AIAgentService（`processAgentRequest(AgentRequest)`）
- 支持 TEXT / IMAGE / VOICE 三种请求类型的测试
- 顶部 Toolbar 显示 AIAgent 服务连接状态
- 左上角设置入口（预留）
- 适配车机和手机屏幕

---

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Java |
| UI | RecyclerView + ConstraintLayout / Material 3 |
| 通信 | AIDL（跨进程绑定 AIAgentService） |
| 构建 | Gradle 8.11.1 / AGP 8.9.1 / Version Catalog |
| 最低 API | 24 (Android 7.0) |
| 目标 API | 34 (Android 14) |

---

## 目录结构

```
AIAgentTestApp/
├── build.gradle.kts                             # 顶级构建（空 AGP apply）
├── settings.gradle.kts                          # 项目设置
├── gradle.properties                            # Gradle 全局属性
├── local.properties                             # SDK 路径
├── gradle/
│   ├── libs.versions.toml                       # 版本目录
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradlew / gradlew.bat                         # Gradle 包装脚本
└── app/
    ├── build.gradle.kts                         # 模块构建配置
    └── src/main/
        ├── AndroidManifest.xml                  # Activity + queries 声明
        ├── java/com/hirain/aiagent/             # AIDL 桩 + 客户端类（从 AIAgent 复制）
        │   ├── AgentRequest.java                # 请求体 Parcelable
        │   ├── AgentResponse.java               # 响应体 Parcelable
        │   ├── AIAgent.java                     # Facade 单例（绑定服务 + 发送请求）
        │   ├── IAIAgentAidlInterface.java       # AIDL 接口桩（自动生成）
        │   ├── IAIAgentAidlListener.java        # AIDL 监听器桩（自动生成）
        │   └── IAIAgentServiceListener.java     # 本地回调接口
        └── java/com/hirain/aiagent/test/        # 测试 App 源码
            ├── MainActivity.java                # 聊天主界面 + AIDL 回调
            ├── SettingsActivity.java            # 设置页（预留）
            ├── ChatAdapter.java                 # RecyclerView 气泡适配器
            ├── ChatMessage.java                 # 消息数据模型
            └── MyApplication.java               # 入口：初始化 AIAgent 连接
```

---

## 核心组件

### 1. AIAgent 连接层（`com.hirain.aiagent.*`）

从 AIAgent 项目复制的 6 个 Java 文件，构成与 AIAgentService 通信的完整客户端栈：

```
AIAgent.getInstance().init(context)      ← MyApplication.onCreate()
    ├─ ensureServiceRunning()              → startForegroundService(AIAgentService)
    ├─ bindService()                       → bind to AIAgentService via AIDL
    └─ onServiceConnected()               → m_service 就绪，通知监听器
         │
AIAgent.getInstance().processAgentRequest(req)   ← 发送请求
    └─ m_service.processAgentRequest(req)         → AIDL IPC → AIAgentService
         │
IAIAgentServiceListener.onAIResponse(resp)       ← 接收响应
    └─ MainActivity.onAIResponse()               → UI 更新
```

**AgentRequest 结构：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `requestId` | String | 是 | UUID，关联请求与响应 |
| `sessionId` | String | 是 | 会话标识 |
| `sourceApp` | String | 是 | 来源应用标识 |
| `text` | String | 是 | 用户文本输入 |
| `inputType` | String | 是 | TEXT / IMAGE / VOICE / CONTROL |
| `sceneType` | String | 否 | 场景识别结果 |
| `imagePath` | String | 否 | 图片文件路径 |
| `extraContext` | Map | 否 | 扩展参数 |
| `timestamp` | long | 是 | 请求时间戳 |

**AgentResponse 结构：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `requestId` | String | 关联的请求 ID |
| `sessionId` | String | 关联的会话 ID |
| `success` | boolean | 是否成功 |
| `text` | String | 输出文本 / 错误描述 |
| `errorType` | String | 错误类型（成功时为 null） |
| `timestamp` | long | 响应时间戳 |

### 2. MainActivity（主界面）

聊天式 UI 布局（`activity_main.xml`）：

```
┌──────────────────────────────────┐
│ [⚙]  AI 测试     已连接          │ ← Toolbar + 连接状态
├──────────────────────────────────┤
│ ┌──────────────────────────────┐ │
│ │          你好！我是 AI 测试助手│ │ ← 接收气泡（白色背景，左对齐）
│ │  hello                       │ │
│ │ ────────────────────────────┘ │
│ │                   hi there    │ │ ← 发送气泡（蓝色背景，右对齐）
│ │                   ─────────── │ │
│ └──────────────────────────────┘ │ ← RecyclerView（自动滚到底部）
├──────────────────────────────────┤
│ 请输入消息…                  [→] │ ← EditText + 发送按钮
└──────────────────────────────────┘
```

核心方法：

| 方法 | 作用 |
|------|------|
| `sendTextMessage()` | 读取 EditText → 构造 AgentRequest(TEXT) → processAgentRequest → 清空输入 |
| `saveImageToTempFile()` | 将 Base64 图片解码写入临时文件（IMAGE 请求中使用） |
| `onAIResponse()` | 收到 AgentResponse → 添加到聊天列表 + 滚到底部 |
| `scrollToBottom()` | RecyclerView 自动滚动到最新消息 |

### 3. 消息模型与适配器

- **ChatMessage**：消息模型（type: SENT/RECEIVED, content, timestamp）
- **ChatAdapter**：RecyclerView 适配器，根据 type 设置不同对齐方向 + 气泡背景

### 4. SettingsActivity（设置页）

空占位 Activity，左上角设置按钮跳转至此，待后续扩展。

---

## 运行流程

### 前提条件

1. **AIAgent 应用**必须已在同一设备上安装并运行
2. 设备上已登录 AliCloud DashScope 账号（AIAgent 内置 API Key）
3. 设备已联网（AIAgent 需要连接 `dashscope.aliyuncs.com`）

### 启动流程

```
用户点击 AIAgenTestApp 图标
    │
    ▼
MyApplication.onCreate()
    └→ AIAgent.getInstance().init(this)
        ├─ AIAgentService 启动（跨进程 startForegroundService）
        └─ AIAgentService 绑定（跨进程 bindService）
    │
    ▼
MainActivity.onCreate()
    ├─ 注册 IAIAgentServiceListener
    ├─ 添加欢迎消息到聊天列表
    └─ Toolbar 显示 "连接中…" → "已连接"（回调后更新）
    │
    ▼
用户输入文字 → 点发送
    ├─ 用户消息立即显示在聊天列表（蓝色气泡右对齐）
    ├─ AgentRequest(inputType=TEXT, text=...)
    └─ AIAgent.getInstance().processAgentRequest(req)
    │
    ▼
AIAgentService 处理（约 1-3 秒）
    │
    ▼
IAIAgentServiceListener.onAIResponse(AgentResponse)
    └→ chatAdapter.addMessage() → 显示 AI 回复（白色气泡左对齐）
```

---

## 遗留问题

1. **IMAGE 输入支持**：当前发送仅实现了 TEXT 类型。IMAGE 请求需要图片前端捕获界面（参考 Launcher 的 PictureTextView），以及 Base64 → 临时文件的转换路径已预留方法 `saveImageToTempFile()`
2. **VOICE 输入支持**：当前通过文字输入框发送 TEXT 请求。语音输入需要集成 ASR（语音识别）组件，VOICE 请求用于测试 AI + TTS 联动
3. **AIAgentService 必须预先运行**：测试 App 通过跨进程 AIDL 绑定 AIAgentService，如果 AIAgent 应用未安装或未运行，Toolbar 会显示"未连接"
4. **Android 11+ 包可见性**：需要 `<queries>` 声明对 `com.hirain.aiagent` 包的可见性（已在 manifest 中添加）
5. **设置页为空**：`SettingsActivity` 是预留占位，无实际配置功能
6. **未实现设置页回退导航**：当前 SettingsActivity 无返回按钮处理，需要用户手动按系统返回键

---

## 构建与运行

```bash
# 克隆项目（需先有 AIAgent 项目在同级目录）
cd AndroidStudioProjects/

# 编译
cd AIAgentTestApp
./gradlew :app:compileDebugJavaWithJavac

# 安装
./gradlew :app:installDebug

# 运行前需确保 AIAgent 已安装并运行
# adb shell am start -n com.hirain.aiagent.test/.MainActivity
```
