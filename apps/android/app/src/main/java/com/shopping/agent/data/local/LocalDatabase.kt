package com.shopping.agent.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * 本地长效数据库 — 对标微信 WCDB / 豆包 Room 的记忆机制。
 *
 * 表结构:
 *   user_profile     用户画像 + 偏好
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
        const val DATABASE_NAME = "hermes_local.db"
        const val DATABASE_VERSION = 2

        // 表名
        const val TABLE_USER = "user_profile"
        const val TABLE_CONVERSATIONS = "conversations"
        const val TABLE_MESSAGES = "messages"
        const val TABLE_CART = "cart_items"
        const val TABLE_SETTINGS = "settings"
        const val TABLE_SEARCH = "search_history"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 用户画像
        db.execSQL("""
            CREATE TABLE $TABLE_USER (
                id          INTEGER PRIMARY KEY DEFAULT 1,
                nickname    TEXT DEFAULT '',
                gender      TEXT DEFAULT '',
                age_range   TEXT DEFAULT '',
                budget_min  REAL DEFAULT 0,
                budget_max  REAL DEFAULT 99999,
                preferred_categories TEXT DEFAULT '[]',
                created_at  INTEGER NOT NULL,
                updated_at  INTEGER NOT NULL
            )
        """.trimIndent())

        // 确保有一条默认用户记录
        db.execSQL("""
            INSERT INTO $TABLE_USER (id, created_at, updated_at)
            VALUES (1, ${System.currentTimeMillis()}, ${System.currentTimeMillis()})
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

        // 购物车
        db.execSQL("""
            CREATE TABLE $TABLE_CART (
                product_id  TEXT PRIMARY KEY,
                session_id  TEXT NOT NULL,
                title       TEXT DEFAULT '',
                price       REAL DEFAULT 0,
                brand       TEXT DEFAULT '',
                category    TEXT DEFAULT '',
                image_url   TEXT DEFAULT '',
                rating      REAL DEFAULT 0,
                quantity    INTEGER DEFAULT 1,
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

        // 索引
        db.execSQL("CREATE INDEX idx_messages_conv ON $TABLE_MESSAGES(conversation_id, created_at)")
        db.execSQL("CREATE INDEX idx_cart_session ON $TABLE_CART(session_id)")
        db.execSQL("CREATE INDEX idx_search_time ON $TABLE_SEARCH(created_at DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_MESSAGES ADD COLUMN web_search_results TEXT DEFAULT '[]'")
        }
    }
}
