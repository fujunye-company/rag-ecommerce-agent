package com.shopping.agent.ui.navigation

/**
 * 导航图 — Home → Chat → Detail → Compare → History
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Chat : Screen("chat")
    data class ProductDetail(val productId: String) : Screen("detail/{productId}")
    object Compare : Screen("compare")  // [全量]
    object History : Screen("history")  // [全量]
}
