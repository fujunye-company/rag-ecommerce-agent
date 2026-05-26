package com.shopping.agent.ui.navigation

/**
 * 路由定义 — 所有页面路由枚举
 */
sealed class Screen(val route: String) {
    // 4 Tab 主页
    data object Home : Screen("home")
    data object CompareTab : Screen("compare_tab")
    data object Explore : Screen("explore")
    data object Profile : Screen("profile")

    // 二级页面
    data object Chat : Screen("chat")
    data object CategoryList : Screen("category_list")
    data object ProductDetail : Screen("product_detail/{productId}") {
        fun createRoute(productId: String) = "product_detail/$productId"
    }
    data object CompareScreen : Screen("compare_screen")
    data object Settings : Screen("settings")
    data object History : Screen("history")
}
