package com.shopping.agent.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.shopping.agent.ui.theme.Neutral500
import com.shopping.agent.ui.theme.Neutral0

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

val bottomNavItems = listOf(
    BottomNavItem("home",        "主页", Icons.Filled.Home,          Icons.Outlined.Home),
    BottomNavItem("compare_tab", "比价", Icons.AutoMirrored.Filled.CompareArrows, Icons.AutoMirrored.Outlined.CompareArrows),
    BottomNavItem("explore",     "探索", Icons.Filled.Explore,       Icons.Outlined.Explore),
    BottomNavItem("cart",        "购物车", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart),
    BottomNavItem("profile",     "我的", Icons.Filled.Person,        Icons.Outlined.Person),
)

@Composable
fun MainBottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = Neutral0,
    ) {
        bottomNavItems.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                    )
                },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    unselectedIconColor  = Neutral500,
                    unselectedTextColor  = Neutral500,
                ),
            )
        }
    }
}
