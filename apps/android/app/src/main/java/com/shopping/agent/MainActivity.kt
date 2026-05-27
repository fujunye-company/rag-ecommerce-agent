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
import com.shopping.agent.ui.navigation.AppNavGraph
import com.shopping.agent.ui.theme.ShoppingTheme
import kotlinx.coroutines.delay

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
            ShoppingTheme {
                var showSplash by remember { mutableStateOf(true) }

                // 首帧绘制后立即释放系统启动画面
                SideEffect { splashDrawn = true }

                LaunchedEffect(Unit) {
                    delay(1500)
                    showSplash = false
                }

                Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
                    AnimatedVisibility(
                        visible = showSplash,
                        exit = fadeOut(),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.splash_screen),
                            contentDescription = "开屏",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds,
                        )
                    }

                    if (!showSplash) {
                        AppNavGraph()
                    }
                }
            }
        }
    }
}
