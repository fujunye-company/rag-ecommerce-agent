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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.shopping.agent.data.local.CryptoUtil
import com.shopping.agent.data.local.UserRepository
import com.shopping.agent.ui.components.GradientTopBar
import com.shopping.agent.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 密码重置页面 — 验证通过后在此设置新密码。
 *
 * 功能：
 * - 新密码输入框（支持可见性切换）
 * - 确认密码输入框（支持可见性切换）
 * - 校验：两次密码一致、与旧密码不同
 * - 重置成功后更新数据库中的加密密码，返回登录页面
 *
 * @param onBack 返回按钮回调
 * @param onResetSuccess 密码重置成功回调（返回登录页）
 * @param account 重置密码的账号（手机号/邮箱），用于关联凭证
 */
@Composable
fun PasswordResetScreen(
    onBack: () -> Unit = {},
    onResetSuccess: () -> Unit = {},
    account: String = "",
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { UserRepository(context) }

    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isResetting by remember { mutableStateOf(false) }

    /** 执行密码重置 */
    fun performReset() {
        if (newPassword.isBlank()) {
            Toast.makeText(context, "请输入新密码", Toast.LENGTH_SHORT).show()
            return
        }
        if (newPassword.length < 6) {
            Toast.makeText(context, "密码长度不能少于6位", Toast.LENGTH_SHORT).show()
            return
        }
        if (newPassword != confirmPassword) {
            Toast.makeText(context, "两次输入的密码不一致", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            isResetting = true
            try {
                val credentials = withContext(Dispatchers.IO) { repository.getCredentials() }
                if (credentials != null) {
                    // 检查新密码是否与旧密码相同
                    val oldDecrypted = withContext(Dispatchers.IO) {
                        CryptoUtil.decrypt(credentials["password"] ?: "")
                    }
                    if (newPassword == oldDecrypted) {
                        Toast.makeText(context, "新密码不能与旧密码相同", Toast.LENGTH_SHORT).show()
                        isResetting = false
                        return@launch
                    }
                }

                // 加密新密码并保存
                val encrypted = withContext(Dispatchers.IO) { CryptoUtil.encrypt(newPassword) }
                val loginMethod = credentials?.get("login_method") ?: "phone"
                withContext(Dispatchers.IO) {
                    repository.saveCredentials(loginMethod, encrypted)
                    repository.markAsLoggedIn()
                }
                Toast.makeText(context, "密码重置成功，请登录", Toast.LENGTH_SHORT).show()
                onResetSuccess()
            } catch (_: Exception) {
                Toast.makeText(context, "密码重置失败，请重试", Toast.LENGTH_SHORT).show()
            }
            isResetting = false
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
            Text("密码重置", style = MaterialTheme.typography.titleMedium,
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
                "请输入新的登录密码。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // 新密码
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("新密码") },
                placeholder = { Text("请输入新密码（至少6位）") },
                singleLine = true,
                visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                leadingIcon = {
                    Icon(Icons.Default.Lock, "新密码", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                        Icon(
                            if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (newPasswordVisible) "隐藏密码" else "显示密码",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RadiusMd,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
            )

            // 确认密码
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("确认新密码") },
                placeholder = { Text("请再次输入新密码") },
                singleLine = true,
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = confirmPassword.isNotEmpty() && confirmPassword != newPassword,
                supportingText = {
                    if (confirmPassword.isNotEmpty() && confirmPassword != newPassword) {
                        Text("两次输入的密码不一致", color = ErrorColor)
                    }
                },
                leadingIcon = {
                    Icon(Icons.Default.Lock, "确认密码", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (confirmPasswordVisible) "隐藏密码" else "显示密码",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RadiusMd,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
            )

            Spacer(Modifier.height(8.dp))

            // 重置按钮
            Button(
                onClick = { performReset() },
                enabled = !isResetting,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RadiusMd,
                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
            ) {
                if (isResetting) {
                    CircularProgressIndicator(color = OnPrimary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("重置密码", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
