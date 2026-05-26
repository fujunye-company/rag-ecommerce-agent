package com.shopping.agent.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shopping.agent.ui.theme.*

@Composable
fun CartPreviewSection(modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RadiusLg) {
        Text("购物车预览 (Sprint 2 实现)", modifier = Modifier.padding(Dimens.cardPadding))
    }
}
