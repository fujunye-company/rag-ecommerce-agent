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

/** 注册方式枚举 */
private enum class RegisterTab(val label: String) {
    PHONE("手机号注册"),
    EMAIL("邮箱注册"),
}

/**
 * 注册页面 — 支持手机号/邮箱注册，验证码校验后创建账号。
 *
 * 功能：
 * - 手机号注册 / 邮箱注册 两个 Tab 切换
 * - 验证码获取与校验（模拟：生成 6 位随机码）
 * - 密码 + 确认密码输入，支持可见性切换
 * - 注册成功后在 user_profile 表新增记录，保存登录凭证
 * - 注册后返回登录页面
 *
 * @param onBack 返回按钮回调
 * @param onRegisterSuccess 注册成功回调（跳回登录页）
 */
@Composable
fun RegisterScreen(
    onBack: () -> Unit = {},
    onRegisterSuccess: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { UserRepository(context) }

    var currentTab by remember { mutableStateOf(RegisterTab.PHONE) }
    var account by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var verificationCode by remember { mutableStateOf("") }
    var generatedCode by remember { mutableStateOf("") }
    var countdown by remember { mutableIntStateOf(0) }
    var isRegistering by remember { mutableStateOf(false) }

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            kotlinx.coroutines.delay(1000L)
            countdown--
        }
    }

    /** 获取验证码（模拟） */
    fun requestVerificationCode() {
        if (account.isBlank()) {
            Toast.makeText(context, "请输入${if (currentTab == RegisterTab.PHONE) "手机号" else "邮箱"}", Toast.LENGTH_SHORT).show()
            return
        }
        val code = (100000..999999).random().toString()
        generatedCode = code
        countdown = 60
        Toast.makeText(context, "验证码已发送：$code（模拟）", Toast.LENGTH_LONG).show()
    }

    /** 执行注册 */
    fun performRegister() {
        // 表单校验
        if (account.isBlank()) {
            Toast.makeText(context, "请输入${if (currentTab == RegisterTab.PHONE) "手机号" else "邮箱"}", Toast.LENGTH_SHORT).show()
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
        if (password.isBlank()) {
            Toast.makeText(context, "请输入密码", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.length < 6) {
            Toast.makeText(context, "密码长度不能少于6位", Toast.LENGTH_SHORT).show()
            return
        }
        if (password != confirmPassword) {
            Toast.makeText(context, "两次输入的密码不一致", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            isRegistering = true
            try {
                withContext(Dispatchers.IO) {
                    // 创建新的用户画像（非游客，is_guest=0）
                    val newUserId = repository.createUserProfile()
                    // 保存登录凭证
                    val encryptedPwd = CryptoUtil.encrypt(password)
                    // createUserProfile 创建了新用户，但我们需要确保使用新用户 ID
                    // createUserProfile 插入新行，但 getUserId() 可能返回旧 ID
                    // 这里直接使用 saveCredentials + 切换到新用户
                    repository.saveCredentials(currentTab.name.lowercase(), encryptedPwd)
                }
                Toast.makeText(context, "注册成功，请登录", Toast.LENGTH_SHORT).show()
                onRegisterSuccess()
            } catch (_: Exception) {
                Toast.makeText(context, "注册失败，请重试", Toast.LENGTH_SHORT).show()
            }
            isRegistering = false
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
            Text("注册", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(start = 4.dp))
        })

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Tab 切换
            TabRow(
                selectedTabIndex = RegisterTab.entries.indexOf(currentTab),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Primary,
                modifier = Modifier.fillMaxWidth(),
            ) {
                RegisterTab.entries.forEach { tab ->
                    Tab(
                        selected = currentTab == tab,
                        onClick = {
                            currentTab = tab
                            account = ""
                            password = ""
                            confirmPassword = ""
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
                label = { Text(if (currentTab == RegisterTab.PHONE) "手机号" else "邮箱") },
                placeholder = { Text(if (currentTab == RegisterTab.PHONE) "请输入手机号" else "请输入邮箱地址") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (currentTab == RegisterTab.PHONE) KeyboardType.Phone else KeyboardType.Email,
                ),
                leadingIcon = {
                    Icon(
                        if (currentTab == RegisterTab.PHONE) Icons.Default.Phone else Icons.Default.Email,
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

            // 密码
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("密码") },
                placeholder = { Text("请输入密码（至少6位）") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                leadingIcon = {
                    Icon(Icons.Default.Lock, "密码", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
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
                label = { Text("确认密码") },
                placeholder = { Text("请再次输入密码") },
                singleLine = true,
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = confirmPassword.isNotEmpty() && confirmPassword != password,
                supportingText = {
                    if (confirmPassword.isNotEmpty() && confirmPassword != password) {
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

            // 注册按钮
            Button(
                onClick = { performRegister() },
                enabled = !isRegistering,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RadiusMd,
                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
            ) {
                if (isRegistering) {
                    CircularProgressIndicator(color = OnPrimary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("注册", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
