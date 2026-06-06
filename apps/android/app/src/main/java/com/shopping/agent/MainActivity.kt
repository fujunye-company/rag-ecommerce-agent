package com.shopping.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.CompositionLocalProvider
import com.shopping.agent.data.local.UserRepository
import com.shopping.agent.ui.navigation.AppNavGraph
import com.shopping.agent.ui.theme.LocalThemeState
import com.shopping.agent.ui.theme.ShoppingTheme
import com.shopping.agent.ui.theme.rememberThemeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 接管系统冷启动：透明图标 + 白色背景，无图标放大动画
        // 必须在 super.onCreate 之前调用
        var splashDrawn = false
        val systemSplash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 系统启动画面一直保持，直到 Compose 开屏图完成首帧绘制
        systemSplash.setKeepOnScreenCondition { !splashDrawn }

        setContent {
            val themeState = rememberThemeState(this@MainActivity)
            CompositionLocalProvider(LocalThemeState provides themeState) {
                var showSplash by remember { mutableStateOf(true) }
                // null=加载中, true=已登录→home, false=未登录→login
                var startDestination by remember { mutableStateOf<String?>(null) }

                // 首帧绘制后立即释放系统启动画面
                SideEffect { splashDrawn = true }

                // 启动流程：快速确定导航目标 → 立即进入应用，购物车同步移至后台
                LaunchedEffect(Unit) {
                    // 1. 快速检查登录状态（单次 SQLite 查询，< 10ms）
                    val isLoggedIn = try {
                        withContext(Dispatchers.IO) {
                            UserRepository(this@MainActivity).isLoggedIn()
                        }
                    } catch (_: Exception) { false }
                    startDestination = if (isLoggedIn) "home" else "login"

                    // 2. 确定目标后立即释放启动画面，不再阻塞
                    showSplash = false

                    // 3. 后台异步：从后端同步购物车数据（不阻塞 UI 渲染）
                    if (isLoggedIn) {
                        try {
                            val prefs = getSharedPreferences("cart_prefs", android.content.Context.MODE_PRIVATE)
                            val sessionId = prefs.getString("cart_session_id", "") ?: ""
                            if (sessionId.isNotEmpty()) {
                                withContext(Dispatchers.IO) {
                                    UserRepository(this@MainActivity).syncCartFromBackend(sessionId)
                                }
                            }
                        } catch (_: Exception) {
                            // 同步失败不影响启动流程
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
                    // 开屏动画（"拾物"品牌图）
                    AnimatedVisibility(
                        visible = showSplash || startDestination == null,
                        exit = fadeOut(),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.splash_screen),
                            contentDescription = "开屏",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds,
                        )
                    }

                    // 登录状态已就绪且开屏结束 → 渲染应用主体
                    if (!showSplash && startDestination != null) {
                        ShoppingTheme {
                            AppNavGraph(startDestination = startDestination!!)
                        }
                    }
                }
            }
        }
    }
}
