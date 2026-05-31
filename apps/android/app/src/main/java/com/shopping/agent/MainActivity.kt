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
import kotlinx.coroutines.delay
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

                // 并行：登录状态检查 + 最短开屏时间，两者都完成后才进入应用
                LaunchedEffect(Unit) {
                    // 先检查登录状态（IO 操作）
                    val isLoggedIn = try {
                        withContext(Dispatchers.IO) {
                            UserRepository(this@MainActivity).isLoggedIn()
                        }
                    } catch (_: Exception) { false }
                    startDestination = if (isLoggedIn) "home" else "login"
                    // 确保开屏动画至少展示 1.5 秒
                    delay(1500)
                    showSplash = false
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
