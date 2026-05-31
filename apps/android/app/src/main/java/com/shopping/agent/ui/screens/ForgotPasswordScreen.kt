package com.shopping.agent.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.shopping.agent.data.local.UserRepository
import com.shopping.agent.ui.components.GradientTopBar
import com.shopping.agent.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 找回方式枚举 */
private enum class RecoveryTab(val label: String) {
    PHONE("手机号找回"),
    EMAIL("邮箱找回"),
}

/**
 * 忘记密码页面 — 支持手机号/邮箱找回，验证码校验后跳转密码重置。
 *
 * 功能：
 * - 手机号找回 / 邮箱找回 两个 Tab 切换
 * - 账号输入 + 验证码获取（模拟 6 位随机码）
 * - 验证码校验通过后跳转到密码重置页面
 *
 * @param onBack 返回按钮回调
 * @param onNavigateToPasswordReset 跳转密码重置页面（携带账号信息）
 */
@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit = {},
    onNavigateToPasswordReset: (account: String) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { UserRepository(context) }

    var currentTab by remember { mutableStateOf(RecoveryTab.PHONE) }
    var account by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var generatedCode by remember { mutableStateOf("") }
    var countdown by remember { mutableIntStateOf(0) }

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            kotlinx.coroutines.delay(1000L)
            countdown--
        }
    }

    /** 获取验证码（模拟） */
    fun requestVerificationCode() {
        if (account.isBlank()) {
            Toast.makeText(context, "账号不能为空", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            try {
                // 检查该账号是否在数据库中存在凭证记录
                val credentials = withContext(Dispatchers.IO) { repository.getCredentials() }
                if (credentials == null) {
                    // 数据库中无凭证，但允许继续（模拟环境可接受任意账号）
                    Toast.makeText(context, "该账号未注册，请先注册", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val code = (100000..999999).random().toString()
                generatedCode = code
                countdown = 60
                Toast.makeText(context, "验证码已发送：$code（模拟）", Toast.LENGTH_LONG).show()
            } catch (_: Exception) {
                Toast.makeText(context, "发送验证码失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** 验证并跳转密码重置 */
    fun verifyAndProceed() {
        if (account.isBlank()) {
            Toast.makeText(context, "请输入${if (currentTab == RecoveryTab.PHONE) "手机号" else "邮箱"}", Toast.LENGTH_SHORT).show()
            return
        }
        if (verificationCode.isBlank()) {
            Toast.makeText(context, "请输入验证码", Toast.LENGTH_SHORT).show()
            return
        }
        if (verificationCode != generatedCode) {
            Toast.makeText(context, "验证码错误", Toast.LENGTH_SHORT).show()
            return
        }
        // 验证通过，跳转密码重置页
        onNavigateToPasswordReset(account)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        GradientTopBar(icons = {
            IconButton(onClick = onBack, modifier = Modifier.size(34.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回",
                    tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(26.dp))
            }
            Text("忘记密码", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(start = 4.dp))
        })

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 提示文本
            Text(
                "请选择找回方式并输入账号，获取验证码后即可重置密码。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Tab 切换
            TabRow(
                selectedTabIndex = RecoveryTab.entries.indexOf(currentTab),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Primary,
                modifier = Modifier.fillMaxWidth(),
            ) {
                RecoveryTab.entries.forEach { tab ->
                    Tab(
                        selected = currentTab == tab,
                        onClick = {
                            currentTab = tab
                            account = ""
                            verificationCode = ""
                            generatedCode = ""
                        },
                        text = { Text(tab.label) },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // 账号输入框
            OutlinedTextField(
                value = account,
                onValueChange = { account = it },
                label = { Text(if (currentTab == RecoveryTab.PHONE) "手机号" else "邮箱") },
                placeholder = { Text(if (currentTab == RecoveryTab.PHONE) "请输入已注册的手机号" else "请输入已注册的邮箱") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (currentTab == RecoveryTab.PHONE) KeyboardType.Phone else KeyboardType.Email,
                ),
                leadingIcon = {
                    Icon(
                        if (currentTab == RecoveryTab.PHONE) Icons.Default.Phone else Icons.Default.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RadiusMd,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
            )

            // 验证码
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = verificationCode,
                    onValueChange = { verificationCode = it },
                    label = { Text("验证码") },
                    placeholder = { Text("请输入验证码") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RadiusMd,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                )
                Button(
                    onClick = { requestVerificationCode() },
                    enabled = countdown == 0,
                    modifier = Modifier.height(56.dp),
                    shape = RadiusMd,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        contentColor = OnPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.outlineVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    Text(
                        if (countdown == 0) "获取验证码" else "${countdown}s",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // 下一步按钮
            Button(
                onClick = { verifyAndProceed() },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RadiusMd,
                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
            ) {
                Text("下一步", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}
