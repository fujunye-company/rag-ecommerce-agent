package com.shopping.agent.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.shopping.agent.ui.theme.*

@Composable
fun CategoryListScreen(navController: NavHostController? = null) {
    Column(modifier = Modifier.fillMaxSize().padding(Dimens.pageHorizontal)) {
        Text("商品分类列表 (Sprint 2 实现)", style = MaterialTheme.typography.headlineMedium)
    }
}
