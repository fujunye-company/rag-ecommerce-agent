# 拾物 — 本地长效记忆机制

> 对标微信 WCDB / 豆包 Room 的用户数据持久化方案

---

## 架构概览

```
┌──────────────────────────────────────────┐
│              ViewModel 层                 │
│  ChatViewModel / CartViewModel            │
│         ↓ suspend fun                    │
│       UserRepository                     │
│         ↓ SQLiteOpenHelper               │
│       LocalDatabase                      │
│         ↓ sqlite3                        │
│  hermes_local.db  (App 私有目录)          │
└──────────────────────────────────────────┘
```

## 设计选择

| 方案 | 微信 WCDB | 豆包 Room | 拾物(本方案) |
|------|----------|----------|------------|
| 数据库 | SQLite | SQLite | SQLite |
| 封装 | 自研 WCDB | Room(KSP) | SQLiteOpenHelper |
| 加密 | ✓ | ✗ | ✗(MVP) |
| ORM | 自研 | ✓ | 手工 SQL |
| 额外依赖 | 无 | KSP插件 | 零 |

选择原生 SQLiteOpenHelper 的原因：
- 零额外 Gradle 插件依赖（Room 需要 KSP/KAPT）
- 性能等同于 Room（底层都是 SQLite）
- 对标微信 WCDB 的轻量思路

## 数据库结构

### user_profile — 用户画像
```
id, nickname, gender, age_range, budget_min, budget_max,
preferred_categories(JSON), created_at, updated_at
```

### conversations — 对话会话
```
id(UUID), title, message_count, last_message, created_at, updated_at
```

### messages — 聊天消息
```
id, conversation_id, role(user/assistant), content,
product_cards(JSON), status, created_at
```
索引: `(conversation_id, created_at)`

### cart_items — 购物车缓存
```
product_id, session_id, title, price, brand, category,
image_url, rating, quantity, added_at
```

### settings — 键值设置
```
key PRIMARY KEY, value
```
已用键: `chat_session_id`, `cart_session_id`

### search_history — 搜索历史
```
id AUTOINCREMENT, query, created_at
```
自动裁剪: 保留最近 50 条

---

## 记忆恢复流程

### App 首次安装
1. `settings` 表为空 → 生成新 `chat_session_id` UUID
2. 写入 `settings` 表
3. 创建空 `conversations` 记录
4. 购物车为空

### App 重启
1. 读取 `settings` 中的 `chat_session_id`
2. 恢复同 sessionId → 后端 state_manager 复用同一会话
3. 从 `messages` 表加载最近 50 条历史消息
4. 从 `cart_items` 表加载购物车

### App 卸载重装
1. `hermes_local.db` 被删除
2. 回到「首次安装」流程
3. 数据完全重置

---

## 对标分析

| 能力 | 微信 | 豆包 | 拾物 |
|------|:--:|:--:|:--:|
| 消息持久化 | ✓ | ✓ | ✓ |
| 会话管理 | ✓ | ✓ | ✓ |
| 用户画像 | ✓ | ✓ | ✓ |
| 购物车缓存 | ✗ | ✗ | ✓ |
| 搜索历史 | ✓ | ✓ | ✓ |
| 云同步 | ✓ | ✓ | 后端 state_manager |
| 端到端加密 | ✓ | ✗ | ✗(MVP) |

拾物方案的特点：
- 本地 SQLite 保证离线可用
- 后端 state_manager 提供云端会话状态
- 双层记忆: 本地(消息/设置) + 云端(对话状态/商品数据)
