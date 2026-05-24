package com.shopping.agent.ui.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.shopping.agent.ui.chat.ChatScreen
import com.shopping.agent.ui.components.HistoryDrawer
import com.shopping.agent.ui.components.MainBottomNavBar
import com.shopping.agent.ui.components.bottomNavItems
import com.shopping.agent.ui.screens.CategoryListScreen
import com.shopping.agent.ui.screens.CompareTabScreen
import com.shopping.agent.ui.screens.ExploreScreen
import com.shopping.agent.ui.screens.HomeScreen
import com.shopping.agent.ui.screens.ProfileScreen
import kotlinx.coroutines.launch

/**
 * Tab 路由集合 — 用于判断当前是否在主页 Tab 上
 */
private val tabRoutes = bottomNavItems.map { it.route }.toSet()

/**
 * 顶层导航图 — ModalNavigationDrawer + Scaffold + NavHost
 *
 * - Tab 路由显示底部导航栏 + 顶部菜单按钮（打开历史抽屉）
 * - chat 等二级路由隐藏底部导航栏和顶部菜单
 * - 左侧抽屉：HistoryDrawer（月份分组 + 搜索 + 会话列表）
 */
@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isTabRoute = currentRoute in tabRoutes

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            HistoryDrawer(
                onSessionClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate("chat")
                },
                onNewChat = {
                    scope.launch { drawerState.close() }
                    navController.navigate("chat")
                },
                onLogout = {
                    scope.launch { drawerState.close() }
                    // TODO: 实现退出登录逻辑
                },
            )
        },
    ) {
        Scaffold(
            topBar = {
                if (isTabRoute) {
                    // 顶部菜单按钮 — 打开历史侧边栏
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 1.dp,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = {
                                scope.launch { drawerState.open() }
                            }) {
                                Icon(
                                    Icons.Outlined.Menu,
                                    contentDescription = "打开历史菜单",
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                if (isTabRoute) {
                    MainBottomNavBar(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(innerPadding)
            ) {
                // === 4 个 Tab 页 ===
                composable("home") { HomeScreen(navController = navController) }
                composable("compare_tab") { CompareTabScreen() }
                composable("explore") { ExploreScreen() }
                composable("profile") { ProfileScreen() }

                // === P02 商品列表页 ===
                composable("category_list") { CategoryListScreen(navController = navController) }

                // === 二级路由：ChatScreen (自带 Scaffold) ===
                composable("chat") {
                    // TODO: 接入 ChatViewModel 真实数据
                    ChatScreen(
                        messages = emptyList(),
                        isLoading = false,
                        error = null,
                        onSendMessage = {},
                        onClearError = {},
                        onFeedback = { _, _, _ -> }
                    )
                }
            }
        }
    }
}
