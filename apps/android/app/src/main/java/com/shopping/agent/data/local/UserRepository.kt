package com.shopping.agent.data.local

import android.content.ContentValues
import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.shopping.agent.data.model.CartItem
import com.shopping.agent.data.model.ChatMessage
import com.shopping.agent.data.model.MessageRole
import com.shopping.agent.data.model.Product
import com.shopping.agent.data.model.WebSearchItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 用户数据仓库 — 封装 LocalDatabase 的 CRUD 操作。
 *
 * 对标:
 *   微信 WCDB → 消息/联系人本地缓存
 *   豆包 Room → 对话历史/用户偏好持久化
 */
class UserRepository(context: Context) {

    private val db = LocalDatabase(context)
    private val gson = Gson()

    // ═══════════════════════════════════════════════════════
    // 用户画像
    // ═══════════════════════════════════════════════════════

    suspend fun getUserProfile(): Map<String, String> = withContext(Dispatchers.IO) {
        val cursor = db.readableDatabase.query(
            LocalDatabase.TABLE_USER, null, "id=1", null, null, null, null
        )
        val result = mutableMapOf<String, String>()
        if (cursor.moveToFirst()) {
            for (i in 0 until cursor.columnCount) {
                result[cursor.getColumnName(i)] = cursor.getString(i) ?: ""
            }
        }
        cursor.close()
        result
    }

    suspend fun updateUserProfile(fields: Map<String, String>) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            for ((k, v) in fields) put(k, v)
            put("updated_at", System.currentTimeMillis())
        }
        db.writableDatabase.update(LocalDatabase.TABLE_USER, cv, "id=1", null)
    }

    // ═══════════════════════════════════════════════════════
    // 对话会话
    // ═══════════════════════════════════════════════════════

    suspend fun createConversation(id: String, title: String = "") = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cv = ContentValues().apply {
            put("id", id)
            put("title", title)
            put("created_at", now)
            put("updated_at", now)
        }
        // INSERT OR IGNORE preserves existing rows (and their created_at)
        db.writableDatabase.insertWithOnConflict(
            LocalDatabase.TABLE_CONVERSATIONS, null, cv,
            android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE
        )
        // Always refresh updated_at so the conversation moves to top of list
        val updateCv = ContentValues().apply {
            put("updated_at", now)
        }
        db.writableDatabase.update(LocalDatabase.TABLE_CONVERSATIONS, updateCv, "id=?", arrayOf(id))
    }

    suspend fun getConversations(limit: Int = 20): List<Map<String, String>> = withContext(Dispatchers.IO) {
        val cursor = db.readableDatabase.query(
            LocalDatabase.TABLE_CONVERSATIONS, null, null, null, null, null,
            "updated_at DESC", limit.toString()
        )
        val list = mutableListOf<Map<String, String>>()
        while (cursor.moveToNext()) {
            val row = mutableMapOf<String, String>()
            for (i in 0 until cursor.columnCount) {
                row[cursor.getColumnName(i)] = cursor.getString(i) ?: ""
            }
            list.add(row)
        }
        cursor.close()
        list
    }

    suspend fun getConversationMetas(limit: Int = 50): List<com.shopping.agent.data.model.ConversationMeta> = withContext(Dispatchers.IO) {
        val cursor = db.readableDatabase.query(
            LocalDatabase.TABLE_CONVERSATIONS, null, null, null, null, null,
            "updated_at DESC", limit.toString()
        )
        val list = mutableListOf<com.shopping.agent.data.model.ConversationMeta>()
        while (cursor.moveToNext()) {
            list.add(com.shopping.agent.data.model.ConversationMeta(
                id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                title = cursor.getString(cursor.getColumnIndexOrThrow("title")) ?: "",
                messageCount = cursor.getInt(cursor.getColumnIndexOrThrow("message_count")),
                lastMessage = cursor.getString(cursor.getColumnIndexOrThrow("last_message")) ?: "",
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at")),
            ))
        }
        cursor.close()
        list
    }

    suspend fun updateConversationTitle(convId: String, title: String) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put("title", title)
            put("updated_at", System.currentTimeMillis())
        }
        db.writableDatabase.update(LocalDatabase.TABLE_CONVERSATIONS, cv, "id=?", arrayOf(convId))
    }

    suspend fun deleteConversation(convId: String) = withContext(Dispatchers.IO) {
        db.writableDatabase.delete(LocalDatabase.TABLE_MESSAGES, "conversation_id=?", arrayOf(convId))
        db.writableDatabase.delete(LocalDatabase.TABLE_CONVERSATIONS, "id=?", arrayOf(convId))
    }

    // ═══════════════════════════════════════════════════════
    // 聊天消息
    // ═══════════════════════════════════════════════════════

    suspend fun saveMessage(message: ChatMessage, conversationId: String) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put("id", message.id)
            put("conversation_id", conversationId)
            put("role", message.role.name)
            put("content", message.content)
            put("product_cards", gson.toJson(message.productCards))
            put("web_search_results", gson.toJson(message.webSearchResults))
            put("status", message.status.name)
            put("created_at", System.currentTimeMillis())
        }
        db.writableDatabase.insertWithOnConflict(
            LocalDatabase.TABLE_MESSAGES, null, cv,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )

        // 更新会话元数据 (COUNT(*) 已含本消息，不再 +1)
        val convCv = ContentValues().apply {
            put("message_count", getMessageCount(conversationId))
            put("last_message", message.content.take(100))
            put("updated_at", System.currentTimeMillis())
        }
        db.writableDatabase.update(
            LocalDatabase.TABLE_CONVERSATIONS, convCv, "id=?", arrayOf(conversationId)
        )
    }

    suspend fun getMessages(conversationId: String, limit: Int = 50): List<ChatMessage> = withContext(Dispatchers.IO) {
        val cursor = db.readableDatabase.query(
            LocalDatabase.TABLE_MESSAGES, null,
            "conversation_id=?", arrayOf(conversationId),
            null, null, "created_at ASC", limit.toString()
        )
        val list = mutableListOf<ChatMessage>()
        while (cursor.moveToNext()) {
            val cardsJson = cursor.getString(cursor.getColumnIndexOrThrow("product_cards")) ?: "[]"
            val cards: List<Product> = try {
                gson.fromJson(cardsJson, object : TypeToken<List<Product>>() {}.type)
            } catch (e: Exception) {
                Log.e("UserRepository", "Failed to deserialize product cards", e)
                emptyList()
            }

            val webJson = cursor.getString(cursor.getColumnIndexOrThrow("web_search_results")) ?: "[]"
            val webResults: List<WebSearchItem> = try {
                gson.fromJson(webJson, object : TypeToken<List<WebSearchItem>>() {}.type)
            } catch (e: Exception) {
                Log.e("UserRepository", "Failed to deserialize web search results", e)
                emptyList()
            }

            list.add(ChatMessage(
                id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                role = MessageRole.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("role"))),
                content = cursor.getString(cursor.getColumnIndexOrThrow("content")) ?: "",
                productCards = cards,
                webSearchResults = webResults,
                status = com.shopping.agent.data.model.MessageStatus.valueOf(
                    cursor.getString(cursor.getColumnIndexOrThrow("status")) ?: "Sent"
                ),
            ))
        }
        cursor.close()
        list
    }

    suspend fun getMessageCount(conversationId: String): Int = withContext(Dispatchers.IO) {
        val cursor = db.readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM ${LocalDatabase.TABLE_MESSAGES} WHERE conversation_id=?",
            arrayOf(conversationId)
        )
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        count
    }

    // ═══════════════════════════════════════════════════════
    // 购物车 (本地缓存)
    // ═══════════════════════════════════════════════════════

    suspend fun saveCartItem(product: Product, sessionId: String, quantity: Int = 1) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put("product_id", product.productId)
            put("session_id", sessionId)
            put("title", product.title)
            put("price", product.price)
            put("brand", product.brand ?: "")
            put("category", product.category)
            put("image_url", product.imageUrl ?: "")
            put("rating", product.rating.toDouble())
            put("quantity", quantity)
            put("added_at", System.currentTimeMillis())
        }
        db.writableDatabase.insertWithOnConflict(
            LocalDatabase.TABLE_CART, null, cv,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    suspend fun getCartItems(sessionId: String): List<CartItem> = withContext(Dispatchers.IO) {
        val cursor = db.readableDatabase.query(
            LocalDatabase.TABLE_CART, null,
            "session_id=?", arrayOf(sessionId),
            null, null, "added_at DESC"
        )
        val list = mutableListOf<CartItem>()
        while (cursor.moveToNext()) {
            val product = Product(
                productId = cursor.getString(cursor.getColumnIndexOrThrow("product_id")),
                title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                price = cursor.getDouble(cursor.getColumnIndexOrThrow("price")),
                brand = cursor.getString(cursor.getColumnIndexOrThrow("brand")).takeIf { it.isNotEmpty() },
                category = cursor.getString(cursor.getColumnIndexOrThrow("category")),
                imageUrl = cursor.getString(cursor.getColumnIndexOrThrow("image_url")).takeIf { it.isNotEmpty() },
                rating = cursor.getDouble(cursor.getColumnIndexOrThrow("rating")).toFloat(),
            )
            list.add(CartItem(product, cursor.getInt(cursor.getColumnIndexOrThrow("quantity"))))
        }
        cursor.close()
        list
    }

    suspend fun removeCartItem(productId: String) = withContext(Dispatchers.IO) {
        db.writableDatabase.delete(LocalDatabase.TABLE_CART, "product_id=?", arrayOf(productId))
    }

    suspend fun clearCart(sessionId: String) = withContext(Dispatchers.IO) {
        db.writableDatabase.delete(LocalDatabase.TABLE_CART, "session_id=?", arrayOf(sessionId))
    }

    // ═══════════════════════════════════════════════════════
    // 设置
    // ═══════════════════════════════════════════════════════

    suspend fun getSetting(key: String, default: String = ""): String = withContext(Dispatchers.IO) {
        getSettingSync(key, default)
    }

    fun getSettingSync(key: String, default: String = ""): String {
        val cursor = db.readableDatabase.query(
            LocalDatabase.TABLE_SETTINGS, arrayOf("value"),
            "key=?", arrayOf(key), null, null, null
        )
        val value = if (cursor.moveToFirst()) cursor.getString(0) ?: default else default
        cursor.close()
        return value
    }

    suspend fun setSetting(key: String, value: String) = withContext(Dispatchers.IO) {
        setSettingSync(key, value)
    }

    fun setSettingSync(key: String, value: String) {
        val cv = ContentValues().apply {
            put("key", key)
            put("value", value)
        }
        db.writableDatabase.insertWithOnConflict(
            LocalDatabase.TABLE_SETTINGS, null, cv,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    // ═══════════════════════════════════════════════════════
    // 搜索历史
    // ═══════════════════════════════════════════════════════

    suspend fun addSearchHistory(query: String) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put("query", query)
            put("created_at", System.currentTimeMillis())
        }
        db.writableDatabase.insert(LocalDatabase.TABLE_SEARCH, null, cv)
        // 只保留最近 50 条
        db.writableDatabase.execSQL(
            "DELETE FROM ${LocalDatabase.TABLE_SEARCH} WHERE id NOT IN (SELECT id FROM ${LocalDatabase.TABLE_SEARCH} ORDER BY created_at DESC LIMIT 50)"
        )
    }

    suspend fun getSearchHistory(limit: Int = 20): List<String> = withContext(Dispatchers.IO) {
        val cursor = db.readableDatabase.query(
            LocalDatabase.TABLE_SEARCH, arrayOf("query"),
            null, null, null, null, "created_at DESC", limit.toString()
        )
        val list = mutableListOf<String>()
        while (cursor.moveToNext()) list.add(cursor.getString(0))
        cursor.close()
        list
    }
}
