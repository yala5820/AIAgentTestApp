# AIAgentTestApp GPT 风格 UI 与悬浮 Doge 宠物实施计划

> 文档状态：实施完成；设备验收按当前可用环境完成，受限项目已如实记录
>
> 编写日期：2026-07-13
>
> 严格复审日期：2026-07-14
>
> 执行模式：Goal 模式
>
> 当前进度：三个执行包均已实现，构建与静态检查通过，手机竖屏模拟尺寸和车机横屏已完成核心交互验收
> 复审结论：目标与边界可行；原执行部分存在若干实现歧义，已在本版补齐为可直接执行的工程方案

## 1. Goal 契约

### 1.1 工作目标

在不改变 AIAgentTestApp 现有 AIDL、会话、文字请求、ASR、TTS 和取消请求行为的前提下，完成两项相互配合的改进：

1. 将聊天主界面重设计为接近 GPT 的简洁、专业、克制的浅色界面，以手机竖屏为第一适配目标，同时保证常见车机横屏比例下能够正常操作。
2. 增加一个仅存在于 TestApp 内部的 Doge 悬浮宠物，支持待机动画、拖动、贴边、单击操作、双击动画、隐藏与恢复，并为后续替换成用户自制动画保留清晰边界。

### 1.2 完成定义

只有同时满足以下条件，本 Goal 才能判定完成：

- 主界面、消息区、输入区、侧边栏形成统一的 GPT 风格视觉语言。
- 用户切换入口位于聊天主界面右上角，侧边栏不再保留重复用户入口。
- 手机竖屏可完整使用；车机横屏下内容不过度拉伸，聊天、输入、侧边栏和宠物均处于可操作范围。
- 初始聊天页使用独立欢迎空状态，不再通过伪造一条 AI 消息实现欢迎语；真实系统提示和对话消息仍进入消息列表。
- Doge 宠物能够稳定播放图集动画，且单击、双击、拖动三类手势不会互相误触。
- 宠物可以隐藏为边缘小入口并恢复，显示状态和相对位置能够保存。
- 原有文字发送、语音输入、TTS、发送/停止、会话切换、用户切换和 AI 设置行为没有回归。
- `processDebugResources`、`compileDebugJavaWithJavac`、`assembleDebug` 和 `lintDebug` 通过；人工验收矩阵完成并记录真实结果。

### 1.3 当前状态

- 已完成目标确认、边界确认、现状代码检查和 Doge 图集格式核验。
- 已完成 GPT 风格响应式 UI、右上角用户入口迁移和应用内 Doge 悬浮宠物实现。
- 已完成资源编译、Java 编译、Debug APK 构建和 Lint，并在可用模拟器范围内完成核心人工验收。
- 现有 ASR SDK 不提供 x86_64 原生库，且模拟器中的 AIAgentService 进程存在既有异常，因此文字请求、ASR、TTS 等端到端业务回归仍需在 ARM 手机或真实车机上补验。

## 2. 已确认的产品决策

### 2.1 UI 风格

- 采用接近 GPT 的设计语言，但不复制 OpenAI 品牌标识或专有素材。
- 以白色、暖浅灰、深灰文字为主，交互色克制使用。
- 不使用大面积渐变、强阴影、模板紫色或装饰性动画。
- AI 回复采用无明显气泡或弱背景的左侧内容样式。
- 用户消息采用右对齐浅灰圆角气泡。
- 输入区采用居中、圆角、轻边框的悬浮输入容器。
- 本轮只做浅色主题，不新增深色模式。

### 2.2 屏幕适配

- 手机竖屏是第一验收目标。
- 车机横屏不新增独立业务页面，使用同一布局配合最大内容宽度和尺寸资源适配。
- 聊天列表和输入区在宽屏上限宽居中，不铺满整个车机屏幕。
- 消息最大宽度根据聊天容器实际宽度计算，不继续使用固定 `280dp`。
- 侧边栏在手机上保持紧凑，在 `w600dp` 及以上宽度仅适度增加宽度。
- 主要点击区域不小于 `48dp`。

### 2.3 顶部栏与侧边栏

- 左上角使用真正的菜单图标打开会话侧边栏，移除“旋转发送图标充当菜单”的临时实现。
- 中间显示简洁标题和当前会话/连接状态。
- 右上角增加圆形用户入口，使用 1～2 个字符显示当前用户：`default_user` 显示 `D`，`test_user_1/2` 显示 `T1/T2`，其他值回退为首字符大写；完整 userId 放入无障碍描述。
- 点击右上角用户入口复用现有用户切换逻辑；请求处理中继续禁止切换用户。
- 侧边栏移除底部“用户”按钮，只保留新建对话、会话列表、空状态和 AI 设置入口。
- 当前会话使用圆角弱高亮，不再由 Adapter 写入硬编码紫色色块。

### 2.4 宠物行为

- 宠物是应用内悬浮 View，不是 Android 系统悬浮窗。
- 不申请 `SYSTEM_ALERT_WINDOW`，不新增前台服务。
- 默认显示在聊天区域右下方、输入区上方。
- 普通状态循环播放 Idle。
- 拖动时根据水平方向播放 Run Right 或 Run Left。
- 松手后贴近左右边缘并恢复 Idle。
- 单击宠物显示轻量操作浮层，第一版只提供“隐藏”。
- 双击宠物播放一次 Jumping，结束后恢复 Idle。
- 单击判定等待双击窗口结束，避免双击时误弹操作浮层。
- 点击宠物外部时关闭操作浮层。
- 隐藏时宠物缩小进入最近边缘，保留一个小型 Doge 入口；不额外设计新的宠物图形。
- 点击边缘入口恢复宠物并播放一次 Waving。
- 保存宠物是否隐藏以及相对位置；旋转、尺寸变化和键盘弹出后重新约束到可见区域。
- 位置持久化明确保存为“左/右边缘 + 垂直比例”，不保存易受分辨率影响的绝对像素。
- 第一版不把宠物动画与 AIAgent 请求状态、成功/失败状态深度绑定。

### 2.5 临时素材边界

- 临时素材来源：`https://codex-pets.net/#/pets/doge`。
- 站点作者标识：`seymour`。
- 使用目的：个人、本地、非商业 TestApp 测试。
- 保留原始 `spritesheet.webp`，不重新绘制 Doge 形象。
- 在项目内记录来源、作者、获取日期和“临时测试素材”属性。
- 如果未来公开源码、公开分发 APK 或转为正式产品，必须先替换为用户自制素材或取得作者明确许可。

## 3. 当前实现与主要差距

| 区域 | 当前实现 | 本轮需要解决的问题 |
|---|---|---|
| 顶部栏 | 自定义 `top_bar`，菜单按钮复用并旋转 `ic_send`；另有一个始终 `gone` 的无效 `Toolbar` | 更换真实菜单图标；删除无效 Toolbar；增加右上角用户入口；重新组织标题和状态层级 |
| 顶部状态 | `refreshHeaderState()` 默认按“已连接”拼接 userId/sessionId，会覆盖断开状态 | 建立独立连接状态字段；连接状态、会话摘要和用户标识分别渲染，不能相互覆盖 |
| 聊天列表 | `RecyclerView` 全宽，统一 `12dp` padding；`onCreate()` 立即插入欢迎消息 | 增加宽屏限宽与真正空状态；删除伪消息式欢迎语；保持真实消息滚动逻辑不变 |
| 消息样式 | 双方均为明显气泡，最大宽度固定 `280dp` | AI 弱气泡、用户浅灰气泡；根据容器动态计算最大宽度 |
| 输入区 | 白色横条、无输入容器边界、裸图标按钮 | 改为居中圆角输入容器；统一语音、发送和停止视觉状态 |
| 侧边栏 | 普通 Button 和硬编码选中颜色，底部用户/AI 设置双按钮 | 优化新建按钮、空状态和选中态；移除重复用户入口 |
| 颜色主题 | 保留 Material 模板紫色、多个 XML/Java 硬编码颜色 | 建立语义颜色资源并清理本轮涉及区域的硬编码色值 |
| 宠物 | 不存在 | 新增独立图集渲染、交互、位置保存和生命周期管理 |

### 3.1 严格复审发现与处理结论

| 编号 | 原计划漏洞 | 风险等级 | 本版处理 |
|---|---|---:|---|
| R1 | 初始欢迎消息会让“聊天空状态”永远不可见 | 高 | 明确删除 `onCreate()` 中的欢迎消息注入，改为 Adapter 数据驱动的空状态 |
| R2 | 连接状态没有独立状态源，刷新会话后可能把“未连接”覆盖成 userId | 高 | 增加仅服务于 UI 的连接状态字段，并规定唯一 Header 渲染入口 |
| R3 | 消息最大宽度只描述“约 78%”，未说明何时计算、旋转后如何更新 | 中 | 明确由 `RecyclerView` 布局变化回调计算并传给 Adapter |
| R4 | 宠物直接位于主层，顶部栏、输入区、IME 和系统区域的坐标边界不稳定 | 高 | 新增受约束的 `pet_overlay`，坐标只相对该容器计算 |
| R5 | 原手势描述没有解决 Doge 贴左边缘时与 Drawer 边缘手势竞争 | 高 | 命中宠物的 `ACTION_DOWN` 立即禁止父级拦截，结束或取消时恢复 |
| R6 | 单次动画、拖动、隐藏、恢复之间没有优先级和中断规则 | 高 | 增加显式状态转换表和动画完成回调规则 |
| R7 | “归一化位置”没有定义持久化字段和恢复公式 | 中 | 固定为 `edge + verticalFraction + hidden` 三个字段 |
| R8 | 未规定图集帧率，实际实现仍需临场猜测 | 中 | 根据素材无动画元数据的事实，明确第一版各状态帧间隔 |
| R9 | 单击操作浮层未选定具体实现，外部点击关闭和生命周期存在歧义 | 中 | 固定使用现有 AppCompat 的 `PopupMenu`，不新增依赖 |
| R10 | 原计划只有改后构建，没有改前基线，难以区分既有问题 | 中 | 增加实施前基线步骤和逐包证据记录 |
| R11 | 只写了 `onDestroy()` 释放，Activity 进入后台后仍可能继续刷新动画 | 中 | 补充 View attach/window visibility 级别暂停与恢复 |
| R12 | 没有说明无法获得设备时如何判定完成 | 中 | 区分“代码完成”和“设备验收完成”；缺少设备证据时不得宣称完整验收 |

## 4. 技术方案

### 4.1 UI 资源策略

新增或整理以下语义资源，避免继续按具体颜色名称组织界面：

- 页面背景、表面背景、输入区背景。
- 主文字、次级文字、弱提示文字。
- 用户消息背景、AI 消息背景/边框。
- 会话选中背景、分割线、连接成功/未连接状态色。
- 统一的圆角、间距、内容最大宽度、侧边栏宽度和宠物尺寸。

使用 `values/dimens.xml` 保存手机默认尺寸，使用 `values-w600dp/dimens.xml` 只覆盖宽屏必要参数。除非实际验证证明同一布局无法满足车机比例，否则不新增 `layout-land` 的重复布局。

第一版直接采用以下设计令牌，实施时不再临场选择颜色或尺寸；只有设备验收出现明确可用性问题时才允许微调，并记录原因：

| 令牌 | 默认值 | 用途 |
|---|---:|---|
| `color_page_background` | `#FFFFFF` | 主页面背景 |
| `color_surface_subtle` | `#F7F7F8` | 输入区、弱表面 |
| `color_text_primary` | `#202123` | 主文字、深色操作按钮 |
| `color_text_secondary` | `#6B6B6B` | 次级状态与说明 |
| `color_border_subtle` | `#E5E5E5` | 输入区、分隔线 |
| `color_user_message` | `#F4F4F4` | 用户消息背景 |
| `color_conversation_selected` | `#ECECEC` | 当前会话弱高亮 |
| `color_status_connected` | `#2E7D32` | 已连接状态 |
| `color_status_disconnected` | `#B3261E` | 未连接状态 |
| `top_bar_height` | `64dp` | 容纳标题和单行状态，不继续使用不明确的 `actionBarSize` |
| `chat_content_max_width` | 手机不限宽；`w600dp` 为 `840dp` | 聊天列表和输入区共同宽度上限 |
| `message_width_ratio` | `0.78` | 消息相对聊天可用宽度比例，仅在 Java 计算 |
| `message_width_cap` | 手机 `520dp`；`w600dp` 为 `640dp` | 防止车机长行过宽 |
| `drawer_width` | 手机 `300dp`；`w600dp` 为 `360dp` | 侧边栏宽度 |
| `pet_width/height` | 手机 `88×96dp`；`w600dp` 为 `96×104dp` | 保持 `192:208` 图集单元格比例 |
| `pet_hidden_width/height` | `48×52dp` | 隐藏边缘入口，保持图集比例且点击宽高均不小于 `48dp` |
| `pet_edge_margin` | `8dp` | 宠物与 Overlay 边缘间距 |

状态栏和导航栏保持浅色背景并启用深色系统图标；本轮不切换到全屏 edge-to-edge，继续依赖现有 `fitsSystemWindows` 和 `adjustResize`，降低 API 24 与车机系统差异带来的风险。

### 4.2 主界面层级

继续保留现有 `DrawerLayout`，主内容仍由一个 `ConstraintLayout` 承载，但宠物必须放入明确受约束的 Overlay：

```text
DrawerLayout
├── Main ConstraintLayout
│   ├── Top bar：菜单 / 标题状态 / 用户入口
│   ├── Chat RecyclerView
│   ├── Empty state
│   ├── Pet FrameLayout overlay（仅覆盖 Top bar 与 Input 之间）
│   │   └── PetSpriteView
│   └── Rounded input surface
└── Drawer content
    ├── 新建对话
    ├── 会话列表 / 空状态
    └── AI 设置
```

具体布局约束如下：

- 删除始终 `gone` 的 `Toolbar` 兼容占位以及 MainActivity 中对应字段和 import。
- 将 `btn_settings` 重命名为语义正确的 `btn_menu`，使用新菜单 VectorDrawable，不再旋转发送图标。
- 顶部标题容器宽度为 `0dp`，约束在 `btn_menu` 与 `btn_user` 之间；标题和状态都设置单行、省略号，避免窄屏与大字体时覆盖两侧按钮。
- `rv_chat` 和 `input_layout` 同时使用左右约束、横向 margin 和 `layout_constraintWidth_max`，在手机填满可用宽度，在宽屏自动限宽居中。
- `pet_overlay` 是不可点击、不可聚焦的透明 `FrameLayout`，顶部约束到 `top_bar` 底部，底部约束到 `input_layout` 顶部；因此 IME 触发 `adjustResize` 后其真实高度会自动缩小。
- 只有 `PetSpriteView` 自身命中区域处理触摸；Overlay 空白区域必须返回未消费，让事件继续到聊天列表。
- 宠物 Overlay 在 RecyclerView 之后声明，保证宠物显示于消息之上；它仍属于 Drawer 的主内容子树，因此 Drawer 打开时自然位于其下方。
- Drawer 开始打开时关闭宠物操作菜单；Drawer 拦截触摸导致 `ACTION_CANCEL` 时，宠物控制器必须清理拖动态并恢复父级拦截。

### 4.3 消息宽度与样式

- `MainActivity` 给 `rv_chat` 注册 `OnLayoutChangeListener`。每当宽度变化时，按 `可用宽度 = RecyclerView 宽度 - paddingStart - paddingEnd` 计算。
- 消息最大宽度公式固定为 `min(round(可用宽度 × 0.78), message_width_cap)`；结果必须大于 0 且与上一次不同才调用 `ChatAdapter.setMessageMaxWidthPx()`。
- `ChatAdapter` 保存该像素值，在 `onBindViewHolder()` 中调用 `TextView.setMaxWidth()`；宽度变化后只刷新可见/现有消息，不在每个 ViewHolder 内重复注册布局监听。
- 首次布局尚未得到有效宽度时，使用 XML 中的保守默认 `280dp`，得到容器宽度后立即替换；旋转和宽屏变化通过同一监听自动重算。
- 用户消息右对齐，使用浅灰圆角背景。
- AI 消息左对齐，使用透明或接近页面背景的弱样式，保持足够内边距和行距。
- 不改变 `ChatMessage` 的业务字段和消息类型定义。
- `ChatAdapter` 的 `addMessage()`、`clearMessages()` 继续作为消息变更入口，不改变消息数据模型。

### 4.4 聊天空状态的数据规则

- 删除 `MainActivity.onCreate()` 中人为添加“你好！我是 AI 测试助手……”的消息；该文字改为布局中的静态欢迎标题，例如“有什么可以帮忙的？”。
- 欢迎空状态不是消息，不进入 Adapter，不参与 TTS，不保存到会话，也不触发滚动。
- `MainActivity` 为 `ChatAdapter` 注册一个 `RecyclerView.AdapterDataObserver`，统一根据 `getItemCount() == 0` 切换 `empty_chat_state` 和 `rv_chat` 的可见性，避免每个添加/清空调用点手动同步。
- 新建会话、切换会话、删除当前会话和切换用户时，当前代码仍会添加真实的操作提示消息；这些提示存在时空状态应隐藏。本轮不改变这些提示的业务语义。
- `scrollToBottom()` 增加 `itemCount > 0` 防护，避免未来空列表调用出现 `-1` 位置。
- `AdapterDataObserver` 在 `onDestroy()` 中注销，保持生命周期对称。

### 4.5 用户入口与 Header 状态迁移

- 将现有 `mBtnDrawerUser` 对应的点击处理迁移到顶部右侧用户 View。
- 用户列表、SharedPreferences、切换后清空消息、重新加载会话和请求处理中禁用等逻辑保持不变。
- `setRequestProcessing()` 改为禁用顶部用户入口，而不是已经移除的 Drawer 用户按钮。
- 在 `MainActivity` 内增加私有枚举 `ConnectionUiState { CONNECTING, CONNECTED, DISCONNECTED }`。`onCreate()` 初始为连接中；服务连接/断开回调只更新该状态并调用 `refreshHeaderState()`。
- `refreshHeaderState()` 成为唯一 Header 渲染入口，不能再由连接回调直接写 TextView 后又被会话刷新覆盖。
- 标题固定为“AI 测试”；状态行映射为：连接中、未连接、`已连接 · 未选择会话`、`已连接 · 会话 <sessionId 后 8 位>`。
- 顶部用户入口文字由独立 `formatUserBadge(userId)` 产生，完整 userId 只放在 contentDescription 和切换对话框选中项中，不再挤入连接状态行。
- 用户切换成功后按既有顺序保存 userId、清空 session、清空消息、加载该用户会话，最后统一刷新 Header；请求处理中入口保持 disabled，视觉上通过 state-list 资源降低透明度。
- 将当前匿名 lambda 中的用户对话框逻辑提取为 `showUserSwitchDialog()`，顶部按钮只调用该方法，避免迁移时复制出两套逻辑。

### 4.6 Doge 素材基线与引入门槛

2026-07-14 复审时再次读取已下载测试包，确认包内只有 `pet.json` 和 `spritesheet.webp`：

- `pet.json` 只包含 id、显示名、描述和图集路径，没有帧率、动作范围、作者或许可证字段。
- ZIP 大小约 `1.90 MiB`，SHA-256：`3F2E4EF2C2289901FA5387FBE86E76E63B35C19969141B7A3B6BBAC5F1AFBB24`。
- `spritesheet.webp` 大小 `1,988,222` 字节，SHA-256：`E2C7216E564F375C2F60AAD32475574FDB50EFFF7896D1066548746C767D8AF2`。
- 上述哈希只作为本轮测试基线；实施时如果重新下载得到不同哈希，必须重新核验尺寸、帧布局和内容后再接入。
- 来源页展示的作者标识与包内元数据不是许可证。由于包内没有许可证，本计划不能把素材描述成“开源素材”；只能按用户确认的个人、本地、非商业测试边界使用。

### 4.7 `PetSpriteView` 渲染契约

使用一个专用 `PetSpriteView` 绘制静态 WebP 图集的指定源矩形，不依赖 GIF、APNG、Lottie 或第三方游戏框架。

已核验图集参数：

- 图集尺寸：`1536 × 1872`
- 网格：`8 列 × 9 行`
- 单元格：`192 × 208`
- 格式：带透明通道的静态 WebP

| 行号 | 状态 | 有效帧数 | 第一版用途 |
|---:|---|---:|---|
| 0 | Idle | 6 | 默认循环 |
| 1 | Run Right | 8 | 向右拖动时循环 |
| 2 | Run Left | 8 | 向左拖动时循环 |
| 3 | Waving | 4 | 从隐藏状态恢复时单次播放 |
| 4 | Jumping | 5 | 双击时单次播放 |
| 5 | Failed | 8 | 第一版保留但不接业务状态 |
| 6 | Waiting | 6 | 第一版保留但不接业务状态 |
| 7 | Running | 6 | 第一版保留但不接业务状态 |
| 8 | Review | 6 | 第一版保留但不接业务状态 |

`PetSpriteView` 只负责图集加载、帧推进、循环/单次播放和绘制；动画状态枚举放在该类内部，避免额外创建过多小类。实现接口固定为以下最小集合，命名可以按项目风格微调，但职责不能外溢：

```text
setAnimation(Animation animation, boolean restart)
showStaticFrame(Animation animation, int frameIndex)
setOnAnimationFinishedListener(...)
start()
stop()
```

渲染规则：

- 从 `drawable-nodpi` 使用 `BitmapFactory.decodeResource()` 解码，确保不因 density 自动缩放源图像。
- 源矩形固定为 `left = column × 192`、`top = row × 208`、`right = left + 192`、`bottom = top + 208`。
- 目标矩形使用 View 的实际宽高，并保持与单元格相同纵横比，禁止拉伸变形。
- 当前素材为像素/精灵风格，第一版关闭位图平滑过滤，优先保持边缘清晰；若真机缩放产生明显锯齿，再以验收截图为依据开启过滤。
- 通过单个 `Runnable + postOnAnimation()` 使用单调时间推进帧，不为每一帧创建新的 Animator 或 Bitmap。
- 一次只存在一个帧推进回调；切换动画先移除旧回调，防止多重循环导致加速。
- 单次动画结束必须只回调一次，由 Controller 决定回 Idle；View 不直接理解隐藏、拖动或业务状态。

由于素材不提供帧率，第一版明确采用以下间隔，不再由实施者临场猜测：

| 动画 | 帧间隔 | 播放方式 |
|---|---:|---|
| Idle | `160ms` | 循环 |
| Run Left / Run Right | `90ms` | 循环 |
| Waving | `130ms` | 单次 |
| Jumping | `120ms` | 单次 |
| 其他保留行 | `140ms` | 第一版不调用 |

### 4.8 `FloatingPetController` 坐标与持久化契约

新增一个范围明确的 `FloatingPetController`，避免把拖动、双击、贴边、弹层、位置持久化和窗口尺寸处理全部堆入当前已经较大的 `MainActivity`。

控制器职责：

- 使用 `GestureDetector` 的 `onSingleTapConfirmed()` 和 `onDoubleTap()` 区分单击与双击，不使用自定义双击计时器。
- 使用 `ViewConfiguration.getScaledTouchSlop()` 区分点击与拖动，不写死像素或 dp 阈值。
- 触摸命中宠物的 `ACTION_DOWN` 后立即请求 `DrawerLayout` 不要拦截，解决宠物贴左边缘时与 Drawer 边缘滑动竞争；在 `ACTION_UP`/`ACTION_CANCEL` 无条件恢复。
- 根据水平位移切换左右运行动画。
- 坐标只相对 `pet_overlay` 计算。可用范围为 `[edgeMargin, overlayWidth - petWidth - edgeMargin]` 和 `[edgeMargin, overlayHeight - petHeight - edgeMargin]`，无需再次扣除顶部栏、输入区或 IME。
- 拖动使用 `rawX/rawY` 与按下时 View 起点的差值更新位置，每次 MOVE 立即 clamp，任何时刻都不能先移动到屏幕外再等松手修正。
- 松手根据宠物中心点与 Overlay 中线比较，选择 `LEFT` 或 `RIGHT`；使用 `180ms` 减速动画贴边，动画开始前取消上一次 ViewPropertyAnimator。
- 持久化文件单独使用 `floating_pet_state`，字段固定为 `pet_hidden:boolean`、`pet_edge:string(left/right)`、`pet_vertical_fraction:float(0..1)`。
- 垂直比例计算为 `(y - minY) / max(1, maxY - minY)`；恢复时反算 y 并再次 clamp。异常值、NaN 或缺失值回退为右侧 `0.82`。
- 使用现有 AppCompat `PopupMenu` 管理宠物操作浮层，菜单资源第一版只有“隐藏”；系统自动处理外部点击关闭，不新增自定义 PopupWindow。
- 隐藏时缩小同一个 Doge View 形成边缘入口；恢复后回到保存位置。
- 隐藏后静止显示 Idle 第一帧，避免隐藏入口仍持续占用刷新；其点击目标不得小于 `48dp`。
- 给 Overlay 注册一个 `OnLayoutChangeListener`。尺寸变化、横竖屏变化或输入法导致 Overlay 改变时，停止贴边动画，按保存的 edge/fraction 恢复并夹取位置。

`MainActivity` 只负责初始化控制器、传入必要 View、在生命周期结束时释放动画回调，不承担具体帧播放与坐标算法。

### 4.9 宠物触摸处理顺序

每次手势必须按以下顺序处理，避免 GestureDetector 与拖动逻辑各自消费同一事件：

1. `ACTION_DOWN`：取消贴边动画，保存原始触点与 View 起点，清除旧拖动标记，立即禁止 Drawer 父级拦截，并把事件交给 GestureDetector。
2. `ACTION_MOVE`：若位移未超过 touch slop，继续交给 GestureDetector；一旦超过，标记 `dragging=true`、关闭 PopupMenu、取消单击候选并进入 Run 动画。此后 MOVE 只更新拖动坐标，不再触发点击语义。
3. `ACTION_UP`：如果正在拖动，执行贴边、保存 edge/fraction、回 Idle；否则让 GestureDetector 完成单击或双击判定。
4. `ACTION_CANCEL`：取消点击候选；如果曾进入拖动则夹取并贴边，否则回到当前合法位置；最后恢复 Idle 和父级拦截。
5. 任意结束路径都必须在 `finally` 等价逻辑中恢复 `requestDisallowInterceptTouchEvent(false)`，不能只写在正常 `ACTION_UP`。

### 4.10 宠物状态转换表

| 当前状态 | 事件 | 动作 | 下一状态 |
|---|---|---|---|
| Visible + Idle | 单击确认 | 打开只有“隐藏”的 PopupMenu | Visible + Idle |
| Visible + Idle/单次动画 | 双击 | 取消旧单次动画，从第 0 帧播放 Jumping | Visible + Jumping |
| Visible + 任意动画 | 超过拖动阈值 | 取消 PopupMenu、单次动画和贴边动画，按 dx 播放左右 Run | Visible + Dragging |
| Visible + Dragging | 松手/取消 | 夹取坐标、贴最近边、保存位置 | Visible + Idle |
| Visible + Idle | 选择隐藏 | 保存当前 edge/fraction，缩小为边缘入口并显示 Idle 第 0 帧 | Hidden |
| Hidden | 单击入口 | 恢复正常尺寸与保存位置，播放 Waving 一次 | Visible + Waving |
| Hidden | 双击或拖动 | 按一次恢复处理，不启动 Jump/Run | Visible + Waving |
| Visible + Jumping/Waving | 动画自然结束 | 只接受当前动画对应的完成回调，防止过期回调覆盖新状态 | Visible + Idle |
| 任意状态 | Overlay 尺寸变化 | 取消位置动画，按 edge/fraction 重新计算并夹取 | 原显示状态；动画必要时回 Idle |
| 任意状态 | Controller destroy | 关闭菜单、移除监听、取消 View 动画与 Handler/Runnable | Released |

状态优先级固定为：`Released > Hidden/Restore > Dragging > Jumping/Waving > Idle`。拖动可以中断 Jumping/Waving；过期的单次动画完成回调不得把拖动状态改回 Idle，可使用动画 generation/token 校验。

### 4.11 生命周期与资源释放

- `PetSpriteView.onAttachedToWindow()` 在可见且非隐藏时恢复帧回调；`onDetachedFromWindow()` 和 `onWindowVisibilityChanged(INVISIBLE/GONE)` 停止回调。
- 从后台返回时，如果仍处于可见状态，从保存的当前帧继续或安全回 Idle，不能并行启动第二个循环。
- `FloatingPetController.destroy()` 必须具备幂等性，并依次关闭 PopupMenu、取消 `ViewPropertyAnimator`、移除 Overlay 布局监听、清除触摸监听引用、停止 PetSpriteView。
- `MainActivity.onDestroy()` 在 `super.onDestroy()` 之前调用 controller destroy 和 AdapterDataObserver 注销，避免系统销毁后仍回调 Activity View。
- Bitmap 不在每次动画切换时解码，也不主动对 resource Bitmap 调用 `recycle()`；由 View/Activity 生命周期和 GC 管理，避免正在绘制时被回收。

### 4.12 性能、无障碍与可维护性约束

- 图集完整 RGBA 解码约 `10.97 MiB`，APK 增量约 `1.90 MiB`。第一版只允许一个 Bitmap 实例和一个可运行帧回调。
- 连续打开/关闭 MainActivity 5 次后，日志中不得出现重复动画速度、`Canvas: trying to use a recycled bitmap` 或 Window 泄漏。
- 菜单、用户、输入模式、发送/停止、宠物和隐藏入口均提供资源化 contentDescription；纯装饰 View 标记为不参与无障碍。
- 所有主要点击目标至少 `48dp`；用户 badge 的视觉圆形可以为 `40dp`，但外层点击区域必须为 `48dp`。
- 大字体测试至少覆盖系统字体 `1.3×`：顶部状态单行省略、输入框最多 4 行、Drawer 项不互相遮挡。
- 本轮不建立通用宠物插件框架。未来更换用户自制动画时，只替换图集、帧表和必要的尺寸/速度常量，不改 Controller 手势与持久化协议。

## 5. 文件变更规划

### 5.1 计划新增

- `app/src/main/java/com/hirain/aiagent/test/pet/PetSpriteView.java`
  - Doge 图集解码、状态定义、帧播放和 Canvas 绘制。
- `app/src/main/java/com/hirain/aiagent/test/pet/FloatingPetController.java`
  - 手势、贴边、隐藏/恢复、操作浮层、位置保存和尺寸修正。
- `app/src/main/res/drawable-nodpi/pet_doge_spritesheet.webp`
  - 从确认来源下载的原始 Doge 图集。
- `app/src/main/res/values/dimens.xml`
  - 手机默认间距、最大宽度、侧边栏和宠物尺寸。
- `app/src/main/res/values-w600dp/dimens.xml`
  - 宽屏/车机必要尺寸覆盖。
- `app/src/main/res/menu/menu_pet_actions.xml`
  - 宠物 `PopupMenu` 的“隐藏”操作；不在 Java 中硬编码菜单文字。
- `docs/third_party/doge-temporary-asset.md`
  - 记录来源页、页面作者标识、获取日期、ZIP/图集哈希、包内不含许可证的事实、使用边界和未来替换要求；该记录不等同于开源许可证。
- 必要的菜单图标、输入模式图标、输入区、会话选中态、用户 badge、按钮 enabled/disabled 状态和宠物菜单背景资源。

### 5.2 计划修改

- `app/src/main/res/layout/activity_main.xml`
  - 删除无效 Toolbar；重构顶部栏、聊天空状态、受约束的 `pet_overlay`、圆角输入区和侧边栏底部结构。
- `app/src/main/res/layout/item_chat_message.xml`
  - 支持 GPT 风格消息间距和更合理的内容容器。
- `app/src/main/res/layout/item_conversation.xml`
  - 增加圆角选中态和统一文字层级。
- `app/src/main/res/values/colors.xml`
  - 建立语义色并替换当前模板色。
- `app/src/main/res/values/themes.xml`
  - 统一状态栏、导航栏和 Material 主题基础颜色。
- `app/src/main/res/values/strings.xml`
  - 补齐标题、连接状态、欢迎空状态、用户入口、宠物隐藏和可访问性文本；不得改动与本任务无关的既有 ASR 配置字符串。
- `app/src/main/java/com/hirain/aiagent/test/activity/MainActivity.java`
  - 删除 Toolbar/Drawer 用户按钮字段；增加连接 UI 状态、顶部用户入口、AdapterDataObserver、消息宽度监听和宠物控制器生命周期接线。
- `app/src/main/java/com/hirain/aiagent/test/adapter/ChatAdapter.java`
  - 增加最大宽度 setter，调整消息样式、对齐方式和复用时的完整状态重置。
- `app/src/main/java/com/hirain/aiagent/test/adapter/ConversationAdapter.java`
  - 使用资源化选中态，移除硬编码紫色。
- 现有聊天、语音条和消息气泡 drawable 按新语义调整或替换。

### 5.3 明确不修改

- 不修改 `AgentRequest`、`AgentResponse`、AIDL Stub 和 `AIAgent` facade。
- 不修改会话创建、切换、删除和请求取消协议。
- 不修改 `ASRManager`、录音流程和 TTS 引擎逻辑。
- 不修改 `SettingsActivity` 的预留业务页面。
- 不修改 Gradle、AGP、依赖版本和 `AndroidManifest.xml`。
- 不增加系统悬浮窗权限、网络图片运行时加载或远程宠物服务。
- 不新增 Gradle 测试依赖；当前仓库没有现成 UI 测试基础设施，本轮使用构建、Lint、静态检查和真机/模拟器交互矩阵验收。

## 6. 执行包

本次只拆为三个执行包。每个执行包完成后先验证，再进入下一包；不按单个图标或单个 XML 切成零散 Phase。

### 6.0 实施前基线（不计为执行包）

实施开始后、修改任何应用文件之前必须完成：

1. 执行 `git status --short`，记录用户已有变更；本计划文档本身可能仍是未跟踪文件，不能因此误判工作区干净，也不能覆盖其他改动。
2. 执行 `:app:processDebugResources`、`:app:compileDebugJavaWithJavac`、`:app:assembleDebug` 和 `:app:lintDebug`，把既有失败与警告记录到计划末尾的“实施记录”。
3. 检查 `adb devices`。若存在可用设备/模拟器，记录其 `wm size`、`wm density`、Android 版本和当前页面截图；若不存在，明确记录“设备验收待完成”。
4. 再次读取下载包中的 `pet.json`、图集尺寸和 SHA-256；与 4.6 基线不一致时停止素材接入并重新核验。
5. 记录实现前 `activity_main.xml`、三种输入状态和 Drawer 的现状，作为视觉回归对照。

基线完成出口：能够明确区分“实施前已有问题”和“本轮新增问题”，并确认没有需要用户授权的依赖、Manifest 或业务协议变更。

### 执行包 1：视觉基础与响应式聊天界面

**目标：** 在不接入宠物的情况下，先让手机和车机比例下的聊天界面形成完整、稳定的 GPT 风格骨架。

**工作项：**

- [x] 按 4.1 的固定表建立语义颜色、尺寸、圆角、state-list 和 VectorDrawable；先让资源可独立编译，再改布局引用。
- [x] 删除无效 Toolbar，建立 `btn_menu`、受约束的标题状态容器和 `btn_user`；同步更新 MainActivity 菜单字段、findViewById、监听器和 import，两侧点击目标固定 `48dp`。
- [x] 将聊天列表和输入容器约束到同一中心轴，接入 `chat_content_max_width`、手机/宽屏 margin 和 Drawer 宽度资源。
- [x] 新增 `empty_chat_state`，删除 `onCreate()` 的欢迎消息注入，注册 AdapterDataObserver，并给 `scrollToBottom()` 加空列表防护。
- [x] 为 `rv_chat` 增加布局宽度监听，实现 4.3 的计算公式和 `ChatAdapter.setMessageMaxWidthPx()`；验证旋转后会重算。
- [x] 调整 AI/用户消息背景、padding、gravity 和 maxWidth；`onBindViewHolder()` 必须同时设置两种消息的全部可变属性，防止 RecyclerView 复用串色或错位。
- [x] 重做文字输入、语音条、发送和停止按钮共同外层；保留既有 View id 和 `applyInputMode()` 的 visible/enabled 语义，除非计划明确重命名。
- [x] 优化新建对话、会话列表和 AI 设置入口，ConversationAdapter 改用资源化选中态且在非选中分支完整复位背景。
- [x] 更新主题状态栏/导航栏与浅色系统图标配置；不启用 edge-to-edge。
- [x] 清理本轮涉及 XML/Adapter 的硬编码颜色和 contentDescription；不顺手修改无关 ASR/TTS 字符串或业务代码。

**阶段验证：**

```powershell
.\gradlew.bat :app:processDebugResources
.\gradlew.bat :app:compileDebugJavaWithJavac
.\gradlew.bat :app:assembleDebug
```

人工检查：手机竖屏、手机横屏和至少一种车机比例下，无重叠、裁切、无法点击或输入框过度拉伸。

**执行包 1 完成出口：** 即使用户入口尚未接业务、宠物尚未接入，页面也必须可以独立编译并呈现完整视觉骨架；空状态、真实消息和多行输入三种布局均可切换。若 Header 在 `1.3×` 字体下覆盖按钮，必须在本包解决后才能继续。

### 执行包 2：用户入口迁移与现有状态回归

**目标：** 把右上角用户入口真正接入现有状态流，并确保本次视觉调整没有破坏会话、文字和语音行为。

**工作项：**

- [x] 把 Drawer 用户 lambda 原样提取为 `showUserSwitchDialog()`，先保证行为不变，再把调用入口接到 `btn_user`。
- [x] 删除 Drawer 中 `btn_drawer_user` 和分隔 View，让 AI 设置成为单一底部入口；同步删除 Java 字段、findViewById 和 processing 分支引用。
- [x] 引入 4.5 的连接 UI 状态，改造连接/断开回调统一调用 `refreshHeaderState()`，禁止直接与刷新方法竞争写状态 TextView。
- [x] 实现 `formatUserBadge()` 和资源化 contentDescription；切换后 badge、选中会话和状态行必须一次刷新完成。
- [x] 更新 `setRequestProcessing()`：请求中禁用新建、会话项、AI 设置和顶部用户入口；菜单按钮仍允许打开 Drawer 查看状态，发送按钮继续切换为停止。
- [x] 逐条核对用户切换顺序：保存 userId → 清 session → 清消息 → 加切换提示 → 加载新用户会话 → 刷新 Header；不调整 AIAgent 调用协议。
- [x] 核对服务断开、重新连接、无当前会话、有当前会话四种 Header 输出，确认断开状态不会被会话加载覆盖。
- [ ] 回归文字模式、语音模式、发送/停止、新建/切换/删除会话、用户切换、Persona 与 TTS 设置。

**阶段验证：**

```powershell
.\gradlew.bat :app:compileDebugJavaWithJavac
.\gradlew.bat :app:assembleDebug
```

人工检查：请求处理中用户入口不可用；请求结束恢复；侧边栏会话操作、ASR 按住说话和停止请求均保持原有语义。

**执行包 2 完成出口：** 所有入口迁移只改变位置和表现，不改变会话、用户、请求、ASR、TTS 的数据与调用顺序；连接状态与用户/会话摘要不再互相覆盖。

### 执行包 3：Doge 悬浮宠物

**目标：** 使用临时 Doge 图集完成可替换的应用内宠物闭环。

**工作项：**

- [x] 从确认包仅复制原始 `spritesheet.webp` 到 `drawable-nodpi`，在 `docs/third_party` 记录来源、页面作者标识、两个 SHA-256、包内无许可证和未来替换门槛。
- [x] 先单独实现 `PetSpriteView`：加载一次 Bitmap、按行列计算源矩形、执行 4.7 帧间隔、保证循环/单次完成回调互斥。
- [x] 在静态界面中逐一人工播放 Idle、Run Left/Right、Waving、Jumping，确认帧数、方向、无透明空帧后再接手势。
- [x] 在 `activity_main.xml` 增加不可点击 `pet_overlay` 和 Doge View，先验证 Overlay 空白区域不阻断 RecyclerView 滚动与消息点击。
- [x] 实现 Controller 的默认状态、坐标 clamp、edge/fraction 持久化和 Overlay 尺寸监听；先验证重启、旋转、IME 三类恢复，再接复杂手势。
- [x] 按 4.9 的固定顺序接入触摸，覆盖 touch slop、父拦截、ACTION_CANCEL 和贴左边缘 Drawer 冲突。
- [x] 按 4.10 状态表接入 Jumping/Waving/Run，使用 generation/token 忽略过期动画完成回调。
- [x] 使用 `PopupMenu + menu_pet_actions.xml` 实现单击“隐藏”；点击外部关闭，选择隐藏后缩小为静态边缘入口。
- [x] 隐藏入口单击只执行恢复和 Waving；隐藏状态不接受拖动/Jump，避免同一手势产生双重状态。
- [x] 实现 attach/window visibility 暂停与 Controller 幂等 destroy；取消全部 Runnable、Animator、监听器和菜单引用。
- [x] 最后才把宠物初始化和销毁接入 MainActivity，Activity 不出现帧表、手势阈值或坐标公式。

**阶段验证：**

```powershell
.\gradlew.bat :app:processDebugResources
.\gradlew.bat :app:compileDebugJavaWithJavac
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:lintDebug
```

人工检查：连续执行单击、双击、拖动、快速拖动后双击、隐藏、恢复、旋转和键盘弹出，宠物不得卡死、消失到屏幕外或阻断聊天操作。

**执行包 3 完成出口：** 宠物具备完整可恢复闭环，状态转换和释放路径均有证据；若只能编译但没有设备/模拟器完成手势矩阵，只能标记“实现完成、设备验收待完成”，不能宣称整个 Goal 完成。

### 6.4 每个执行包的强制收尾动作

每一包结束后都必须：

1. 运行该包列出的 Gradle 命令，记录退出码和新增警告。
2. 执行 `git diff --check` 与 `git status --short`，确认没有越界文件。
3. `git diff --check` 不覆盖未跟踪文件，因此必须额外检查所有新增 Java/XML/Markdown 文本文件的尾随空白和结尾换行；二进制 WebP 只核对哈希与资源编译。
4. 阅读实际 diff，重点检查硬编码色、重复监听、未清理回调、旧 View id 残留和空指针路径。
5. 更新本计划对应复选框和“实施记录”，写真实结果，不预填“通过”。
6. 只有本包完成出口满足后才能进入下一包；发现必须改 Gradle、Manifest、AIDL 或 ASR/TTS 业务时停止并询问用户。

## 7. 验收矩阵

验收项必须记录“通过 / 失败 / 未执行”以及证据。截图命名建议为 `phone-portrait-*`、`phone-landscape-*`、`car-1280x720-*`；如果不能生成截图，至少记录设备参数、操作步骤和观察结果。禁止把“未执行”写成“通过”。

### 7.1 视觉与适配

- [x] 360dp 左右手机竖屏：顶部栏、消息、输入区和宠物完整显示。
- [ ] 手机横屏：输入区不覆盖聊天内容，宠物仍可恢复到可见区域。
- [x] 1280×720 或相近车机比例：聊天内容限宽居中，侧边栏和右上角用户入口可操作。
- [ ] 长消息：用户与 AI 消息不超出容器，换行自然。
- [x] 空消息列表：欢迎状态显示；首条消息加入后欢迎状态消失。
- [ ] 输入多行文本：输入容器可增高但不挤掉顶部栏。
- [ ] 系统字体 `1.3×`：标题状态不覆盖两侧按钮，Drawer 与输入区文字不裁切。
- [x] IME 显示/隐藏：聊天列表与 pet_overlay 高度正确重算，最后一条消息仍可滚动到可见区域。
- [x] 浅色系统栏：状态栏/导航栏图标有足够对比度，API 24 基础运行不因新主题属性异常。

### 7.2 现有功能回归

- [x] 服务连接、断开状态可辨识。
- [ ] 新建、切换、删除会话仍可用。
- [x] 用户切换入口位于右上角，切换后的 userId/sessionId 行为不变。
- [ ] 请求处理中不能切换用户、会话或 AI 设置。
- [ ] 文字发送、停止请求和响应气泡正常。
- [ ] 语音/键盘切换、按住说话、上滑取消、松开发送正常。
- [ ] TTS 自动/开/关行为不受影响。
- [ ] 服务断开后 Header 保持“未连接”，随后会话刷新不得覆盖；重连后恢复“已连接”摘要。
- [x] 初次进入没有真实消息时只显示欢迎空状态，不额外生成 AI 消息或触发 TTS。

### 7.3 宠物交互

- [x] Idle 循环稳定，无空白帧闪烁。
- [x] 单击只显示一次操作浮层。
- [x] 双击不弹出单击浮层，Jumping 只播放一次并回 Idle。
- [x] 拖动不误触单击/双击，方向动画正确。
- [x] 松手自动贴边，宠物不会进入顶部栏、输入区或屏幕外。
- [x] 隐藏后保留边缘入口；点击入口能够恢复。
- [x] 重启 Activity 后恢复显示状态与合理位置。
- [x] Drawer、RecyclerView 和语音条手势不被宠物的非命中区域阻断。
- [x] 宠物贴左边缘时仍可拖动/双击；从宠物以外的左侧边缘滑动仍可打开 Drawer。
- [ ] 拖动过程中 Drawer 打开或 Activity 失焦产生 ACTION_CANCEL 后，宠物没有卡在 Run 状态。
- [ ] Jumping/Waving 未结束时开始拖动，过期完成回调不会把 Run 状态提前改回 Idle。
- [x] 隐藏入口不持续播放动画；恢复后只播放一次 Waving。
- [ ] 连续旋转或切换前后台 5 次，动画速度不叠加且无 Window/Handler 泄漏日志。

### 7.4 构建与静态检查

- [x] `:app:processDebugResources` 通过。
- [x] `:app:compileDebugJavaWithJavac` 通过。
- [x] `:app:assembleDebug` 通过。
- [x] `:app:lintDebug` 通过；如存在既有问题，必须区分既有与本轮新增问题。
- [x] `git diff --check` 无空白错误。
- [x] `git status --short` 仅包含本计划范围内文件。

### 7.5 最低设备组合

| 组合 | 目标参数 | 必验内容 |
|---|---|---|
| 手机竖屏 | 约 `360×800dp`，字体 1.0× | 完整主流程、长消息、多行输入、用户入口、所有宠物手势 |
| 手机横屏 | 同一设备旋转 | Header/输入区不挤压、宠物位置恢复、Drawer 可用 |
| 车机横屏 | `1280×720px` 或相近比例，并记录 density/dp | 内容限宽、主要点击目标、Drawer 宽度、宠物不遮挡输入 |
| 大字体补充 | 任一手机，字体 1.3× | Header、Drawer、输入区和对话项无文字覆盖 |

如果只能使用一个设备，可通过模拟器配置补足另外两种尺寸；如果完全没有设备或模拟器，只能完成构建与静态检查，设备矩阵保留为未执行。

### 7.6 验收证据模板

```text
日期：
执行包/版本：
设备与 Android 版本：
分辨率 / density / 实际 dp：
操作步骤：
预期结果：
实际结果：
结论：通过 / 失败 / 未执行
截图或日志位置：
备注：
```

## 8. 风险与控制措施

### 风险 1：临时素材没有明确再分发许可证

控制措施：仅用于个人、本地、非商业测试；记录来源和作者；不声称素材为项目自有或开源。公开源码、分发 APK 或正式发布前必须替换或获得许可。

### 风险 2：WebP 图集解码后的内存高于文件体积

`1536×1872` RGBA 图集完整解码约占 11 MiB。第一版允许完整解码以换取简单稳定的帧绘制，但需要观察 Activity 重建和后台返回后的内存。如果出现明显压力，再把实际使用行预处理成更小图集；不在没有证据时提前引入复杂缓存层。

### 风险 3：单击、双击和拖动发生冲突

控制措施：使用系统双击判定；单击动作延迟到双击窗口结束；位移超过阈值后立即进入拖动态并取消待执行单击；每次手势结束统一恢复状态。

### 风险 4：宠物位置在横竖屏或键盘弹出后失效

控制措施：保存归一化位置；每次容器尺寸或 Insets 变化后重新计算可用矩形并夹取坐标，不直接恢复旧绝对像素。

### 风险 5：MainActivity 继续膨胀

控制措施：图集渲染和悬浮交互放入两个职责明确的 pet 类；MainActivity 仅接线，不建立通用宠物插件框架，也不为未来假设增加配置层。

### 风险 6：视觉调整破坏语音或请求状态

控制措施：不改请求、ASR、TTS 和会话语义；每个执行包结束都回归文字、语音和请求中禁用状态，不能只以 APK 编译成功作为完成证据。

### 风险 7：Overlay 空白区域吞掉 RecyclerView 触摸

控制措施：`pet_overlay` 自身保持 `clickable=false/focusable=false`，只给 PetSpriteView 设置触摸监听；在接入手势之前先单独验证空白区域滚动。若系统分发行为与预期不符，改为更小的可移动 Pet 容器，而不是添加全屏透明点击层。

### 风险 8：左边缘宠物与 Drawer 手势竞争

控制措施：只有触摸实际命中宠物时才在 ACTION_DOWN 禁止父级拦截，结束后无条件恢复；宠物之外的 Drawer 边缘区域仍可滑动打开。

### 风险 9：过期动画回调污染新状态

控制措施：每次 `setAnimation()` 递增 generation/token，单次完成回调先核对 token 和当前状态；拖动、隐藏、destroy 都使旧 token 失效。

### 风险 10：资源重命名造成 MainActivity 残留引用

控制措施：`btn_settings → btn_menu`、删除 Toolbar 和 Drawer 用户按钮时同步检查 XML id、字段、findViewById、监听器、processing 状态及 import；每个小步骤先执行资源编译和 Java 编译。

### 风险 11：缺少自动化 UI 测试导致手势回归

控制措施：不为本次一次性 TestApp UI 引入新的测试框架和依赖；用固定状态表、设备矩阵、连续组合手势和证据模板补足。未来宠物成为长期模块后，再单独评估把坐标/状态机提取为纯 Java 并增加单元测试。

### 风险 12：复审后的计划过度扩张实现范围

控制措施：新增细节只用于消除实现歧义，不新增深色模式、业务状态联动、宠物配置页、音效、网络加载或通用插件系统；任何额外体验想法进入后续清单，不混入本 Goal。

## 9. Goal 执行与停止规则

- 实施时按执行包顺序推进，每完成一个执行包更新本计划复选框和真实验证结果。
- 如果发现必须修改 AIDL、ASR/TTS 业务逻辑、依赖版本、Manifest 权限或系统悬浮窗能力，应立即停止并请求用户确认，因为这超出本计划边界。
- 如果 Doge 素材地址失效、哈希或文件参数变化、出现授权冲突，应停止素材接入，保留 UI 计划并请求改用其他素材；不能为了赶进度静默换图。
- 如果单一布局在真实车机比例验证后无法保持可操作，先记录具体裁切证据，再请求是否增加 `layout-land`；不能在没有证据时提前复制整套布局。
- 如果 Overlay 空白区域确实阻断聊天触摸，优先缩小交互容器或调整层级，禁止通过全屏透明点击层绕过。
- 如果实施过程中发现现有业务 bug，只记录到总结；除非它直接阻止本计划验证，否则不顺手修复。
- 构建通过但人工手势或屏幕适配未验证时，不得将 Goal 标记为完成。
- 本计划获得用户批准后才能进入实现；批准计划不等同于批准额外扩展功能。

## 10. 计划自检

- 已覆盖 GPT 风格主界面、手机优先、车机基本适配、右上角用户入口和侧边栏整理。
- 已覆盖应用内悬浮宠物、单击操作、双击动画、拖动贴边、隐藏恢复和位置保存。
- 已核验 Doge 图集尺寸、网格、帧数、文件格式和当前测试包哈希；帧率因素材缺少元数据而采用本计划明确值，不再留给实现阶段猜测。
- 已记录临时素材的个人测试边界、包内无许可证事实和未来替换条件。
- 已明确不修改 AIDL、会话、ASR、TTS、Gradle、Manifest 和 SettingsActivity。
- 已为每个执行包设置输入、具体落点、完成出口和阶段验证，并提供最终人工验收矩阵与证据模板。
- 已补齐欢迎空状态、Header 连接状态、消息宽度更新、Overlay 边界、手势优先级、状态机、生命周期和失败停止条件。
- 当前没有未决产品选项，可以在用户批准后直接执行。

## 11. 实施记录（执行时填写）

### 11.1 基线

- Git 状态：实施前仅本计划文档为未跟踪文件；实施后差异仅包含 UI、宠物、资源及对应文档。
- 资源编译：通过，`:app:processDebugResources` 成功。
- Java 编译：通过，`:app:compileDebugJavaWithJavac` 成功。
- APK 构建：通过，`:app:assembleDebug` 成功，正式产物为 `app/build/outputs/apk/debug/app-debug.apk`。
- Lint：通过，`0 errors, 22 warnings`；本轮涉及文件中的告警为原有 `notifyDataSetChanged` 和语音触摸可访问性告警，本轮未扩大处理范围。
- 可用设备/模拟器：Android 15 Automotive 横屏模拟器；同时通过调整模拟显示尺寸完成约 360dp 手机竖屏视觉验证。
- 素材哈希复核：通过；Doge 图集 SHA-256 为 `E2C7216E564F375C2F60AAD32475574FDB50EFFF7896D1066548746C767D8AF2`。

### 11.2 执行包结果

| 执行包 | 代码状态 | 构建结果 | 人工验收 | 遗留问题 |
|---|---|---|---|---|
| 1. 视觉基础与响应式聊天界面 | 已完成 | 通过 | 车机横屏、约 360dp 手机竖屏、欢迎空状态和 IME 场景通过 | 手机横屏、1.3× 字体和长消息专项仍需补验 |
| 2. 用户入口迁移与现有状态回归 | 已完成 | 通过 | 右上角用户弹窗、Drawer 去重、断开状态和空会话通过 | AIAgentService 既有异常阻断文字/语音/TTS 端到端回归 |
| 3. Doge 悬浮宠物 | 已完成 | 通过 | 单击、双击、拖动贴边、隐藏恢复、重启持久化和 IME 重定位通过 | ACTION_CANCEL、连续旋转/前后台 5 次仍需真实设备专项补验 |

### 11.3 最终结论

本计划的代码实现与当前可用环境范围内验收已经完成：GPT 风格响应式主界面、右上角用户入口、独立欢迎空状态和应用内 Doge 悬浮宠物均已落地，四项 Gradle 验证与差异检查通过。

正式 APK 因现有 ASR SDK 只包含 ARM 原生库，无法直接安装到 x86_64 Automotive 模拟器。为验证纯 UI，验收时仅对正式 APK 的临时副本移除原生库并重新使用调试证书签名；该副本不属于交付物，未修改 Gradle、Manifest 或正式 APK。模拟器上的 `com.hirain.aiagent` 服务进程还出现了既有监听器注销异常，因此不能把文字请求、ASR、TTS 和完整会话业务链写成已通过。

后续在 ARM 手机或真实车机上补验手机横屏、1.3× 字体、长消息、多行输入、连续生命周期切换和完整 AIDL/ASR/TTS 业务链后，可关闭上述环境性验收缺口。本轮没有越界修改这些业务模块。
