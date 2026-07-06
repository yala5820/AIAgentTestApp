# 语音输入与播报功能设计

## 核心方向

本阶段给 TestApp 增加**本地语音输入体验**：
- ASR 在本地将语音转文字
- 识别文本以 `inputType=TEXT` 发送给 AIAgent
- TTS 由 TestApp 本地负责，不触发 AIAgent 的 VOICE 路径
- AIAgent `handleVoiceRequest()` / `mManager?.speak()` 不参与本阶段

## 1. ASR 语音识别

### 1.1 方案

从 Launcher 复制**讯飞 SparkChain SDK** 及封装代码：

| 文件 | 来源 | 处理 |
|------|------|------|
| `app/libs/SparkChain.aar` | Launcher `app/libs/SparkChain.aar` | 直接复制 |
| `app/libs/Codec.aar` | Launcher `app/libs/Codec.aar` | 直接复制（SparkChain 依赖） |
| `test/asr/ASRManager.java` | Launcher `asr/ASRManager.java` | 复制后做以下修改 |
| `test/asr/AudioRecorderManager.java` | Launcher `utils/AudioRecorderManager.java` | 复制后增加 `onAudioData`/`onAudioVolume` 的 `callback != null` 判空，以及 `AudioRecord` 初始化失败的 try-catch |

### 1.2 ASRManager 修改清单

1. **移除 Lombok @Setter**：删除 `import lombok.Setter` 和 `@Setter` 注解，显式写出：
   ```java
   public void setOnAutomaticSpeechRecognitionListener(OnAutomaticSpeechRecognitionListener listener) {
       this.onAutomaticSpeechRecognitionListener = listener;
   }
   ```

2. **新增 startAsr() 包装方法**：封装权限申请、SDK 检查、ASR 启动的完整链路：
   ```java
   public void startAsr() {
       // ① RECORD_AUDIO 未授权 → 动态申请权限，不回调 onError
       if (!XXPermissions.hasPermission(activity, Manifest.permission.RECORD_AUDIO)) {
           XXPermissions.with(activity)
               .permission(Manifest.permission.RECORD_AUDIO)
               .request(new OnPermission() {
                   @Override
                   public void hasPermission(List<String> granted, boolean all) {
                       if (all) startAsr();  // 授权后重试
                   }
                   @Override
                   public void noPermission(List<String> denied, boolean quick) {
                       // 权限被拒绝 → 回调 onError，MainActivity 显示 Toast 并切换回文本模式
                       if (onAutomaticSpeechRecognitionListener != null)
                           onAutomaticSpeechRecognitionListener.onError();
                   }
               });
           return;
       }
       // ② SDK 未初始化 → 回调 onError，不启动录音
       if (!isAuth) {
           if (onAutomaticSpeechRecognitionListener != null)
               onAutomaticSpeechRecognitionListener.onError();
           return;
       }
       // ③ 都就绪 → 启动录音
       isCanceled = false;
       // 此处不再单独复位 isCanceled — 只在这里复一次，后续 onResult/onError 如果已取消就一直吞掉，不第二次复位
       runAsr_Audio();
   }
   ```
   权限被拒绝或 SDK 未就绪 → `MainActivity.onError()` 恢复语音条 UI + Toast。

3. **增加取消状态标志**：新增 `private volatile boolean isCanceled = false;`
   - `stopAsr()` 增加重载 `stopAsr(boolean cancel)`，ASRManager 内部闭环所有清理：
     ```java
     public void stopAsr(boolean cancel) {
         // ① 先切断录音数据流，防止录音线程继续回调 mAsr.write() 到已停止的引擎
         isWrite.set(false);
         if (audioRecorderManager != null) {
             audioRecorderManager.stopRecord();
             audioRecorderManager = null;
         }
         // ② 再停止 ASR 引擎
         if (mAsr != null) {
             if (cancel) {
                 this.isCanceled = true;
                 mAsr.stop(true);   // 立即结束，不等最后一帧
             } else {
                 mAsr.stop(false);  // 等待最后一帧结果，正常发送必须走这里
             }
         }
         isrun = false;
         startMode = "NONE";
     }
     ```
     Launcher 原实现也是先停录音再停 ASR，保持顺序一致。先关 `isWrite` 再释放 `audioRecorderManager`，最后 `mAsr.stop()`，杜绝录音线程向已停引擎写数据的竞态。
     注意 `audioRecorderManager` 和 `isWrite` 是 `ASRManager` 内部成员，调用方无法直接触碰，所以释放逻辑必须在 `stopAsr()` 内闭环
   - `onResult()` 开头加判断：`if (isCanceled) return;` — 取消后吞掉所有后续回调，**不复位 isCanceled**，避免多次回调击穿
   - `onError()` 内加判断：`if (isCanceled) return;` — 取消不是错误，不触发 Toast
   - `isCanceled` 只在一个地方复位：`startAsr()` 入口处 `isCanceled = false`。取消后保持 `true`，直到下次正常启动录音才复位
   - 调用方（MainActivity）只需在取消后恢复语音条 UI 为 "按住说话"

4. **凭证从 Launcher strings.xml 复制**：appid/apikey/apiSecret 保持从 `R.string` 读取（最小改动），从 Launcher 的 `res/values/strings.xml` 复制。**⚠ 这些是测试凭证，不可提交到生产分支**

### 1.3 权限

AndroidManifest.xml 新增：
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
```
本阶段不控制音频焦点/音量，不引入 `MODIFY_AUDIO_SETTINGS`。

### 1.4 初始化位置

- **ASRManager 实例化**：在 `MainActivity.onCreate()` 中 `new ASRManager(this)`（构造函数需要 Activity）
- **语音按钮首次点击时**：先调用 `getPermissionInit()` 初始化 SDK + 申请 INTERNET → 再申请 RECORD_AUDIO → 两者都成功后切换到语音模式
- **SDK 初始化失败恢复**：`isAuth=false` 时 Toast "语音引擎初始化失败"，语音按钮变为灰色禁用状态；用户再次点击麦克风按钮 → 重新调用 `getPermissionInit()` 尝试初始化，成功后解除禁用
- **必须 RECORD_AUDIO 权限**：用户拒绝时 Toast "需要录音权限才能使用语音输入" 并保持在文本模式

---

## 2. TTS 文字转语音

### 2.1 方案

Android 原生 `android.speech.tts.TextToSpeech`，语言设为 `Locale.CHINA`。

### 2.2 播报策略

| 输入方式 | 默认行为 | 说明 |
|----------|----------|------|
| 语音输入 | **自动播报** | 用户通过语音条说话，AI 回复后 TTS 读出 |
| 文本输入 | **不播报** | 键盘输入，只显示不回读 |

设置中的 TTS 全局开关可覆盖以上默认行为：

| 开关值 | 行为 |
|--------|------|
| `auto`（默认） | 语音输入自动播报，文本输入不播报 |
| `on` | 无论哪种输入方式，AI 回复均播报 |
| `off` | 无论哪种输入方式，AI 回复均不播报 |

### 2.3 请求类型关联与清理

`AgentResponse` 不带 `inputType`，需要在发送请求时建立 requestId → shouldSpeak 映射。`shouldSpeak` 根据输入来源 + TTS 模式统一计算：

```java
// tts_mode 取值为 "auto" / "on" / "off"
private boolean computeShouldSpeak(boolean isVoiceInput) {
    String mode = ttsPrefs.getString("tts_mode", "auto");
    if ("off".equals(mode)) return false;
    if ("on".equals(mode)) return true;
    return isVoiceInput;  // auto 模式：仅语音输入播报
}
```

**所有请求**都写入 `requestTtsMap`（不只是语音请求），确保 `tts_mode=on` 时文本输入也播报：

```java
// 发送任意请求前 — 文本/语音都走这里
boolean shouldSpeak = computeShouldSpeak(isVoiceInput);
requestTtsMap.put(req.getRequestId(), shouldSpeak);
int ret = AIAgent.getInstance().processAgentRequest(req);
if (ret != 0) {
    requestTtsMap.remove(req.getRequestId());  // 发送失败，清理残留
    Toast.makeText(this, "AIAgent 服务未连接", Toast.LENGTH_SHORT).show();
}
```

**线程安全**：`requestTtsMap` 的 put（发送时，在 UI 按钮回调/触摸事件中）和 remove（回调时，在 `onAIResponse()` 的 `runOnUiThread()` 内部）都在主线程执行，无需 `ConcurrentHashMap`。

**内存清理**：`onDestroy()` 中清空 `requestTtsMap.clear()` + `mTextToSpeech.stop()`，防止 Activity 销毁后残留映射或 TTS 继续播放。

**TTS 就绪检查**：新增 `private boolean isTtsReady = false;`，在 `TextToSpeech.OnInitListener` 中设置：
```java
// onInit:
if (status == TextToSpeech.SUCCESS) {
    int langResult = mTextToSpeech.setLanguage(Locale.CHINA);
    isTtsReady = (langResult != TextToSpeech.LANG_MISSING_DATA
               && langResult != TextToSpeech.LANG_NOT_SUPPORTED);
}
```

播报前检查：
```java
Boolean shouldSpeak = requestTtsMap.remove(response.getRequestId());
if (shouldSpeak != null && shouldSpeak && isTtsReady) {
    mTextToSpeech.speak(response.getText(), TextToSpeech.QUEUE_FLUSH, null, "ai_response");
}
```
`isTtsReady=false` 时静默降级为只显示文本，不崩溃。

### 2.4 TTS 生命周期

- `onCreate()` 中 `new TextToSpeech(this, listener)`，listener 中 `setLanguage(Locale.CHINA)`
- `onDestroy()` 中 `mTextToSpeech.stop(); mTextToSpeech.shutdown();`

---

## 3. UI 交互设计

### 3.1 输入区域两种模式

**文本模式：**

```
┌──────────────────────────────────────────┐
│ [🎤] ┌──────────────────────────┐ [→发送]│
│      │  请输入消息…              │        │
│      └──────────────────────────┘        │
└──────────────────────────────────────────┘
```

**语音模式（麦克风按钮在左侧，输入区域变为语音条，发送按钮隐藏）：**

```
┌──────────────────────────────────────────┐
│ [⌨] ┌──────────────────────────┐        │
│      │     按住说话              │        │
│      └──────────────────────────┘        │
└──────────────────────────────────────────┘
```

### 3.2 切换按钮

- 位置：输入区域左侧
- 图标：麦克风（文本模式）/ 键盘（语音模式）
- 行为：点击切换 `isVoiceMode`
- 模式记忆：`SharedPreferences` 保存上次模式
- 语音模式下**隐藏右侧发送按钮**

### 3.3 语音条 — 微信式长按交互

语音条是一个 `TextView` + `OnTouchListener`：

| 手势 | 状态 | 语音条文字 | 背景色 |
|------|------|-----------|--------|
| 手指按下 | 开始录音 | "松开 发送" | `bg_voice_bar_recording`（高亮） |
| 上滑超过阈值 | 准备取消 | "松开 取消" | `bg_voice_bar_cancel`（红色） |
| 上滑后滑回原位 | 恢复录制 | "松开 发送" | `bg_voice_bar_recording` |
| 松开（正常区域） | 发送 | → `ASRManager.stopAsr()` | — |
| 松开（取消区域） | 取消 | → `ASRManager.stopAsr(true)` | — |

取消区域判定：手指 Y 坐标 < 按钮顶部 - 阈值（约 80dp）。

### 3.4 录音状态反馈

| 阶段 | 语音条显示 |
|------|-----------|
| 按压中 | "松开 发送"（高亮背景 + 呼吸透明度动画） |
| 松开 → ASR 处理中 | "识别中…"（不可点击，灰色） |
| 识别完成 | 恢复到 "按住说话"（可再次录音） |
| 识别失败/空结果 | Toast "未识别到语音" → 恢复到 "按住说话" |

---

## 4. 语音输入完整流程

```
用户长按语音条
  → ASRManager.startAsr() → AudioRecorderManager.startRecord()

用户松开（正常区域）
  → ASRManager.stopAsr(false) → AudioRecorderManager.stopRecord()

ASRManager.onResult(text)
  → text 为空 → Toast "未识别到语音" → 恢复语音条
  → text 有效 → 构造 AgentRequest
      req.setInputType("TEXT")   ← 注意：TEXT，不是 VOICE
      req.setText(text)
      requestTtsMap.put(req.getRequestId(), shouldSpeak)  ← 标记是否播报
      int ret = AIAgent.getInstance().processAgentRequest(req)
      if (ret != 0) {
          requestTtsMap.remove(req.getRequestId())   ← 发送失败，清理残留映射
          Toast "AIAgent 服务未连接" → 恢复语音条
      }

用户上滑松开（取消区域）
  → ASRManager.stopAsr(true)    ← isCanceled=true, mAsr.stop(true)
  → AudioRecorderManager.stopRecord()
  → isWrite = false
  → onResult/onError 均被 isCanceled 拦截，不触发回调
  → 恢复语音条为 "按住说话"

AIAgentService 正常处理 TEXT 请求
  → chatOrchestrator.execute(text)
  → 返回 AgentResponse

MainActivity.onAIResponse(response)
  → 显示 AI 回复气泡
  → 查询 requestTtsMap，如果需要播报 → TTS 读出
  → 如果不需要播报 → 纯文本显示
```

### 4.1 与 AIAgent 的 VOICE 路径关系

**本阶段不涉及 AIAgent 的 `handleVoiceRequest()`。** 语音输入在 TestApp 侧完成文字转换后以 `TEXT` 类型发送，AIAgent 照常处理普通的文字聊天请求。后续如果要测试 AIAgent 的 VOICE 路径（含 AIAgent 端 TTS），另外规划。

---

## 5. TTS 设置

点击 Toolbar 设置按钮 → 在当前 `SettingsActivity` 中增加 TTS 选项（本阶段直接弹 `AlertDialog` 即可，不依赖侧边栏）：

```
┌──────────────────────┐
│ TTS 播报设置          │
│                      │
│ ○ 自动（仅语音输入）  │ ← 默认
│ ○ 强制开启            │
│ ○ 强制关闭            │
└──────────────────────┘
```

选择后写入 `SharedPreferences("tts_settings").edit().putString("tts_mode", "auto"|"on"|"off")`。

---

## 6. 依赖变更

### 6.1 AAR 文件

从 Launcher 复制到 TestApp：
```
cp Launcher/app/libs/SparkChain.aar  AIAgentTestApp/app/libs/
cp Launcher/app/libs/Codec.aar       AIAgentTestApp/app/libs/
```

### 6.2 Version Catalog 新增

`gradle/libs.versions.toml`（沿用 Launcher 已验证坐标，不做升级以避免包名兼容问题）：

```toml
[versions]
xxpermissions = "8.2"

[libraries]
xxpermissions = { group = "com.hjq", name = "xxpermissions", version.ref = "xxpermissions" }
```

### 6.3 build.gradle.kts 新增

```kotlin
dependencies {
    implementation(libs.xxpermissions)
    implementation(files("libs/SparkChain.aar"))
    implementation(files("libs/Codec.aar"))
}
```

---

## 7. 文件变更清单

### 新增

| 文件 | 来源 | 职责 |
|------|------|------|
| `app/libs/SparkChain.aar` | Launcher 同名路径 | 讯飞 ASR 引擎 |
| `app/libs/Codec.aar` | Launcher 同名路径 | SparkChain 依赖 |
| `test/asr/ASRManager.java` | Launcher `asr/ASRManager.java` | ASR 封装（删除 Lombok + startAsr 包装 + 取消状态） |
| `test/asr/AudioRecorderManager.java` | Launcher `utils/AudioRecorderManager.java` | 录音机桥接 |
| `res/drawable/bg_voice_bar_normal.xml` | 新增 | 语音条默认背景 |
| `res/drawable/bg_voice_bar_recording.xml` | 新增 | 录音中背景 |
| `res/drawable/bg_voice_bar_cancel.xml` | 新增 | 取消中背景 |
| `res/drawable/ic_mic.xml` | 新增 | 麦克风矢量图标 |
| `res/drawable/ic_keyboard.xml` | 新增 | 键盘矢量图标 |

### 修改

| 文件 | 改动 |
|------|------|
| `activity/MainActivity.java` | TTS 初始化/生命周期；输入模式切换；语音条 OnTouchListener；requestTtsMap 映射+清理；isVoiceMode 状态管理；ASR 权限申请 |
| `res/layout/activity_main.xml` | 输入区域加切换按钮 + 语音条 TextView；语音模式下隐藏发送按钮 |
| `build.gradle.kts` | AAR 文件依赖 + XXPermissions |
| `gradle/libs.versions.toml` | XXPermissions |
| `AndroidManifest.xml` | RECORD_AUDIO + INTERNET 权限（不引入 MODIFY_AUDIO_SETTINGS） |
| `res/values/strings.xml` | ASR 凭证（appid/apikey/apiSecret）从 Launcher 复制。当前仓库属测试用途，可接受提交；若后续转为生产仓库，需迁移到 `local.properties` |

---

## 8. 验证

1. **编译**：`./gradlew :app:compileDebugJavaWithJavac`
2. **功能**：
   - 点击麦克风按钮 → 切换到语音条 → 隐藏发送按钮
   - 长按语音条 → 显示 "松开 发送"，有录音动画
   - 松开 → 识别文字 → 以 TEXT 类型发送 → AI 回复显示 → TTS 自动播报
   - 上滑 → 显示 "松开 取消" → 松开 → 不发送请求，不触发识别回调，恢复语音条
   - 识别失败/空结果 → Toast → 恢复语音条
   - `processAgentRequest` 返回非零 → `requestTtsMap` 清理 + Toast
   - 切换回键盘模式 → 打字发送 → 无 TTS 播报
   - 设置 TTS=off → 语音输入也不播报
   - onDestroy() → TTS stop + shutdown, ASR stop, AudioRecorder release
