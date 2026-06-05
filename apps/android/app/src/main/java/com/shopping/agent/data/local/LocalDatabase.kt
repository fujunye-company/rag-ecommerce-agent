package com.shopping.agent.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.UUID

/**
 * 本地长效数据库 — 对标微信 WCDB / 豆包 Room 的记忆机制。
 *
 * 表结构:
 *   user_profile     用户画像 + 偏好（UUID 主键，以 "sw" 开头；avatar BLOB 存储头像）
 *   conversations    对话会话
 *   messages         聊天消息
 *   cart_items       购物车
 *   settings         键值设置
 *   search_history   搜索历史
 */
class LocalDatabase(context: Context) : SQLiteOpenHelper(
    context, DATABASE_NAME, null, DATABASE_VERSION
) {
    companion object {
        /** 数据库文件名 */
        const val DATABASE_NAME = "hermes_local.db"
        /** 当前数据库版本号，变更 schema 时递增 */
        const val DATABASE_VERSION = 9

        /** 用户画像表 — 以 "sw" UUID 为主键，avatar 为 BLOB 列 */
        const val TABLE_USER = "user_profile"
        const val TABLE_CONVERSATIONS = "conversations"
        const val TABLE_MESSAGES = "messages"
        const val TABLE_CART = "cart_items"
        const val TABLE_SETTINGS = "settings"
        const val TABLE_SEARCH = "search_history"
        /** 客服对话记录表 — 存储用户与拾物客服的消息记录 */
        const val TABLE_CUSTOMER_SERVICE = "customer_service_messages"
        /** 登录凭证表 — 存储用户账号密码（支持游客登录） */
        const val TABLE_CREDENTIALS = "user_credentials"
        /** 收货地址表 — 存储用户收货地址信息 */
        const val TABLE_SHIPPING_ADDRESSES = "shipping_addresses"
        /** 支付设置表 — 存储用户支付偏好 */
        const val TABLE_PAYMENT_SETTINGS = "payment_settings"
        /** 国家与地区表 — 存储用户所在地区 */
        const val TABLE_COUNTRY_REGION = "country_region"
        /** 订单记录表 — 存储用户订单信息 */
        const val TABLE_ORDER_RECORDS = "order_records"
        /** 登录状态表 — 记录用户登录状态，用于启动时判断是否跳过登录页 */
        const val TABLE_LOGIN_STATE = "login_state"
        /** 商品收藏记录表 — 记录用户收藏的商品 */
        const val TABLE_FAVORITES = "favorites"
        /** 商品足迹记录表 — 记录用户浏览过的商品及浏览日期 */
        const val TABLE_FOOTPRINTS = "footprints"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_USER (
                id          TEXT PRIMARY KEY,
                nickname    TEXT DEFAULT '',
                gender      TEXT DEFAULT '',
                age_range   TEXT DEFAULT '',
                budget_min  REAL DEFAULT 0,
                budget_max  REAL DEFAULT 99999,
                preferred_categories TEXT DEFAULT '[]',
                avatar      BLOB,
                is_guest    INTEGER DEFAULT 1,
                created_at  INTEGER NOT NULL,
                updated_at  INTEGER NOT NULL
            )
        """.trimIndent())

        val defaultUserId = "sw" + UUID.randomUUID().toString()
        db.execSQL("""
            INSERT INTO $TABLE_USER (id, created_at, updated_at)
            VALUES ('$defaultUserId', ${System.currentTimeMillis()}, ${System.currentTimeMillis()})
        """.trimIndent())

        // 对话会话
        db.execSQL("""
            CREATE TABLE $TABLE_CONVERSATIONS (
                id              TEXT PRIMARY KEY,
                title           TEXT DEFAULT '',
                message_count   INTEGER DEFAULT 0,
                last_message    TEXT DEFAULT '',
                created_at      INTEGER NOT NULL,
                updated_at      INTEGER NOT NULL
            )
        """.trimIndent())

        // 聊天消息
        db.execSQL("""
            CREATE TABLE $TABLE_MESSAGES (
                id              TEXT PRIMARY KEY,
                conversation_id TEXT NOT NULL,
                role            TEXT NOT NULL,
                content         TEXT DEFAULT '',
                product_cards   TEXT DEFAULT '[]',
                web_search_results TEXT DEFAULT '[]',
                status          TEXT DEFAULT 'sent',
                created_at      INTEGER NOT NULL,
                FOREIGN KEY (conversation_id) REFERENCES $TABLE_CONVERSATIONS(id)
            )
        """.trimIndent())

        // 购物车 — user_id 关联用户画像，支持多用户购物车隔离
        db.execSQL("""
            CREATE TABLE $TABLE_CART (
                product_id  TEXT PRIMARY KEY,
                session_id  TEXT NOT NULL,
                user_id     TEXT DEFAULT '',
                title       TEXT DEFAULT '',
                price       REAL DEFAULT 0,
                brand       TEXT DEFAULT '',
                category    TEXT DEFAULT '',
                image_url   TEXT DEFAULT '',
                rating      REAL DEFAULT 0,
                quantity    INTEGER DEFAULT 1,
                is_selected INTEGER DEFAULT 0,
                added_at    INTEGER NOT NULL
            )
        """.trimIndent())

        // 设置键值表
        db.execSQL("""
            CREATE TABLE $TABLE_SETTINGS (
                key         TEXT PRIMARY KEY,
                value       TEXT NOT NULL
            )
        """.trimIndent())

        // 搜索历史
        db.execSQL("""
            CREATE TABLE $TABLE_SEARCH (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                query       TEXT NOT NULL,
                created_at  INTEGER NOT NULL
            )
        """.trimIndent())

        // 客服对话记录
        db.execSQL("""
            CREATE TABLE $TABLE_CUSTOMER_SERVICE (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id     TEXT NOT NULL,
                role        TEXT NOT NULL,
                content     TEXT DEFAULT '',
                created_at  INTEGER NOT NULL,
                FOREIGN KEY (user_id) REFERENCES $TABLE_USER(id)
            )
        """.trimIndent())

        // 索引
        db.execSQL("CREATE INDEX idx_messages_conv ON $TABLE_MESSAGES(conversation_id, created_at)")
        db.execSQL("CREATE INDEX idx_cart_session ON $TABLE_CART(session_id)")
        db.execSQL("CREATE INDEX idx_cart_user ON $TABLE_CART(user_id)")
        db.execSQL("CREATE INDEX idx_search_time ON $TABLE_SEARCH(created_at DESC)")
        db.execSQL("CREATE INDEX idx_cs_user ON $TABLE_CUSTOMER_SERVICE(user_id, created_at)")

        // ═══════════════ 登录凭证表 ═══════════════
        // user_id: 外键关联 user_profile.id，游客登录时该表无记录
        // login_method: "phone" 或 "email"
        // password: AES-256-CBC 加密存储
        db.execSQL("""
            CREATE TABLE $TABLE_CREDENTIALS (
                user_id         TEXT PRIMARY KEY,
                login_method    TEXT NOT NULL,
                password        TEXT DEFAULT '',
                FOREIGN KEY (user_id) REFERENCES $TABLE_USER(id)
            )
        """.trimIndent())

        // ═══════════════ 收货地址表 ═══════════════
        // is_default: 1 为默认地址，每个用户仅允许一个默认地址，其余为 0
        // address_type: "" / "家" / "公司" / "学校"
        db.execSQL("""
            CREATE TABLE $TABLE_SHIPPING_ADDRESSES (
                user_id         TEXT NOT NULL,
                address_id      INTEGER PRIMARY KEY AUTOINCREMENT,
                is_default      INTEGER DEFAULT 0,
                phone           TEXT DEFAULT '',
                recipient_name  TEXT DEFAULT '',
                address_detail  TEXT DEFAULT '',
                address_type    TEXT DEFAULT '',
                FOREIGN KEY (user_id) REFERENCES $TABLE_USER(id)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX idx_shipping_user ON $TABLE_SHIPPING_ADDRESSES(user_id)")

        // ═══════════════ 支付设置表 ═══════════════
        // default_payment_method: "支付宝" 或 "微信"，默认 "支付宝"
        // payment_password: AES-256-CBC 加密存储
        // small_amount_password_free: 1 为开启，0 为关闭
        db.execSQL("""
            CREATE TABLE $TABLE_PAYMENT_SETTINGS (
                user_id                     TEXT PRIMARY KEY,
                default_payment_method      TEXT DEFAULT '支付宝',
                payment_password            TEXT DEFAULT '',
                small_amount_password_free   INTEGER DEFAULT 0,
                small_amount_limit          TEXT DEFAULT '',
                FOREIGN KEY (user_id) REFERENCES $TABLE_USER(id)
            )
        """.trimIndent())

        // ═══════════════ 国家与地区表 ═══════════════
        db.execSQL("""
            CREATE TABLE $TABLE_COUNTRY_REGION (
                user_id         TEXT PRIMARY KEY,
                country_region  TEXT DEFAULT '中国',
                FOREIGN KEY (user_id) REFERENCES $TABLE_USER(id)
            )
        """.trimIndent())

        // ═══════════════ 订单记录表 ═══════════════
        // status: "待付款" / "待发货" / "待收货" / "待评价" / "已完成" / "已取消"
        db.execSQL("""
            CREATE TABLE $TABLE_ORDER_RECORDS (
                user_id     TEXT NOT NULL,
                order_id    INTEGER PRIMARY KEY AUTOINCREMENT,
                order_body  TEXT DEFAULT '',
                created_at  INTEGER NOT NULL,
                status      TEXT DEFAULT '待付款',
                FOREIGN KEY (user_id) REFERENCES $TABLE_USER(id)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX idx_order_user ON $TABLE_ORDER_RECORDS(user_id, created_at DESC)")

        // ═══════════════ 登录状态表 ═══════════════
        // login_status: 0=未登录, 1=已登录
        // login_type: "" / "guest" / "non_guest"
        db.execSQL("""
            CREATE TABLE $TABLE_LOGIN_STATE (
                user_id      TEXT PRIMARY KEY,
                login_status INTEGER DEFAULT 0,
                login_type   TEXT DEFAULT '',
                FOREIGN KEY (user_id) REFERENCES $TABLE_USER(id)
            )
        """.trimIndent())
    }

    /**
     * 数据库迁移：v1 → v2。
     * v1 的 user_profile 表 id 为 INTEGER，升级为 TEXT UUID（"sw" 前缀）并新增 avatar BLOB 列。
     * 迁移流程：读取旧数据 → 删旧表 → 建新表 → 生成新 UUID → 写入迁移数据。
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            val cursor = db.rawQuery("SELECT nickname, gender, age_range, budget_min, budget_max, preferred_categories FROM $TABLE_USER WHERE id=1", null)
            var nickname = ""
            var gender = ""
            var ageRange = ""
            var budgetMin = 0.0
            var budgetMax = 99999.0
            var categories = "[]"
            if (cursor.moveToFirst()) {
                nickname = cursor.getString(0) ?: ""
                gender = cursor.getString(1) ?: ""
                ageRange = cursor.getString(2) ?: ""
                budgetMin = cursor.getDouble(3)
                budgetMax = cursor.getDouble(4)
                categories = cursor.getString(5) ?: "[]"
            }
            cursor.close()

            db.execSQL("DROP TABLE IF EXISTS $TABLE_USER")
            db.execSQL("""
                CREATE TABLE $TABLE_USER (
                    id          TEXT PRIMARY KEY,
                    nickname    TEXT DEFAULT '',
                    gender      TEXT DEFAULT '',
                    age_range   TEXT DEFAULT '',
                    budget_min  REAL DEFAULT 0,
                    budget_max  REAL DEFAULT 99999,
                    preferred_categories TEXT DEFAULT '[]',
                    avatar      BLOB,
                    created_at  INTEGER NOT NULL,
                    updated_at  INTEGER NOT NULL
                )
            """.trimIndent())

            val newUserId = "sw" + UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            db.execSQL("""
                INSERT INTO $TABLE_USER (id, nickname, gender, age_range, budget_min, budget_max, preferred_categories, created_at, updated_at)
                VALUES ('$newUserId', '$nickname', '$gender', '$ageRange', $budgetMin, $budgetMax, '$categories', $now, $now)
            """.trimIndent())
        }

        if (oldVersion < 3) {
            db.execSQL("""
                CREATE TABLE $TABLE_CUSTOMER_SERVICE (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id     TEXT NOT NULL,
                    role        TEXT NOT NULL,
                    content     TEXT DEFAULT '',
                    created_at  INTEGER NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES $TABLE_USER(id)
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_cs_user ON $TABLE_CUSTOMER_SERVICE(user_id, created_at)")
        }

        if (oldVersion < 4) {
            // v4: 新增 5 张业务表 + cart_items 关联 user_id

            // 登录凭证
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_CREDENTIALS (
                    user_id         TEXT PRIMARY KEY,
                    login_method    TEXT NOT NULL,
                    password        TEXT DEFAULT '',
                    FOREIGN KEY (user_id) REFERENCES $TABLE_USER(id)
                )
            """.trimIndent())

            // 收货地址
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_SHIPPING_ADDRESSES (
                    user_id         TEXT NOT NULL,
                    address_id      INTEGER PRIMARY KEY AUTOINCREMENT,
                    is_default      INTEGER DEFAULT 0,
                    phone           TEXT DEFAULT '',
                    recipient_name  TEXT DEFAULT '',
                    address_detail  TEXT DEFAULT '',
                    address_type    TEXT DEFAULT '',
                    FOREIGN KEY (user_id) REFERENCES $TABLE_USER(id)
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_shipping_user ON $TABLE_SHIPPING_ADDRESSES(user_id)")

            // 支付设置
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_PAYMENT_SETTINGS (
                    user_id                     TEXT PRIMARY KEY,
                    default_payment_method      TEXT DEFAULT '支付宝',
                    payment_password            TEXT DEFAULT '',
                    small_amount_password_free   INTEGER DEFAULT 0,
                    small_amount_limit          TEXT DEFAULT '',
                    FOREIGN KEY (user_id) REFERENCES $TABLE_USER(id)
                )
            """.trimIndent())

            // 国家与地区
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_COUNTRY_REGION (
                    user_id         TEXT PRIMARY KEY,
                    country_region  TEXT DEFAULT '中国',
                    FOREIGN KEY (user_id) REFERENCES $TABLE_USER(id)
                )
            """.trimIndent())

            // 订单记录
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_ORDER_RECORDS (
                    user_id     TEXT NOT NULL,
                    order_id    INTEGER PRIMARY KEY AUTOINCREMENT,
                    order_body  TEXT DEFAULT '',
                    created_at  INTEGER NOT NULL,
                    status      TEXT DEFAULT '待付款',
                    FOREIGN KEY (user_id) REFERENCES $TABLE_USER(id)
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_order_user ON $TABLE_ORDER_RECORDS(user_id, created_at DESC)")

            // cart_items 新增 user_id 列（可空，兼容已有数据）
            try {
                db.execSQL("ALTER TABLE $TABLE_CART ADD COLUMN user_id TEXT DEFAULT ''")
            } catch (_: Exception) {
                // 列已存在则忽略
            }
        }

        if (oldVersion < 5) {
            // v5: user_profile 新增 is_guest 列（1=游客，0=非游客），已有用户默认为游客
            try {
                db.execSQL("ALTER TABLE $TABLE_USER ADD COLUMN is_guest INTEGER DEFAULT 1")
            } catch (_: Exception) {
                // 列已存在则忽略
            }
        }

        if (oldVersion < 6) {
            // v6: 新增登录状态表，用于启动时判断是否跳过登录页
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_LOGIN_STATE (
                    user_id      TEXT PRIMARY KEY,
                    login_status INTEGER DEFAULT 0,
                    login_type   TEXT DEFAULT '',
                    FOREIGN KEY (user_id) REFERENCES $TABLE_USER(id)
                )
            """.trimIndent())
        }

        if (oldVersion < 7) {
            // v7: messages 新增 web_search_results 列，持久化联网搜索结果
            try {
                db.execSQL("ALTER TABLE $TABLE_MESSAGES ADD COLUMN web_search_results TEXT DEFAULT '[]'")
            } catch (_: Exception) {
                // 列已存在则忽略
            }
        }

        if (oldVersion < 8) {
            // v8: cart_items 新增 is_selected 列，记录商品选中状态
            try {
                db.execSQL("ALTER TABLE $TABLE_CART ADD COLUMN is_selected INTEGER DEFAULT 0")
            } catch (_: Exception) {
                android.util.Log.w("LocalDatabase", "v8 迁移：is_selected 列添加失败（可能已存在）")
            }
            // 验证列是否成功添加
            var columnAdded = false
            var cursor: android.database.Cursor? = null
            try {
                cursor = db.rawQuery("PRAGMA table_info($TABLE_CART)", null)
                while (cursor.moveToNext()) {
                    val colName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    if (colName == "is_selected") {
                        columnAdded = true
                        break
                    }
                }
            } catch (_: Exception) {
                // PRAGMA 查询失败，忽略
            } finally {
                cursor?.close()
            }
            if (!columnAdded) {
                android.util.Log.e("LocalDatabase", "v8 迁移失败：is_selected 列未能添加到 $TABLE_CART 表，购物车可能无法正常使用")
            }
        }

        if (oldVersion < 9) {
            // v9: 新增商品收藏记录表和商品足迹记录表
            // 收藏记录表 — 复合主键 (user_id, product_id)，满足 3NF
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_FAVORITES (
                    user_id     TEXT NOT NULL,
                    product_id  TEXT NOT NULL,
                    created_at  INTEGER NOT NULL,
                    PRIMARY KEY (user_id, product_id),
                    FOREIGN KEY (user_id) REFERENCES $TABLE_USER(id)
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_fav_user ON $TABLE_FAVORITES(user_id, created_at DESC)")

            // 足迹记录表 — 复合主键 (user_id, product_id)，满足 3NF
            // browse_date 仅记录年月日（毫秒时间戳在读写时转为日期）
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_FOOTPRINTS (
                    user_id     TEXT NOT NULL,
                    product_id  TEXT NOT NULL,
                    browse_date INTEGER NOT NULL,
                    created_at  INTEGER NOT NULL,
                    PRIMARY KEY (user_id, product_id),
                    FOREIGN KEY (user_id) REFERENCES $TABLE_USER(id)
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_fp_user ON $TABLE_FOOTPRINTS(user_id, browse_date DESC)")
        }
    }
}
