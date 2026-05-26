package com.shopping.agent.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shopping.agent.ui.theme.*

@Composable
fun ProductTrendCard(modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RadiusLg) {
        Text("价格趋势 (Sprint 3 实现)", modifier = Modifier.padding(Dimens.cardPadding))
    }
}
