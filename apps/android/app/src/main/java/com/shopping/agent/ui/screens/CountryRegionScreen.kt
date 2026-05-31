package com.shopping.agent.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shopping.agent.data.local.UserRepository
import com.shopping.agent.ui.components.GradientTopBar
import com.shopping.agent.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 常用国家与地区列表（含区号，供参考）。
 */
private val COUNTRY_REGION_LIST = listOf(
    "中国" to "+86",
    "中国香港" to "+852",
    "中国澳门" to "+853",
    "中国台湾" to "+886",
    "日本" to "+81",
    "韩国" to "+82",
    "新加坡" to "+65",
    "马来西亚" to "+60",
    "泰国" to "+66",
    "越南" to "+84",
    "印度" to "+91",
    "印度尼西亚" to "+62",
    "菲律宾" to "+63",
    "美国" to "+1",
    "加拿大" to "+1",
    "英国" to "+44",
    "法国" to "+33",
    "德国" to "+49",
    "意大利" to "+39",
    "西班牙" to "+34",
    "澳大利亚" to "+61",
    "新西兰" to "+64",
    "俄罗斯" to "+7",
    "巴西" to "+55",
    "阿联酋" to "+971",
    "沙特阿拉伯" to "+966",
    "南非" to "+27",
    "埃及" to "+20",
    "墨西哥" to "+52",
    "阿根廷" to "+54",
)

/**
 * 国家与地区选择页面 — 用户可选择所在国家或地区。
 *
 * 功能：
 * - 列表展示常用国家与地区（含区号参考）
 * - 当前选中项高亮标记
 * - 点击即选中并同步到 SQLite
 * - 支持滚动浏览
 *
 * @param onBack 返回按钮回调
 */
@Composable
fun CountryRegionScreen(
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { UserRepository(context) }

    var currentRegion by remember { mutableStateOf("中国") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            currentRegion = withContext(Dispatchers.IO) { repository.getCountryRegion() }
        } catch (_: Exception) {
            currentRegion = "中国"
        }
        isLoading = false
    }

    val onRegionSelected: (String) -> Unit = { region ->
        scope.launch {
            try {
                withContext(Dispatchers.IO) { repository.saveCountryRegion(region) }
                currentRegion = region
                Toast.makeText(context, "地区已更新为：$region", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(context, "保存失败，请重试", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        GradientTopBar(icons = {
            IconButton(onClick = onBack, modifier = Modifier.size(34.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回",
                    tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(26.dp))
            }
            Text("国家与地区", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(start = 4.dp))
        })

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(COUNTRY_REGION_LIST) { (region, code) ->
                    CountryRegionRow(
                        regionName = region,
                        regionCode = code,
                        isSelected = region == currentRegion,
                        onClick = { onRegionSelected(region) },
                    )
                }
            }
        }
    }
}

/**
 * 国家与地区行 — 选中项用 RadioButton 高亮。
 */
@Composable
private fun CountryRegionRow(
    regionName: String,
    regionCode: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = Primary),
        )
        Spacer(Modifier.width(12.dp))
        Text(regionName, style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(regionCode, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "选择",
            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}
