package com.shopping.agent

import android.app.Application

/**
 * Application 入口 — 全局初始化 (Hilt/DI 预留)
 */
class ShoppingApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // TODO: Hilt DI 初始化 (全量阶段)
    }
}
