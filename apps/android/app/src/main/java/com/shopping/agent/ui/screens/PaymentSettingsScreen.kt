package com.shopping.agent.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.shopping.agent.data.local.CryptoUtil
import com.shopping.agent.data.local.UserRepository
import com.shopping.agent.data.local.UserRepository.PaymentSettings
import com.shopping.agent.ui.components.GradientTopBar
import com.shopping.agent.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 支付设置页面 — 管理支付方式、支付密码和小额免密支付。
 *
 * 功能：
 * - 默认支付方式选择（支付宝/微信）
 * - 支付密码设置（AES 加密存储）
 * - 小额免密支付开关及额度设置
 * - 数据持久化到 SQLite
 *
 * @param onBack 返回按钮回调
 */
@Composable
fun PaymentSettingsScreen(
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { UserRepository(context) }

    var settings by remember { mutableStateOf(PaymentSettings()) }
    var isLoading by remember { mutableStateOf(true) }
    var showPasswordSheet by remember { mutableStateOf(false) }

    // 支付方式选择
    var defaultPaymentMethod by remember { mutableStateOf("支付宝") }
    // 小额免密
    var smallAmountFree by remember { mutableStateOf(false) }
    var smallAmountLimit by remember { mutableStateOf("") }
    // 密码显示/隐藏
    var passwordVisible by remember { mutableStateOf(false) }

    fun loadSettings() {
        scope.launch {
            try {
                settings = withContext(Dispatchers.IO) { repository.getPaymentSettings() }
                defaultPaymentMethod = settings.defaultPaymentMethod
                smallAmountFree = settings.smallAmountPasswordFree
                smallAmountLimit = settings.smallAmountLimit
            } catch (_: Exception) {
                Toast.makeText(context, "加载支付设置失败", Toast.LENGTH_SHORT).show()
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadSettings() }

    // 自动保存支付方式和免密设置变更
    fun saveSettings() {
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.savePaymentSettings(PaymentSettings(
                        defaultPaymentMethod = defaultPaymentMethod,
                        paymentPassword = settings.paymentPassword,
                        smallAmountPasswordFree = smallAmountFree,
                        smallAmountLimit = smallAmountLimit,
                    ))
                }
                settings = settings.copy(
                    defaultPaymentMethod = defaultPaymentMethod,
                    smallAmountPasswordFree = smallAmountFree,
                    smallAmountLimit = smallAmountLimit,
                )
            } catch (_: Exception) {
                Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
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
            Text("支付设置", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(start = 4.dp))
        })

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 分组1: 支付方式
                SettingsSectionCard("支付方式") {
                    // 支付宝
                    PaymentMethodRow(
                        icon = Icons.Default.AccountBalanceWallet,
                        title = "支付宝",
                        subtitle = "推荐使用支付宝支付",
                        selected = defaultPaymentMethod == "支付宝",
                        onClick = {
                            defaultPaymentMethod = "支付宝"
                            saveSettings()
                        },
                    )
                    SettingsDivider()
                    // 微信
                    PaymentMethodRow(
                        icon = Icons.AutoMirrored.Filled.Chat,
                        title = "微信支付",
                        subtitle = "使用微信支付",
                        selected = defaultPaymentMethod == "微信",
                        onClick = {
                            defaultPaymentMethod = "微信"
                            saveSettings()
                        },
                    )
                }

                // 分组2: 支付密码
                SettingsSectionCard("安全设置") {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showPasswordSheet = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Lock, "支付密码",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("支付密码", style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                if (settings.paymentPassword.isNotEmpty()) "已设置" else "未设置",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (settings.paymentPassword.isNotEmpty()) Success else Warning,
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "进入",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                }

                // 分组3: 小额免密支付
                SettingsSectionCard("小额免密支付") {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("小额免密支付", style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface)
                            Text("开启后小额支付无需输入密码", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = smallAmountFree,
                            onCheckedChange = {
                                smallAmountFree = it
                                saveSettings()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Primary, checkedTrackColor = Primary.copy(alpha = 0.3f)),
                        )
                    }

                    // 免密额度设置（仅在开启时显示）
                    if (smallAmountFree) {
                        SettingsDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("免密额度（元）", style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            OutlinedTextField(
                                value = smallAmountLimit,
                                onValueChange = { newValue ->
                                    smallAmountLimit = newValue
                                },
                                placeholder = { Text("如 200") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(120.dp).height(48.dp),
                                shape = RadiusMd,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                ),
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        // 保存额度按钮
                        Button(
                            onClick = {
                                saveSettings()
                                Toast.makeText(context, "免密额度已保存", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(44.dp),
                            shape = RadiusMd,
                            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
                        ) {
                            Text("保存额度", style = MaterialTheme.typography.bodyMedium)
                        }

                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    // 支付密码设置弹窗
    if (showPasswordSheet) {
        PaymentPasswordSheet(
            hasExistingPassword = settings.paymentPassword.isNotEmpty(),
            onConfirm = { newPassword ->
                scope.launch {
                    try {
                        val encrypted = withContext(Dispatchers.IO) {
                            CryptoUtil.encrypt(newPassword)
                        }
                        withContext(Dispatchers.IO) {
                            repository.savePaymentSettings(settings.copy(paymentPassword = encrypted))
                        }
                        settings = settings.copy(paymentPassword = encrypted)
                        showPasswordSheet = false
                        Toast.makeText(context, "支付密码已更新", Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {
                        Toast.makeText(context, "密码设置失败", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { showPasswordSheet = false },
        )
    }
}

/**
 * 支付方式行 — 带 RadioButton 的选择行。
 */
@Composable
private fun PaymentMethodRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = Primary),
        )
        Spacer(Modifier.width(12.dp))
        Icon(icon, title, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * 支付密码设置弹窗 — ModalBottomSheet 形式。
 * 支持首次设置和修改密码（需验证旧密码）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentPasswordSheet(
    hasExistingPassword: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf<String?>(null) }

    val canSave = when {
        hasExistingPassword && oldPassword.isEmpty() -> false
        newPassword.length < 6 -> false
        newPassword != confirmPassword -> false
        else -> true
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    if (hasExistingPassword) "修改支付密码" else "设置支付密码",
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

            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 旧密码（修改时显示）
                if (hasExistingPassword) {
                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it; showError = null },
                        label = { Text("当前支付密码") },
                        placeholder = { Text("请输入当前支付密码") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RadiusMd,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        ),
                    )
                }

                // 新密码
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; showError = null },
                    label = { Text(if (hasExistingPassword) "新支付密码" else "支付密码") },
                    placeholder = { Text("请输入6位数字密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RadiusMd,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                    supportingText = {
                        Text("密码需为6位以上数字", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                )

                // 确认新密码
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; showError = null },
                    label = { Text("确认支付密码") },
                    placeholder = { Text("请再次输入支付密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RadiusMd,
                    isError = showError != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                )

                showError?.let { error ->
                    Text(error, color = ErrorColor, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        when {
                            newPassword.length < 6 -> showError = "密码长度不能少于6位"
                            newPassword != confirmPassword -> showError = "两次输入的密码不一致"
                            else -> onConfirm(newPassword)
                        }
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
                    Text("确认", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

/**
 * 设置区块卡片 — 与 SettingsScreen 同构的复用组件。
 */
@Composable
private fun SettingsSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RadiusLg, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column {
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
            content()
        }
    }
}

/** 设置区块内的分割线 */
@Composable
private fun SettingsDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
}
