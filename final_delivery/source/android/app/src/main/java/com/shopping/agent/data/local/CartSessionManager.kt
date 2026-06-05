package com.shopping.agent.data.local

import android.content.Context
import java.util.UUID

object CartSessionManager {
    private const val PREFS_NAME = "cart_prefs"
    private const val KEY_CART_SESSION_ID = "cart_session_id"

    fun getOrCreate(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_CART_SESSION_ID, null)
        if (!existing.isNullOrBlank()) return existing

        val created = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_CART_SESSION_ID, created).apply()
        return created
    }

    fun getExisting(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CART_SESSION_ID, "") ?: ""
    }
}
