package com.shopping.agent.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.shopping.agent.data.local.UserRepository
import com.shopping.agent.data.remote.AudioClient
import com.shopping.agent.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/** 客服消息本地状态模型 */
private data class CsMessage(
    val id: Long = 0,
    val role: String,
    val content: String,
    val createdAt: Long,
    val isNew: Boolean = false,
)

private val CS_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINESE)

/** "猜你想问"默认问题池，15 条缓存，避免每次刷新都查数据库 */
private val DEFAULT_QUESTIONS = listOf(
    "如何查询订单物流信息？",
    "商品质量问题如何申请售后？",
    "优惠券使用规则是什么？",
    "如何修改收货地址？",
    "支付失败怎么办？",
    "如何取消订单？",
    "商品降价了能退差价吗？",
    "发货时间一般是多久？",
    "如何联系人工客服？",
    "积分如何使用？",
    "如何申请退货退款？",
    "商品与描述不符怎么办？",
    "如何查看历史订单？",
    "会员权益有哪些？",
    "客服工作时间是几点？",
)

private val QUESTION_COLORS = listOf(
    Color(0xFFFF6B6B), Color(0xFFFFB347), Color(0xFF4A90D9),
)

/**
 * 拾物客服页面 — 每次进入视为新对话。
 *
 * 布局结构:
 *   [历史时间戳] → 问候语 → 历史消息(来自DB, 无逐条时间戳)
 *   —— 以下为新消息 ——（仅当有历史时）
 *   [当前时间戳] → 问候语 → 本轮新消息(无逐条时间戳) → 猜你想问
 *
 * - 系统问候语不存入数据库，仅 UI 渲染
 * - 每条对话只记录一个开始时间戳，逐条回复不再显示时间
 * - 历史对话保留在 SQLite，不删除
 * - "猜你想问"从 15 题缓存池中每次取 3 道，可刷新
 */
@Composable
fun CustomerServiceScreen(
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { UserRepository(context) }

    var messages by remember { mutableStateOf<List<CsMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var questionStartIndex by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var lastLoadedId by remember { mutableLongStateOf(0L) }
    val audioClient = remember { AudioClient() }
    var isRecording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }

    val sessionStartTime = remember { System.currentTimeMillis() }
    val sessionStartLabel = remember { CS_DATE_FORMAT.format(Date(sessionStartTime)) }

    val scrollState = rememberScrollState()

    val displayedQuestions = remember(questionStartIndex) {
        DEFAULT_QUESTIONS.drop(questionStartIndex % DEFAULT_QUESTIONS.size)
            .take(3)
            .ifEmpty { DEFAULT_QUESTIONS.take(3) }
    }

    fun onSendMessage(text: String, onUpdated: (List<CsMessage>) -> Unit) {
        val now = System.currentTimeMillis()
        val combined = messages.toMutableList()

        val userMsg = CsMessage(role = "User", content = text, createdAt = now, isNew = true)
        combined.add(userMsg)
        onUpdated(combined)

        scope.launch(Dispatchers.IO) {
            try {
                repository.saveCustomerServiceMessage("user", text)
            } catch (_: Exception) {}
        }

        scope.launch {
            kotlinx.coroutines.delay(800)
            val replyTime = System.currentTimeMillis()
            val reply = generateCsReply(text)
            val replyMsg = CsMessage(role = "Assistant", content = reply, createdAt = replyTime, isNew = true)
            val withReply = combined.toMutableList().also { it.add(replyMsg) }
            onUpdated(withReply)

            scope.launch(Dispatchers.IO) {
                try {
                    repository.saveCustomerServiceMessage("assistant", reply)
                } catch (_: Exception) {}
            }
        }
    }

    fun stopAndTranscribe() {
        val activeRecorder = recorder ?: return
        val audioFile = recordingFile ?: return
        try {
            activeRecorder.stop()
            activeRecorder.release()
        } catch (e: Exception) {
            android.util.Log.e("CustomerServiceScreen", "Audio recorder stop failed", e)
            try { activeRecorder.release() } catch (_: Exception) {}
            android.widget.Toast.makeText(context, "录音时间太短，请重试", android.widget.Toast.LENGTH_SHORT).show()
            recorder = null
            recordingFile = null
            isRecording = false
            return
        }
        recorder = null
        recordingFile = null
        isRecording = false
        android.widget.Toast.makeText(context, "正在识别语音…", android.widget.Toast.LENGTH_SHORT).show()
        scope.launch {
            try {
                val text = audioClient.transcribe(audioFile)
                if (text.isBlank()) {
                    android.widget.Toast.makeText(context, "没有识别到语音内容", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    inputText = text
                }
            } catch (e: Exception) {
                android.util.Log.e("CustomerServiceScreen", "Local ASR failed", e)
                android.widget.Toast.makeText(context, "本地语音识别失败，请检查后端服务", android.widget.Toast.LENGTH_SHORT).show()
            } finally {
                audioFile.delete()
            }
        }
    }

    fun startRecording() {
        try {
            val audioFile = File(context.cacheDir, "cs_voice_${System.currentTimeMillis()}.m4a")
            val newRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16000)
                setAudioEncodingBitRate(64000)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }
            recordingFile = audioFile
            recorder = newRecorder
            isRecording = true
            android.widget.Toast.makeText(context, "正在录音，再点一次结束", android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.util.Log.e("CustomerServiceScreen", "Audio recorder start failed", e)
            android.widget.Toast.makeText(context, "录音启动失败，请稍后重试", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val voicePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startRecording()
        } else {
            android.widget.Toast.makeText(context, "需要麦克风权限才能使用语音输入", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun onVoiceClick() {
        if (isRecording) {
            stopAndTranscribe()
            return
        }
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            startRecording()
        } else {
            voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(Unit) {
        try {
            val existing = withContext(Dispatchers.IO) { repository.getCustomerServiceMessages() }
            lastLoadedId = existing.lastOrNull()?.id ?: 0L
            messages = existing.map { msg ->
                CsMessage(
                    id = msg.id,
                    role = msg.role,
                    content = msg.content,
                    createdAt = msg.createdAt,
                    isNew = false,
                )
            }
        } catch (_: Exception) {}
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding(),
    ) {
        CsTopBar(onBack = onBack)

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Box(modifier = Modifier.weight(1f)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp),
                ) {
                    Spacer(Modifier.height(12.dp))

                    val oldMessages = messages.filter { !it.isNew }
                    val newMessages = messages.filter { it.isNew }

                    if (oldMessages.isNotEmpty()) {
                        val historyTimestamp = CS_DATE_FORMAT.format(Date(oldMessages.first().createdAt))
                        CsSessionHeader(timestampText = historyTimestamp)
                        Spacer(Modifier.height(4.dp))
                        oldMessages.forEach { msg ->
                            CsMessageBubble(msg)
                        }
                    }

                    if (oldMessages.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        CsNewMessageDivider()
                        Spacer(Modifier.height(8.dp))
                    }

                    CsSessionHeader(timestampText = sessionStartLabel)

                    if (newMessages.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        newMessages.forEach { msg ->
                            CsMessageBubble(msg)
                        }
                        Spacer(Modifier.height(12.dp))
                    } else {
                        Spacer(Modifier.height(8.dp))
                    }

                    CsGuessQuestions(
                        questions = displayedQuestions,
                        onRefresh = { questionStartIndex = (questionStartIndex + 3) % DEFAULT_QUESTIONS.size },
                        onQuestionClick = { question ->
                            onSendMessage(question) { newMessages ->
                                messages = newMessages
                            }
                        },
                    )

                    Spacer(Modifier.height(80.dp))
                }

                LaunchedEffect(messages.size) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        CsInputBar(
            inputText = inputText,
            onInputChange = { inputText = it },
            onSend = {
                if (inputText.isNotBlank()) {
                    val text = inputText.trim()
                    inputText = ""
                    onSendMessage(text) { newMessages ->
                        messages = newMessages
                    }
                }
            },
            onVoiceClick = { onVoiceClick() },
            isRecording = isRecording,
        )
    }
}

@Composable
private fun CsTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack, "返回",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            "拾物客服",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun CsSessionHeader(timestampText: String) {
    Text(
        text = timestampText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(12.dp))
    CsBubbleLeft("您好，智能客服助手为您服务！")
    Spacer(Modifier.height(8.dp))
    CsBubbleLeft("晚上好~我一直都在，有问题可以随时咨询我哦~")
}

@Composable
private fun CsMessageBubble(msg: CsMessage) {
    if (msg.role == "User" || msg.role == "user") {
        CsBubbleRight(msg.content)
    } else {
        CsBubbleLeft(msg.content)
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun CsBubbleLeft(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(
            shape = RadiusMd,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun CsBubbleRight(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            shape = RadiusMd,
            color = PrimaryLight,
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun CsNewMessageDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            "—— 以下为新消息 ——",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun CsGuessQuestions(
    questions: List<String>,
    onRefresh: () -> Unit,
    onQuestionClick: (String) -> Unit,
) {
    Card(
        shape = RadiusLg,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "猜你想问",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = onRefresh) {
                    Text("换一批问题", style = MaterialTheme.typography.bodySmall, color = Primary)
                    Spacer(Modifier.width(4.dp))
                    Text("🔄", fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            questions.forEachIndexed { index, question ->
                val color = QUESTION_COLORS[index % QUESTION_COLORS.size]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onQuestionClick(question) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.width(24.dp),
                    )
                    Text(
                        text = question,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        "进入",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
                if (index < questions.size - 1) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun CsInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceClick: () -> Unit,
    isRecording: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = {},
                    shape = RadiusMd,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant),
                    ),
                ) {
                    Text("📦", fontSize = 14.sp)
                    Spacer(Modifier.width(4.dp))
                    Text("发送订单", style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onVoiceClick,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.Default.Mic, "语音",
                        tint = if (isRecording) Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    placeholder = {
                        Text("请输入...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    singleLine = true,
                    shape = RadiusFull,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    ),
                    modifier = Modifier.weight(1f).height(52.dp),
                    trailingIcon = {
                        Text("😉", fontSize = 18.sp, modifier = Modifier.padding(end = 4.dp))
                    },
                )

                Spacer(Modifier.width(6.dp))

                IconButton(onClick = {}, modifier = Modifier.size(40.dp)) {
                    Text("📦", fontSize = 20.sp)
                }

                if (inputText.isNotBlank()) {
                    FilledIconButton(
                        onClick = onSend,
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Primary,
                        ),
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send, "发送",
                            tint = OnPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                } else {
                    IconButton(onClick = {}, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Default.Add, "更多",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

private fun generateCsReply(question: String): String {
    val keywords = mapOf(
        "物流" to "您可以通过「我的订单」页面查看物流详情，通常发货后1-3天内会更新物流信息哦~",
        "售后" to "若商品存在质量问题，请在订单详情页点击「申请售后」，上传相关凭证后客服会在24小时内处理。",
        "优惠券" to "优惠券可在结算时自动抵扣，部分优惠券有使用门槛，具体规则可在领券页面查看。",
        "地址" to "您可以在「个人信息-收货地址」中修改或新增收货地址，保存后立即生效。",
        "支付" to "支付失败可能是网络问题或银行卡限额，建议切换支付方式或稍后重试。如有疑问请联系客服。",
        "取消" to "未发货的订单可直接在订单详情中取消，已发货的订单需先申请退款退货。",
        "降价" to "商品签收7天内若发生降价，可联系客服申请差价补偿，审核通过后将原路退还差价。",
        "发货" to "正常情况下下单后24-48小时内发货，法定节假日可能会有延迟，请您耐心等待~",
        "人工" to "您可以直接输入具体问题，客服将尽快为您解答。如需转人工，请输入「转人工」。",
        "积分" to "积分可用于兑换优惠券和参与会员活动，购物每消费1元可获得1积分哦~",
        "退货" to "在订单详情页点击「申请退货」，选择退货原因并提交，审核通过后按指引寄回商品即可。",
        "不符" to "如收到的商品与描述不符，请拍照留证并联系客服，我们将尽快为您处理退换货。",
        "历史订单" to "进入「我的订单」页面，点击上方筛选条件即可查看所有历史订单记录。",
        "会员" to "拾物会员享有专属折扣、优先客服、免费退货等多项权益，积分达标即可升级会员等级~",
    )

    for ((keyword, reply) in keywords) {
        if (question.contains(keyword)) return reply
    }

    val defaultReplies = listOf(
        "好的，已收到您的问题，客服正在处理中，请稍后~",
        "感谢您的咨询，我们会尽快回复您的问题，请耐心等待~",
        "您好，关于这个问题，建议您提供更多详细信息以便我们更快为您解决~",
        "明白了，我会把这个问题记录下来，后续有进展会通知您~",
    )
    return defaultReplies.random()
}
