# AIAgentTestApp — AIAgent 功能测试应用

## 项目概述

AIAgentTestApp 是 [AIAgent](https://github.com/yala5820/AIAgent)（Android 车机 AI 语音助手引擎）的**独立测试客户端**。通过 AIDL 跨进程连接 AIAgentService，提供聊天界面式的交互入口，用于验证 AIAgent 的各项功能（文字对话、语音、图片问答、状态查询等）。专注于验证 AIAgent 中枢本身的可用性。

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

---## Working Rules

本准则规定了在当前代码仓库中的执行规范。

### Think Before Coding
- 任务需求模糊时，切勿直接修改文件。
- 先查阅相关文件，说明现有代码实现逻辑。
- 正式编码前，明确列出所有预设前提。
- 若需求存在多种解读方向，列出全部可选方案，不擅自选定其中一种。
- 拿不准时优先简洁提问确认需求，而非贸然做出有风险的猜测。

### Simplicity First for simple problems
- 对于小问题采用能解决需求的最小改动方案。
- 不新增预判性功能、抽象层、配置层或多余扩展能力。
- 若无充分合理说明，不引入新依赖包。
- 若解决方案代码量持续膨胀，暂停操作并给出更轻量化的替代方案。

### Surgical Changes
- 仅改动和任务直接相关的文件。
- 不重构无关业务代码。
- 不格式化本次修改无关的文件。
- 遵循项目现有代码风格，即便其他编码风格更优也保持统一。
- 若发现无关的废弃代码或可疑代码，仅在总结中备注，不擅自修改。

### Goal-Driven Execution

所有复杂任务均遵循以下步骤：

1. 定位需要改动的相关文件
2. 说明代码当前运行逻辑
3. 提出最小化实现方案
4. 方案确认清晰后再执行编码修改
5. 使用适配的命令或人工核验，验证修改效果
6. 汇总改动文件、验证结果与尚存风险

### 禁止操作

无用户明确指令时，严禁执行以下操作：

- 未经许可，不执行 `rm -rf` 等具有破坏性的文件操作
- 未经许可，不修改 `.env`、密钥、凭证及本地机器配置文件
- 未经许可，不改动依赖版本与构建脚本
- 未经许可，不进行大规模架构重写

## 用户偏好设定

### 代码注释要求

所有新增代码注释、文档字符串统一使用详尽中文编写。代码需做到自解释，注释重点说明**设计原因**，而非单纯复述代码功能；简单逻辑使用简短单行注释；仅当函数逻辑晦涩难懂时，才编写多行文档字符串。

### 代码修改后的回复格式

完成代码改动后，输出结构化总结，包含三部分内容：

1. **工作目标**：本轮工作要完成什么，或者要解决什么问题。
1. **修改内容与逻辑**：汇总每项改动，包括位置、设计思路或原因、具体做了什么。
2. **工作总结**：首先总结本轮执行任务情况，然后总结当前项目验证状态及遗留风险等