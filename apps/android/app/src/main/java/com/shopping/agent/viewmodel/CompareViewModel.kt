package com.shopping.agent.viewmodel

import androidx.lifecycle.ViewModel

/**
 * 对比 VM — 选中商品, 对比维度 [全量预留]
 */
class CompareViewModel : ViewModel() {
    val selectedIds = mutableListOf<String>()
    var dimensions: List<String> = emptyList()
}
