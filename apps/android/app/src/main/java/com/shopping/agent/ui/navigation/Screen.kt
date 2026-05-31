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

    // 设置子页面
    data object ShippingAddress : Screen("shipping_address")
    data object PaymentSettings : Screen("payment_settings")
    data object CountryRegion : Screen("country_region")

    // 认证相关页面
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object ForgotPassword : Screen("forgot_password")
    data object PasswordReset : Screen("password_reset/{account}") {
        fun createRoute(account: String) = "password_reset/$account"
    }
}
