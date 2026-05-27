package com.shopping.agent.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shopping.agent.ui.components.HistoryDrawer
import com.shopping.agent.ui.components.MainBottomNavBar
import com.shopping.agent.ui.components.bottomNavItems
import com.shopping.agent.ui.screens.*
import com.shopping.agent.viewmodel.ChatViewModel

private val tabRoutes = bottomNavItems.map { it.route }.toSet()

/** CompositionLocal: 各页面可通过它打开历史抽屉 */
val LocalOnMenuClick = staticCompositionLocalOf<() -> Unit> { {} }

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isTabRoute = currentRoute in tabRoutes

    var drawerVisible by remember { mutableStateOf(false) }

    // 共享 ChatViewModel（多对话支持）
    val chatViewModel: ChatViewModel = viewModel()
    val uiState by chatViewModel.uiState.collectAsState()

    CompositionLocalProvider(LocalOnMenuClick provides { drawerVisible = true }) {
        Box {
            Scaffold(
                bottomBar = {
                    if (isTabRoute) {
                        MainBottomNavBar(currentRoute = currentRoute, onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        })
                    }
                }
            ) { innerPadding ->
                NavHost(navController = navController, startDestination = "home",
                    modifier = Modifier.padding(innerPadding)) {
                    composable("home") {
                        HomeScreen(chatViewModel = chatViewModel)
                    }
                    composable("compare_tab") {
                        CompareTabScreen()
                    }
                    composable("explore") {
                        ExploreScreen(
                            chatViewModel = chatViewModel,
                            onChatSend = { navController.navigate("home") {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }},
                            onPostClick = { postId ->
                                navController.navigate("explore_post/$postId")
                            }
                        )
                    }
                    composable("profile") {
                        ProfileScreen(onSettingsClick = { navController.navigate("settings") })
                    }
                    composable("cart") { CartScreen(onBack = { navController.popBackStack() }) }
                    composable("settings") { SettingsScreen(onBack = { navController.popBackStack() }) }
                    composable("category_list") { CategoryListScreen(navController = navController) }
                    composable("explore_post/{postId}",
                        arguments = listOf(navArgument("postId") { type = NavType.StringType })) { entry ->
                        ExploreProductPostScreen(
                            postId = entry.arguments?.getString("postId") ?: "",
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("history") { HistoryScreen() }
                }
            }

            // 挂画式历史侧边栏 — 真实数据
            HistoryDrawer(
                visible = drawerVisible,
                onDismiss = { drawerVisible = false },
                conversations = uiState.conversations,
                currentConversationId = uiState.currentConversationId,
                onSessionClick = { convId ->
                    chatViewModel.loadConversation(convId)
                },
                onNewChat = {
                    chatViewModel.createNewConversation()
                },
                onDeleteConversation = { convId ->
                    chatViewModel.deleteConversation(convId)
                },
            )
        }
    }
}
