# Phase 1-2 实施验收报告

> 验收对象：`docs/plan/2026-07-07-conversation-sidebar-user-persona-cancel-plan.md` 中 Phase 1-2 的当前实现  
> 验收时间：2026-07-07  
> 验收结论：Phase 1 基本通过；Phase 2 暂不通过，需要整改后重新验收。

## 1. 验证结果

### 1.1 已执行命令

```powershell
.\gradlew.bat :app:compileDebugJavaWithJavac
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:lintDebug
```

### 1.2 命令结果

- `:app:compileDebugJavaWithJavac`：通过。
- `:app:assembleDebug`：通过。
- `:app:lintDebug`：失败，存在 1 个 lint error、34 个 warning。

### 1.3 AIDL 同步核验

已将 TestApp 当前文件与 AIAgent 当前源文件或 generated stub 进行 SHA256 对比，以下文件完全一致：

- `AgentRequest.java`
- `AgentResponse.java`
- `CancelRequestResult.java`
- `ConversationInfo.java`
- `ConversationListResponse.java`
- `ConversationOperationResult.java`
- `ConversationRequest.java`
- `IAIAgentAidlInterface.java`
- `IAIAgentAidlListener.java`

Phase 1 的协议同步本身未发现字段顺序、Parcelable 读写顺序或 generated AIDL stub 漂移问题。

## 2. 必须整改问题

### P0：lintDebug 失败，当前阶段不能作为可交付状态

位置：`app/src/main/java/com/hirain/aiagent/test/activity/MainActivity.java:308`

当前代码：

```java
drawerLayout.openDrawer(Gravity.START);
```

问题：

`DrawerLayout.openDrawer()` 的 gravity 参数被 lint 限定为 `Gravity.LEFT`、`Gravity.RIGHT`、`GravityCompat.START`、`GravityCompat.END`、`Gravity.NO_GRAVITY` 等常量。当前使用 `Gravity.START` 触发 `WrongConstant` error，导致 `:app:lintDebug` 失败。

影响：

- Phase 2 计划中的 lint 验证无法通过。
- 后续如果将 lint 作为 CI 门禁，本阶段代码无法合入。

建议：

- 使用 `androidx.core.view.GravityCompat.START`。
- 同步替换 import，避免继续使用 `android.view.Gravity` 打开 drawer。

### P1：侧边栏 UI 控件已添加，但 MainActivity 未初始化和绑定，打开后基本不可用

位置：

- `app/src/main/res/layout/activity_main.xml:164`
- `app/src/main/res/layout/activity_main.xml:176`
- `app/src/main/res/layout/activity_main.xml:190`
- `app/src/main/res/layout/activity_main.xml:215`
- `app/src/main/res/layout/activity_main.xml:230`
- `app/src/main/java/com/hirain/aiagent/test/activity/MainActivity.java:100`
- `app/src/main/java/com/hirain/aiagent/test/activity/MainActivity.java:296`

问题：

布局中新增了 `btn_new_conversation`、`tv_drawer_empty`、`rv_conversations`、`btn_drawer_user`、`btn_drawer_ai_settings`，也新增了 `ConversationAdapter`，但 `MainActivity` 当前只初始化了 `drawerLayout`，没有：

- `findViewById()` 获取这些侧边栏控件。
- 为 `rv_conversations` 设置 `LayoutManager` 和 `ConversationAdapter`。
- 为新建对话、用户、AI 设置按钮设置点击事件。
- 根据列表数据切换 `tv_drawer_empty` 和 `rv_conversations` 的显示状态。

影响：

- 点击左上角按钮只能打开一个静态 drawer。
- 会话列表不会展示任何内容。
- “新建对话”“用户”“AI 设置”按钮点击无业务反馈。
- `ConversationAdapter` 虽然编译存在，但当前 Activity 没有使用，无法验收 item 点击、长按删除和 active 高亮。

边界说明：

如果团队刻意把侧边栏数据加载放到 Phase 3，那么 Phase 2 的验收标准应下调为“静态布局可展示”。但当前计划 Phase 2 已明确要求会话列表 Adapter、空状态、item 点击/长按回调和右侧动作按钮基础状态，因此当前实现仍不能按原计划验收通过。

### P1：右侧动作按钮未按 Phase 2 要求保留为发送/停止双态容器

位置：

- `app/src/main/java/com/hirain/aiagent/test/activity/MainActivity.java:225`
- `app/src/main/res/layout/activity_main.xml:142`
- `app/src/main/res/drawable/ic_stop.xml:2`

问题：

Phase 2 计划要求语音模式下不再隐藏右侧动作按钮，空闲时显示发送图标但禁用，处理中切换为停止图标并启用。当前实现仍然沿用旧逻辑：

```java
if (isVoiceMode) {
    btnSend.setVisibility(View.GONE);
} else {
    btnSend.setVisibility(View.VISIBLE);
}
```

同时 `ic_stop.xml` 已新增但未被任何代码引用，lint 也报告 `R.drawable.ic_stop` 未使用。

影响：

- 语音模式下没有右侧停止入口容器，后续接入取消请求时 UI 结构还要返工。
- 当前阶段没有建立发送/停止双态的基础状态机。
- 与计划第 196-200 行的 Phase 2 明确要求不一致。

建议：

- Phase 2 就应先保留 `btnSend` 可见，并通过 `setEnabled(false)`、`setAlpha()` 表示语音空闲不可点。
- 后续 Phase 4 再接入真正的 `isRequestProcessing` 和 `cancelAgentRequest()`。

### P1：DrawerLayout 与语音条触摸事件的冲突处理缺失

位置：`app/src/main/java/com/hirain/aiagent/test/activity/MainActivity.java:243`

问题：

Phase 2 计划要求语音条 `ACTION_DOWN` 时：

- 调用 `drawerLayout.requestDisallowInterceptTouchEvent(true)`。
- 临时 `setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)`。
- 在 `ACTION_UP/ACTION_CANCEL` 和 ASR 回调中恢复 `LOCK_MODE_UNLOCKED`。

当前语音触摸逻辑只处理 ASR 开始、上滑取消和停止识别，没有任何 DrawerLayout 拦截控制。

影响：

- 从屏幕左侧附近长按语音条或上滑取消时，DrawerLayout 可能抢占手势。
- 语音录制中的触摸序列被中断后，可能出现录音状态、语音条 UI 状态和 ASR 状态不一致。
- 这是引入 DrawerLayout 后对已有语音功能的回归风险。

建议：

- 只在录音触摸生命周期内临时锁定 drawer，不要在整个语音模式下锁死 drawer。
- 在 ASR 成功、ASR 失败、上滑取消、Activity 销毁等路径都恢复 drawer lock mode。

## 3. 需要关注的问题

### P2：顶部连接状态与标题布局可用，但需要真机确认是否被挤压

位置：`app/src/main/res/layout/activity_main.xml:51`

当前标题 `tv_title` 在 `top_bar` 中垂直居中，连接状态 `tv_connection_status` 约束在标题下方。该布局可以解决“AI 测试”和左侧按钮重叠问题，但在 56dp 高度内同时显示 18sp 标题和 12sp 状态，部分字体缩放设置下可能显得拥挤。

建议：

- 保留当前方案也可以，但需要在手机/车机实际字体缩放下看一眼。
- 若出现挤压，可把标题略上移，或将连接状态改到右侧/底部更稳定的位置。

### P2：ConversationAdapter 的 active 高亮不一定有可见效果

位置：`app/src/main/java/com/hirain/aiagent/test/adapter/ConversationAdapter.java:77`

当前代码只调用：

```java
holder.itemView.setActivated(isActive);
```

但 item 根布局背景是 `?attr/selectableItemBackground`，该背景是否响应 `state_activated` 取决于主题实现，实际可能看不出当前会话高亮。

建议：

- 若 Phase 3 要验收“当前会话高亮”，应使用明确的 selector drawable 或在绑定时设置背景色。
- 这不是 Phase 1-2 的阻断项，但会影响后续人工验收。

## 4. 已通过项

- AIDL 客户端协议文件与 AIAgent 当前版本一致。
- `AIAgent` facade 已新增会话管理与取消请求 public 方法，并捕获 `RemoteException`。
- `DrawerLayout` 依赖已加入 version catalog 和 app module。
- `activity_main.xml` 根布局已改为 `DrawerLayout`，左侧 drawer 使用 `ConstraintLayout`，宽度为 `300dp`，并设置 `layout_gravity="start"`。
- 顶部标题已改为独立居中 `TextView`，不再依赖 Toolbar title。
- Java 编译和 debug APK 打包通过。

## 5. 验收结论

Phase 1 可以通过。

Phase 2 暂不通过。当前实现完成了部分静态布局，但存在 lint 阻断错误，并且关键 UI 行为没有接线：侧边栏控件不可用、会话列表 Adapter 未挂载、右侧动作按钮没有形成发送/停止双态容器、DrawerLayout 与语音触摸冲突处理缺失。

建议整改顺序：

1. 先修复 `GravityCompat.START`，恢复 `lintDebug` 通过。
2. 接线侧边栏控件和 `ConversationAdapter`，至少让 drawer 打开后具备可见的空状态和按钮反馈。
3. 调整右侧动作按钮在文字/语音模式下的基础状态，为后续取消请求接入做准备。
4. 补上语音条触摸期间的 DrawerLayout 临时锁定和恢复逻辑。
5. 重新执行 `compileDebugJavaWithJavac`、`assembleDebug`、`lintDebug` 后再进入 Phase 3。
