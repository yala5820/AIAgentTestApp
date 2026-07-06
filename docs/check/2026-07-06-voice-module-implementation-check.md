# 语音处理模块执行结果检查

## 审批结论

当前实现已经完成语音模块的主要代码接入，`TEXT` 请求边界、TTS 映射、ASR 停止顺序和基础 UI 形态基本符合计划方向。

但本轮验收结论为：**暂不通过，需要修复后复验**。

阻塞原因集中在两个方面：

1. `lintDebug` 当前失败，存在 `AudioRecord` 权限检查错误。
2. 语音模式切换和录音权限申请的异步时序没有闭环，首次授权或 SDK 初始化失败时可能进入错误状态。

## 已执行验证

| 验证项 | 命令 | 结果 |
|---|---|---|
| Java 编译 | `.\gradlew.bat :app:compileDebugJavaWithJavac` | 通过 |
| 单元测试任务 | `.\gradlew.bat :app:testDebugUnitTest` | 通过，但当前 `NO-SOURCE`，没有实际测试用例 |
| Debug APK 打包 | `.\gradlew.bat :app:assembleDebug` | 通过 |
| Android lint | `.\gradlew.bat :app:lintDebug` | 失败：1 error, 24 warnings |

`lintDebug` 的阻塞错误：

```text
AudioRecorderManager.java:32: Error: Call requires permission which may be rejected by user:
code should explicitly check to see if permission is available (with checkPermission)
or explicitly handle a potential SecurityException [MissingPermission]
```

## 阻塞问题

### P0：`AudioRecord` 构造缺少显式权限防护，导致 lint 失败

位置：`app/src/main/java/com/hirain/aiagent/test/asr/AudioRecorderManager.java:30-32`

当前 `AudioRecorderManager` 构造函数直接创建 `AudioRecord`：

```java
bufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channels, audioFormat);
mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateInHz, channels, audioFormat, bufferSize);
```

虽然上层 `ASRManager.startAsr()` 有 `RECORD_AUDIO` 权限判断，但 lint 无法证明该调用链始终成立；同时运行时权限可能在检查后被系统回收或用户变更。因此这里需要在录音模块内部再做显式保护。

整改要求：

- 在创建 `AudioRecord` 前检查 `Manifest.permission.RECORD_AUDIO`。
- 或明确捕获 `SecurityException` 并将初始化失败状态向上层返回。
- 修复后 `.\gradlew.bat :app:lintDebug` 必须通过，不能用 lint baseline 掩盖这个错误。

### P1：语音模式切换依赖固定 500ms 延迟，没有等待 SDK 初始化结果

位置：`app/src/main/java/com/hirain/aiagent/test/activity/MainActivity.java:172-180`

当前切换到语音模式的逻辑是：

```java
asrManager.getPermissionInit();
tvVoiceBar.postDelayed(() -> {
    asrSdkInitializing = false;
    isVoiceMode = true;
    applyInputMode();
}, 500);
```

这不是可靠的初始化闭环。`getPermissionInit()` 是异步权限和 SDK 初始化流程，但 `MainActivity` 没有收到明确的成功或失败回调，而是 500ms 后直接切到语音模式。

风险：

- SDK 初始化失败时，UI 仍可能显示语音条。
- INTERNET 权限流程未完成时，UI 仍可能进入语音模式。
- 用户看到可录音 UI，但 `startAsr()` 实际会因为 `isAuth=false` 失败。

整改要求：

- `ASRManager.getPermissionInit()` 需要提供初始化结果回调，例如 `onInitSuccess/onInitFailed`。
- `MainActivity` 只能在 SDK 初始化成功后切换到语音模式。
- 初始化失败时保持文本模式，并给出 Toast 或禁用语音入口。

### P1：首次录音权限申请可能在用户松手后继续启动录音

位置：

- `app/src/main/java/com/hirain/aiagent/test/activity/MainActivity.java:207-245`
- `app/src/main/java/com/hirain/aiagent/test/asr/ASRManager.java:119-147`

当前 `ACTION_DOWN` 立即调用 `asrManager.startAsr()`。如果此时还没有 `RECORD_AUDIO` 权限，`startAsr()` 会异步申请权限，并在授权后递归调用 `startAsr()`：

```java
public void hasPermission(List<String> granted, boolean all) {
    if (all) startAsr();
}
```

如果用户在系统权限弹窗期间已经松手，后续授权成功仍可能启动录音，造成录音生命周期脱离长按手势。

风险：

- 用户已经松手，但授权回调后才真正开始录音。
- `ACTION_UP` 可能已经执行过 `stopAsr(false)`，随后权限回调又启动新一轮 ASR。
- UI 可能停在“识别中…”或录音状态与实际 ASR 状态不一致。

整改要求：

- `RECORD_AUDIO` 权限应在进入语音模式前完成，而不是在长按开始时才申请。
- `ACTION_DOWN` 只负责启动已授权、已初始化的录音流程。
- 如果必须在 `startAsr()` 内申请权限，需要加入本次按压是否仍有效的状态校验，用户松手后不得继续启动录音。

## 非阻塞问题

### P2：语音模式偏好保存时机不正确

位置：`app/src/main/java/com/hirain/aiagent/test/activity/MainActivity.java:176-182`

当前 `ttsPrefs.edit().putBoolean("voice_mode", isVoiceMode).apply()` 在延迟切换前执行。切到语音模式时，保存的仍是旧值 `false`，因此“模式记忆”不可靠。

整改建议：

- 在 `isVoiceMode = true` 后保存偏好。
- 切回文本模式时继续保存 `false`。

### P2：lint warning 暂不阻塞本功能验收

`lintDebug` 还有 24 个 warning，包括旧依赖版本、硬编码文本、无应用图标、无障碍 `performClick` 等。它们不是本轮语音功能的主阻塞项，可以后续单独整理。

需要注意：`ClickableViewAccessibility` 与语音条触摸交互有关，后续若要提升可访问性，应补充 `performClick()` 或自定义可点击控件。

## 复验要求

修复后至少重新执行：

```powershell
.\gradlew.bat :app:compileDebugJavaWithJavac
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:lintDebug
```

人工验证至少覆盖：

1. 首次点击麦克风入口时，SDK 初始化失败不会进入语音模式。
2. 首次进入语音模式前已完成 `RECORD_AUDIO` 授权。
3. 权限弹窗期间用户取消或松手，不会在授权回调后自动开始录音。
4. 长按录音、正常松开发送、上滑取消三条路径 UI 状态都能恢复。
5. 语音识别结果发送给 AIAgent 时仍保持 `inputType="TEXT"`。
