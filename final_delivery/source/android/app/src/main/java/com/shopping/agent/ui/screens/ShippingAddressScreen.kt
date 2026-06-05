package com.shopping.agent.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shopping.agent.data.local.UserRepository
import com.shopping.agent.data.local.UserRepository.ShippingAddress
import com.shopping.agent.ui.components.GradientTopBar
import com.shopping.agent.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 收货地址管理页面 — 支持地址的增删改查和默认地址设置。
 *
 * 功能：
 * - 列表展示所有收货地址，默认地址置顶并标注
 * - 新增/编辑地址弹窗（ModalBottomSheet 形式）
 * - 删除地址（需确认）
 * - 设为默认地址
 * - 地址类型标签（家/公司/学校）
 * - 数据持久化到 SQLite
 *
 * @param onBack 返回按钮回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShippingAddressScreen(
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { UserRepository(context) }

    var addresses by remember { mutableStateOf<List<ShippingAddress>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showEditSheet by remember { mutableStateOf(false) }
    var editingAddress by remember { mutableStateOf<ShippingAddress?>(null) }

    /** 从数据库加载地址列表 */
    fun loadAddresses() {
        scope.launch {
            try {
                addresses = withContext(Dispatchers.IO) { repository.getShippingAddresses() }
            } catch (_: Exception) {
                Toast.makeText(context, "加载地址失败", Toast.LENGTH_SHORT).show()
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadAddresses() }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        GradientTopBar(icons = {
            IconButton(onClick = onBack, modifier = Modifier.size(34.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回",
                    tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(26.dp))
            }
            Text("收货地址", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(start = 4.dp))
            // 新增按钮
            TextButton(onClick = {
                editingAddress = null
                showEditSheet = true
            }) {
                Text("新增", color = Primary, fontWeight = FontWeight.Medium)
            }
        })

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (addresses.isEmpty()) {
            // 空状态
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LocationOn, "无地址",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("暂无收货地址", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = {
                        editingAddress = null
                        showEditSheet = true
                    }) {
                        Text("添加新地址", color = Primary)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(addresses, key = { it.addressId }) { address ->
                    AddressCard(
                        address = address,
                        onEdit = {
                            editingAddress = address
                            showEditSheet = true
                        },
                        onDelete = {
                            scope.launch {
                                try {
                                    withContext(Dispatchers.IO) { repository.deleteShippingAddress(address.addressId) }
                                    loadAddresses()
                                    Toast.makeText(context, "地址已删除", Toast.LENGTH_SHORT).show()
                                } catch (_: Exception) {
                                    Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onSetDefault = {
                            scope.launch {
                                try {
                                    withContext(Dispatchers.IO) { repository.setDefaultShippingAddress(address.addressId) }
                                    loadAddresses()
                                } catch (_: Exception) {
                                    Toast.makeText(context, "设置失败", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    // 新增/编辑地址弹窗
    if (showEditSheet) {
        AddressEditSheet(
            address = editingAddress,
            onSave = { updated ->
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            if (updated.addressId == 0L) {
                                repository.addShippingAddress(updated)
                            } else {
                                repository.updateShippingAddress(updated)
                            }
                        }
                        showEditSheet = false
                        loadAddresses()
                        Toast.makeText(context, "地址已保存", Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {
                        Toast.makeText(context, "保存失败，请重试", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { showEditSheet = false },
        )
    }
}

/**
 * 收货地址卡片 — 展示单条地址信息。
 */
@Composable
private fun AddressCard(
    address: ShippingAddress,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit,
) {
    Card(
        shape = RadiusLg,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 顶行：姓名 + 电话 + 默认标签
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(address.recipientName, style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(12.dp))
                    Text(address.phone, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (address.isDefault) {
                    Surface(
                        shape = RadiusSm,
                        color = Primary.copy(alpha = 0.1f),
                    ) {
                        Text("默认", style = MaterialTheme.typography.labelSmall,
                            color = Primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
            }

            // 地址类型标签
            if (address.addressType.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                AddressTypeTag(address.addressType)
            }

            // 详细地址
            Spacer(Modifier.height(6.dp))
            Text(address.addressDetail, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis)

            // 操作按钮行
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!address.isDefault) {
                    TextButton(onClick = onSetDefault) {
                        Text("设为默认", color = Primary, style = MaterialTheme.typography.bodySmall)
                    }
                }
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "编辑", tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("编辑", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "删除", tint = ErrorColor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("删除", color = ErrorColor, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/**
 * 地址类型小标签 — 家/公司/学校。
 */
@Composable
private fun AddressTypeTag(type: String) {
    val (bgColor, textColor) = when (type) {
        "家" -> Warning.copy(alpha = 0.15f) to Warning
        "公司" -> Primary.copy(alpha = 0.15f) to Primary
        "学校" -> Success.copy(alpha = 0.15f) to Success
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f) to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = RadiusSm, color = bgColor) {
        Text(type, style = MaterialTheme.typography.labelSmall, color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
    }
}

/**
 * 地址编辑弹窗 — ModalBottomSheet 形式，支持新增和编辑两种模式。
 * @param address 现有地址，为 null 表示新增模式
 * @param onSave 保存回调
 * @param onDismiss 关闭回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddressEditSheet(
    address: ShippingAddress?,
    onSave: (ShippingAddress) -> Unit,
    onDismiss: () -> Unit,
) {
    val isEditMode = address != null && address.addressId != 0L

    var recipientName by remember { mutableStateOf(address?.recipientName ?: "") }
    var phone by remember { mutableStateOf(address?.phone ?: "") }
    var addressDetail by remember { mutableStateOf(address?.addressDetail ?: "") }
    var addressType by remember { mutableStateOf(address?.addressType ?: "") }
    var isDefault by remember { mutableStateOf(address?.isDefault ?: false) }

    val context = LocalContext.current
    val addressTypes = listOf("" to "无", "家" to "家", "公司" to "公司", "学校" to "学校")

    val canSave = recipientName.isNotBlank() && phone.isNotBlank() && addressDetail.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
        ) {
            // 标题栏
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    if (isEditMode) "编辑地址" else "新增地址",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.Center),
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterEnd).size(32.dp),
                ) {
                    Icon(Icons.Default.Close, "关闭", tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            // 表单区
            Column(
                modifier = Modifier.padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 收货人姓名
                OutlinedTextField(
                    value = recipientName,
                    onValueChange = { recipientName = it },
                    label = { Text("收货人姓名") },
                    placeholder = { Text("请输入收货人姓名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RadiusMd,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                )

                // 联系电话
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("联系电话") },
                    placeholder = { Text("请输入联系电话") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RadiusMd,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                )

                // 详细地址
                OutlinedTextField(
                    value = addressDetail,
                    onValueChange = { addressDetail = it },
                    label = { Text("详细地址") },
                    placeholder = { Text("省/市/区/街道/门牌号") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RadiusMd,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                )

                // 地址类型选择
                Text("地址类型", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    addressTypes.forEach { (key, label) ->
                        FilterChip(
                            selected = addressType == key,
                            onClick = { addressType = key },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary.copy(alpha = 0.15f),
                                selectedLabelColor = Primary,
                            ),
                        )
                    }
                }

                // 设为默认地址
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { isDefault = !isDefault }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it },
                        colors = CheckboxDefaults.colors(checkedColor = Primary),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("设为默认地址", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                }

                Spacer(Modifier.height(8.dp))

                // 保存按钮
                Button(
                    onClick = {
                        onSave(ShippingAddress(
                            addressId = address?.addressId ?: 0L,
                            recipientName = recipientName.trim(),
                            phone = phone.trim(),
                            addressDetail = addressDetail.trim(),
                            addressType = addressType,
                            isDefault = isDefault,
                        ))
                    },
                    enabled = canSave,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RadiusMd,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        contentColor = OnPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.outline,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    Text("保存", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
