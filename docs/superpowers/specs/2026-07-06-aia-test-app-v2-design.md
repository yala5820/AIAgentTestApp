# AIA 테스트 App V2 设计方案

## 概述

在现有聊天测试功能基础上，引入 DrawerLayout 侧边栏、Room 持久化多轮对话管理、用户切换、AI 性格预留接口。

---

## 1. UI 布局改造

### 1.1 整体结构

```
DrawerLayout (match_parent)
├── 主内容区 (ConstraintLayout)
│   ├── Toolbar (标题居中 + 左侧汉堡菜单按钮 + 右侧新建对话按钮)
│   ├── RecyclerView (聊天消息)
│   └── LinearLayout (输入框 + 发送按钮)
│
└── 侧边栏 (ConstraintLayout, android:layout_gravity="start")
    ├── 顶部: 用户切换区域 (TextView 用户名 + 切换按钮)
    ├── 中部: RecyclerView (对话历史列表)
    │   └── 每个 item: 对话标题 + 删除按钮
    └── 底部: 按钮栏
        ├── 左下: 用户信息按钮
        └── 右下: AI 设置按钮
```

### 1.2 标题居中修复

Toolbar 默认 title 左对齐，设置按钮定位于左侧时会与 title 重叠。

**方案：** 不使用 `app:title`，改为在 Toolbar 内部放一个居中的 `TextView`，通过 `layout_gravity="center"` 实现真正居中。左侧放汉堡菜单 icon，右侧放新建对话 icon。

### 1.3 侧边栏实现

- 外层 `DrawerLayout` 白嫖系统手势和遮罩
- 侧边内容用 `ConstraintLayout`，`layout_width="280dp"`，`layout_gravity="start"`
- 打开/关闭通过 Toolbar 汉堡按钮触发 `drawerLayout.openDrawer(Gravity.START)`

---

## 2. 多轮对话管理

### 2.1 数据模型 (Room)

```java
@Entity(tableName = "conversations")
public class Conversation {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String title;          // 对话标题（首条用户消息截取）
    public String userId;         // 所属用户
    public long createdAt;        // 创建时间戳
    public long lastMessageAt;    // 最后一条消息时间
}
```

- 按 `lastMessageAt DESC` 排序显示
- `title` 取首条用户消息前 20 字

### 2.2 操作

| 操作 | 触发方式 | 行为 |
|------|----------|------|
| **新建** | Toolbar 右侧 + 按钮 | 生成新 session，清空聊天列表，聚焦输入框 |
| **切换** | 点击侧边栏历史条目 | 加载该对话的消息到 RecyclerView，后续消息追加到该对话 |
| **删除** | 侧边栏条目 × 按钮 | Room delete + 从列表移除 |
| **自动创建** | 首次发送消息 | 若无当前对话，自动创建一条 Conversation 记录 |

### 2.3 消息持久化

每条消息也存 SQLite，不属于 Room Entity，而是 ChatMessage 扩展：

```java
@Entity(tableName = "messages")
public class MessageEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long conversationId;   // 外键 → conversations.id
    public int type;              // SENT=0 / RECEIVED=1
    public String content;
    public long timestamp;
}
```

- 发送消息时：立即写入 `messages` 表
- 收到 `AgentResponse` 时：立即写入 `messages` 表
- 加载对话时：`SELECT * FROM messages WHERE conversationId = ? ORDER BY timestamp ASC`

---

## 3. 用户切换

利用 AIAgent 现有的 `AgentRequest.sessionId` 字段：

- 默认用户 `"default_user"`
- 点击侧边栏用户区域 → 弹出 Dialog 输入新用户 ID
- 切换后：`currentUserId` 更新，后续请求的 `sessionId` 变为新用户 ID
- 侧边栏显示当前用户名

AIAgent 中枢已支持多用户记忆隔离，无需修改 AIDL。

---

## 4. AI 性格切换（预留接口）

本次不实现实质功能，仅预留 UI 入口：

- 侧边栏 "AI 设置" 按钮 → 弹出底部 Sheet 或跳转到 SettingsActivity
- SettingsActivity 中放置一排留空选项（暖男/毒舌/专业等占位文本 + 点击 Toast）
- 不修改 `AgentRequest`，不修改 AIDL 接口

---

## 5. 新增文件清单

| 文件 | 职责 |
|------|------|
| `test/model/Conversation.java` | Room Entity — 对话记录 |
| `test/model/MessageEntity.java` | Room Entity — 消息记录 |
| `test/db/AppDatabase.java` | Room Database 类 |
| `test/db/ConversationDao.java` | 对话 DAO |
| `test/db/MessageDao.java` | 消息 DAO |
| `test/adapter/ConversationAdapter.java` | 侧边栏对话列表适配器 |
| `test/dialog/UserSwitchDialog.java` | 用户切换弹窗 |

## 6. 修改文件清单

| 文件 | 改动 |
|------|------|
| `activity/MainActivity.java` | 新增 Drawer 逻辑、对话管理、用户切换、构造 DrawerLayout 相关内容 |
| `activity/SettingsActivity.java` | 改为从侧边栏入口进入，添加占位 UI |
| `adapter/ChatAdapter.java` | 无改动（纯 UI 适配器） |
| `model/ChatMessage.java` | 添加 `id` 和 `conversationId` 字段 |
| `MyApplication.java` | Room 数据库初始化 |
| `build.gradle.kts` | 新增 Room 依赖 |
| `res/layout/activity_main.xml` | 外层加 DrawerLayout，修复标题居中 |
| `res/layout/activity_settings.xml` | 重新设计留空占位 UI |
| `res/layout/item_conversation.xml` | 新增 — 侧边栏对话条目 |
| `res/layout/item_drawer_header.xml` | 新增 — 侧边栏顶部用户区域 |
| `res/layout/drawer_bottom_buttons.xml` | 新增 — 侧边栏底部按钮栏 |
| `AndroidManifest.xml` | 无改动 |

## 7. 构建依赖变更

`app/build.gradle.kts` 新增：
```kotlin
// Room
val roomVersion = "2.6.1"
implementation("androidx.room:room-runtime:$roomVersion")
annotationProcessor("androidx.room:room-compiler:$roomVersion")
kapt {  // 如果不用 kapt，则用 annotationProcessor
}
```

---

## 8. 验证

1. 编译: `./gradlew :app:compileDebugJavaWithJavac`
2. 运行验证:
   - 发送消息 → 自动创建对话 → 出现在侧边栏
   - 切换对话 → 聊天列表清空/加载对应消息
   - 删除对话 → 侧边栏移除 + 界面清空
   - 切换用户 → 后续消息使用新 sessionId
   - 侧边栏底部两个按钮 → 分别触发对应入口
