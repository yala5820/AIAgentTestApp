# Phase 2 验收审查报告

**审查对象：** Phase 2：主界面与侧边栏布局

**审查方式：** 逐文件对照计划 + 功能交叉验证

---

## 审查结论：未通过 — 5 个阻塞问题

| Task | 状态 | 说明 |
|------|------|------|
| Task 2.1 DrawerLayout 依赖 | ✅ 通过 | `libs.versions.toml` 和 `build.gradle.kts` 均已添加 |
| Task 2.2 布局重构 | ❌ **未完成** | 布局文件正确但 MainActivity 代码完全未对接 |
| Task 2.3 ConversationAdapter | ✅ 通过 | 代码正确，逻辑完整 |

---

## P0：`btn_settings` 点击行为与计划要求不符

**位置：** `MainActivity.java:299-301`

计划要求左侧按钮用来**打开侧边栏**。但当前仍然是旧的 `showTtsSettingsDialog()`。

```java
// 当前代码 — 错误行为
btnSettings.setOnClickListener(v -> showTtsSettingsDialog());

// 应为 — 打开 DrawerLayout
DrawerLayout drawer = findViewById(R.id.drawer_layout);
btnSettings.setOnClickListener(v -> drawer.openDrawer(Gravity.START));
```

---

## P0：`MainActivity` 未初始化侧边栏任何交互控件

布局文件新增了 5 个侧边栏控件 ID，但 `MainActivity` 未绑定其中任何一个：

| 控件 ID | 计划要求 | 实际状态 |
|---------|----------|----------|
| `btn_new_conversation` | 新建对话 | ❌ 未引用 |
| `rv_conversations` | 显示会话列表 | ❌ 未引用 |
| `btn_drawer_user` | 用户切换 | ❌ 未引用 |
| `btn_drawer_ai_settings` | AI 设置 | ❌ 未引用 |
| `drawer_layout` | DrawerLayout 实例 | ❌ 未引用 |

这些属于 Phase 3 的业务逻辑，但至少 `drawer_layout` 的 `openDrawer()` 属于布局基础交互，应该在 Phase 2 完成。

---

## P1：连接状态使用不可见的 Toolbar subtitle

**位置：** `MainActivity.java:104, 367, 375`

布局已将原 `Toolbar` 设为 `android:visibility="gone"`，新布局改用独立的 `tv_connection_status`。但代码仍调用 `toolbar.setSubtitle()`。

```java
// 当前代码 — 日志正确但 UI 不可见
toolbar.setSubtitle("连接中…");   // Toolbar 不可见，无效果

// 应为
TextView status = findViewById(R.id.tv_connection_status);
status.setText("连接中…");
```

三处代码都需要更新（`onCreate`、`onConnected`、`onDisconnected`）。

---

## P1：布局 `fitsSystemWindows` 未添加

**位置：** `activity_main.xml:8`

之前的审查建议了 `DrawerLayout` 需要 `android:fitsSystemWindows="true"` 来正确处理状态栏区域。当前布局已没有该属性。虽然不是阻塞崩溃问题，但可能导致顶部状态栏下方出现空白或重叠。

---

## ✅ 已确认正确的部分

| 检查项 | 状态 |
|--------|------|
| Task 2.1 DrawerLayout 依赖 | ✅ `libs.versions.toml` + `build.gradle.kts` 正确 |
| Task 2.2 布局结构：DrawerLayout → 主内容 + 侧边栏 | ✅ 结构正确 |
| 标题居中 `tv_title` | ✅ `app:layout_constraintStart/End_toStart/EndOf="parent"` 水平居中 |
| 侧边栏宽度 300dp，`layout_gravity="start"` | ✅ |
| 侧边栏底部按钮栏（用户 + AI 设置） | ✅ 两个 button layout 正确 |
| 侧边栏空状态 `tv_drawer_empty` | ✅ present，默认 gone |
| Task 2.3 `ConversationAdapter` | ✅ `submitList()` 带 activeSessionId 高亮；点击/长按回调；subtitle 拼合 personaId + messageCount |
| `item_conversation.xml` | 布局简洁，无冗余 |
| 编译 | ✅ `BUILD SUCCESSFUL` |

---

## 修复建议优先级

1. **P0: `btn_settings` 绑定 `drawerLayout.openDrawer()`** — 改 2 行代码
2. **P0: `drawer_layout` 绑定到字段** — 1 行 `findViewById`
3. **P1: `tv_connection_status` 替代 `toolbar.setSubtitle()`** — 改 3 处共 3 行
4. **P1: 添加 `fitsSystemWindows`** — 布局 1 行
