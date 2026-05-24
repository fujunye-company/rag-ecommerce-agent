package com.shopping.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.shopping.agent.ui.navigation.AppNavGraph
import com.shopping.agent.ui.theme.ShoppingTheme

/**
 * 主 Activity — 导航容器, Compose 入口
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShoppingTheme {
                AppNavGraph()
            }
        }
    }
}
