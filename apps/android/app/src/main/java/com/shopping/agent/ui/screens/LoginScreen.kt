package com.shopping.agent.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import kotlin.random.Random

/** 登录方式枚举 */
private enum class LoginTab(val label: String) {
    PHONE("手机号登录"),
    EMAIL("邮箱登录"),
}

/**
 * 登录页面 — 支持手机号/邮箱密码登录、验证码登录和游客登录。
 *
 * 功能：
 * - 手机号登录 / 邮箱登录 两个 Tab 切换
 * - 密码登录：输入账号 + 密码，点击登录校验
 * - 验证码登录：输入账号，获取验证码（模拟），输入验证码后登录
 * - 游客登录：直接进入主页面
 * - 密码可见性切换（按钮点击显示/隐藏，非长按）
 * - 忘记密码 / 注册账号 / 游客登录 链接
 *
 * @param onBack 返回按钮回调
 * @param onLoginSuccess 登录成功回调
 * @param onNavigateToRegister 跳转注册页面
 * @param onNavigateToForgotPassword 跳转忘记密码页面
 * @param onGuestLogin 游客登录回调
 */
@Composable
fun LoginScreen(
    onBack: () -> Unit = {},
    onLoginSuccess: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {},
    onNavigateToForgotPassword: () -> Unit = {},
    onGuestLogin: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { UserRepository(context) }

    var currentTab by remember { mutableStateOf(LoginTab.PHONE) }
    var account by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var verificationCode by remember { mutableStateOf("") }
    var generatedCode by remember { mutableStateOf("") }
    var showVerificationInput by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(0) }
    var isLoggingIn by remember { mutableStateOf(false) }
    /** 是否显示游客登录按钮 — 登录状态表中已是游客登录则不显示（避免游客→游客的死循环） */
    var showGuestLogin by remember { mutableStateOf(true) }

    // 检查 login_state 表：若已通过游客确认登录则隐藏游客登录入口
    LaunchedEffect(Unit) {
        showGuestLogin = try {
            val state = withContext(Dispatchers.IO) { repository.getLoginState() }
            // 仅当 login_state 记录为 "guest" 且已登录时隐藏
            !(state.loginType == "guest" && state.loginStatus)
        } catch (_: Exception) { true }
    }

    // 倒计时效果
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            kotlinx.coroutines.delay(1000L)
            countdown--
        }
    }

    /** 获取验证码（模拟：生成 6 位随机码并 Toast 显示） */
    fun requestVerificationCode() {
        if (account.isBlank()) {
            Toast.makeText(context, "请先输入${if (currentTab == LoginTab.PHONE) "手机号" else "邮箱"}", Toast.LENGTH_SHORT).show()
            return
        }
        val code = (100000..999999).random().toString()
        generatedCode = code
        showVerificationInput = true
        countdown = 60
        Toast.makeText(context, "验证码已发送：$code（模拟）", Toast.LENGTH_LONG).show()
    }

    /** 执行登录 */
    fun performLogin() {
        if (account.isBlank()) {
            Toast.makeText(context, "请输入${if (currentTab == LoginTab.PHONE) "手机号" else "邮箱"}", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            isLoggingIn = true
            try {
                val credentials = withContext(Dispatchers.IO) { repository.getCredentials() }

                if (showVerificationInput) {
                    // 验证码登录
                    if (verificationCode.isBlank()) {
                        Toast.makeText(context, "请输入验证码", Toast.LENGTH_SHORT).show()
                        isLoggingIn = false
                        return@launch
                    }
                    if (verificationCode != generatedCode) {
                        Toast.makeText(context, "验证码错误，请重新输入", Toast.LENGTH_SHORT).show()
                        isLoggingIn = false
                        return@launch
                    }
                    // 验证码正确 → 登录成功
                    val encryptedPwd = if (password.isNotEmpty()) {
                        withContext(Dispatchers.IO) { CryptoUtil.encrypt(password) }
                    } else ""
                    withContext(Dispatchers.IO) {
                        repository.saveCredentials(currentTab.name.lowercase(), encryptedPwd)
                        repository.markAsLoggedIn()
                        repository.saveLoginState("non_guest")
                    }
                    Toast.makeText(context, "登录成功", Toast.LENGTH_SHORT).show()
                    onLoginSuccess()
                } else {
                    // 密码登录
                    if (password.isBlank()) {
                        Toast.makeText(context, "请输入密码", Toast.LENGTH_SHORT).show()
                        isLoggingIn = false
                        return@launch
                    }
                    // 校验密码
                    if (credentials != null) {
                        val decrypted = withContext(Dispatchers.IO) {
                            CryptoUtil.decrypt(credentials["password"] ?: "")
                        }
                        if (decrypted != password) {
                            Toast.makeText(context, "密码错误", Toast.LENGTH_SHORT).show()
                            isLoggingIn = false
                            return@launch
                        }
                    } else {
                        // 首次登录（无历史凭证），允许任意密码通过并保存
                        val encryptedPwd = withContext(Dispatchers.IO) { CryptoUtil.encrypt(password) }
                        withContext(Dispatchers.IO) {
                            repository.saveCredentials(currentTab.name.lowercase(), encryptedPwd)
                        }
                    }
                    withContext(Dispatchers.IO) {
                        repository.markAsLoggedIn()
                        repository.saveLoginState("non_guest")
                    }
                    Toast.makeText(context, "登录成功", Toast.LENGTH_SHORT).show()
                    onLoginSuccess()
                }
            } catch (_: Exception) {
                Toast.makeText(context, "登录失败，请重试", Toast.LENGTH_SHORT).show()
            }
            isLoggingIn = false
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
            Text("登录", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(start = 4.dp))
        })

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Tab 切换：手机号 / 邮箱
            TabRow(
                selectedTabIndex = LoginTab.entries.indexOf(currentTab),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Primary,
                modifier = Modifier.fillMaxWidth(),
            ) {
                LoginTab.entries.forEach { tab ->
                    Tab(
                        selected = currentTab == tab,
                        onClick = {
                            currentTab = tab
                            account = ""
                            password = ""
                            verificationCode = ""
                            showVerificationInput = false
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
                label = { Text(if (currentTab == LoginTab.PHONE) "手机号" else "邮箱") },
                placeholder = { Text(if (currentTab == LoginTab.PHONE) "请输入手机号" else "请输入邮箱地址") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (currentTab == LoginTab.PHONE) KeyboardType.Phone else KeyboardType.Email,
                ),
                leadingIcon = {
                    Icon(
                        if (currentTab == LoginTab.PHONE) Icons.Default.Phone else Icons.Default.Email,
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

            // 密码输入框（非验证码模式）
            if (!showVerificationInput) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    placeholder = { Text("请输入密码") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    leadingIcon = {
                        Icon(Icons.Default.Lock, "密码",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
            }

            // 验证码输入（验证码模式）
            if (showVerificationInput) {
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
            }

            // 切换到验证码登录 / 密码登录
            TextButton(
                onClick = { showVerificationInput = !showVerificationInput },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(
                    if (showVerificationInput) "使用密码登录" else "使用验证码登录",
                    color = Primary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(Modifier.height(8.dp))

            // 登录按钮
            Button(
                onClick = { performLogin() },
                enabled = !isLoggingIn,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RadiusMd,
                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
            ) {
                if (isLoggingIn) {
                    CircularProgressIndicator(color = OnPrimary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("登录", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            }

            // 辅助链接行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = onNavigateToRegister) {
                    Text("注册账号", color = Primary, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onNavigateToForgotPassword) {
                    Text("忘记密码", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }

            // 游客登录 — 仅在登录状态表无游客记录时显示，避免"已是游客→强制登录→再选游客"的死循环
            if (showGuestLogin) {
                // 分割线
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                    Text(" 或 ", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                }

                // 游客登录按钮
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    repository.deleteCredentials()
                                    // 游客登录也记录登录状态，下次启动跳过登录页
                                    repository.saveLoginState("guest")
                                }
                            } catch (_: Exception) {}
                            onGuestLogin()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RadiusMd,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline)
                    ),
                ) {
                    Text("游客登录", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
