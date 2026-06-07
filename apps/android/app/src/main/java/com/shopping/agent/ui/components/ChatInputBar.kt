package com.shopping.agent.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.shopping.agent.ui.theme.*
import com.shopping.agent.viewmodel.ChatViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 可复用聊天输入栏 — 主页/比价/探索页共用。
 *
 * 支持: 文本输入 + 拍照找货 + 语音(预留)
 */
@Composable
fun ChatInputBar(
    chatViewModel: ChatViewModel,
    onSendRequested: (() -> Unit)? = null,
    placeholder: String = "输入商品需求…",
    showIcons: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val uiState by chatViewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf(uiState.inputText) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showChooser by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    // 相册选择器
    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val tempFile = File(context.cacheDir, "gallery_${System.currentTimeMillis()}.jpg")
                inputStream?.use { inp ->
                    tempFile.outputStream().use { out -> inp.copyTo(out) }
                }
                chatViewModel.sendImage(tempFile)
            } catch (e: Exception) {
                android.util.Log.e("ChatInputBar", "Image save failed", e)
                android.widget.Toast.makeText(context, "图片处理失败，请重试", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 拍照
    var cameraUriToLaunch by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraUri?.let { uri ->
                selectedImageUri = uri
                try {
                    val tempFile = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(uri)?.use { inp ->
                        tempFile.outputStream().use { out -> inp.copyTo(out) }
                    }
                    chatViewModel.sendImage(tempFile)
                } catch (e: Exception) {
                    android.util.Log.e("ChatInputBar", "Camera image save failed", e)
                    android.widget.Toast.makeText(context, "拍照处理失败，请重试", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val launchTakePicture: (Uri) -> Unit = { uri ->
        cameraUri = uri
        cameraLauncher.launch(uri)
    }

    /** 相机权限请求 launcher — 用户授权后自动打开相机 */
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraUriToLaunch?.let { launchTakePicture(it) }
            cameraUriToLaunch = null
        } else {
            android.widget.Toast.makeText(context, "需要相机权限才能拍照", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // 语音识别
    // 语音输入：录音 → 豆包 API 音频理解 → RAG
    val voiceRecorder = remember { com.shopping.agent.data.local.VoiceRecorder(context) }
    var isRecording by remember { mutableStateOf(false) }
    var voiceVolume by remember { mutableStateOf(0f) }
    var recordStartTime by remember { mutableLongStateOf(0L) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    val voicePermLauncherRef = remember { mutableStateOf<androidx.activity.result.ActivityResultLauncher<String>?>(null) }

    if (isRecording) {
        LaunchedEffect(recordStartTime) {
            while (isRecording) {
                elapsedSeconds = ((System.currentTimeMillis() - recordStartTime) / 1000).toInt()
                kotlinx.coroutines.delay(500)
            }
        }
    }

    fun resetVoiceState() { isRecording = false; voiceVolume = 0f; elapsedSeconds = 0 }

    fun launchVoice() {
        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (!hasPerm) { voicePermLauncherRef.value?.launch(Manifest.permission.RECORD_AUDIO); return }
        voiceRecorder.start(object : com.shopping.agent.data.local.VoiceRecorder.RecordCallback {
            override fun onStart() { isRecording = true; recordStartTime = System.currentTimeMillis() }
            override fun onVolume(volume: Float) { voiceVolume = volume }
            override fun onResult(audioFile: java.io.File) { resetVoiceState(); chatViewModel.sendVoice(audioFile) }
            override fun onError(message: String) { resetVoiceState(); android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show() }
        })
    }

    val voicePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchVoice() else android.widget.Toast.makeText(context, "需要麦克风权限", android.widget.Toast.LENGTH_SHORT).show()
    }
    SideEffect { voicePermLauncherRef.value = voicePermissionLauncher }

    // 同步外部状态到本地
    if (uiState.inputText.isEmpty() && inputText.isNotEmpty()) {
        inputText = ""
    }

    Column {
        // 已选图片预览（拍照找货中）
        if (selectedImageUri != null && uiState.isStreaming) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "拍照找货",
                        modifier = Modifier.size(60.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        uiState.searchStatus.ifBlank { "📷 正在拍照找货…" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { selectedImageUri = null },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, "取消", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // 输入栏
        Surface(shadowElevation = 3.dp, color = MaterialTheme.colorScheme.surface, modifier = modifier) {
            Row(
                Modifier
                    .padding(horizontal = Dimens.space3, vertical = Dimens.space2)
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showIcons) {
                    IconButton(
                        onClick = { showChooser = true },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, "拍照找货", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { newText ->
                        inputText = newText
                        chatViewModel.onInputChange(newText)
                    },
                    enabled = !uiState.isStreaming,
                    placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    shape = RadiusFull,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.outlineVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.weight(1f),
                    maxLines = 4
                )
                // 语音输入按钮
                if (showIcons) {
                    IconButton(
                        onClick = {
                            if (isRecording) {
                                val f = voiceRecorder.stop(); isRecording = false
                                if (f != null && f.length() > 0) chatViewModel.sendVoice(f) else resetVoiceState()
                            } else { launchVoice() }
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.Mic, "语音输入", tint = if (isRecording) Primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                FilledIconButton(
                    onClick = {
                        chatViewModel.sendMessage()
                        onSendRequested?.invoke()
                    },
                    enabled = inputText.isNotBlank() && !uiState.isStreaming,
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Primary,
                        disabledContainerColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, "发送", tint = OnPrimary)
                }
            }
        }

        // 语音状态指示条（录音时显示）
        if (isRecording) {
            val animatedVolume by animateFloatAsState(voiceVolume, label = "voiceVol")
            Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), shape = RoundedCornerShape(12.dp), color = Primary.copy(alpha = 0.08f)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔴 正在录音 ${elapsedSeconds}s / 60s", style = MaterialTheme.typography.bodyMedium, color = Primary, modifier = Modifier.weight(1f))
                        Text("轻触麦克风停止", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))) {
                        Box(Modifier.fillMaxWidth(animatedVolume / 10f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Primary))
                    }
                }
            }
        }

        // 拍照/相册选择弹窗
        if (showChooser) {
            AlertDialog(
                onDismissRequest = { showChooser = false },
                icon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                title = { Text("拍照找货") },
                text = { Text("选择获取图片的方式") },
                confirmButton = {
                    Column {
                        // 拍照
                        OutlinedButton(
                            onClick = {
                                showChooser = false
                                val dateStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                val photoFile = File(context.cacheDir, "JPEG_${dateStamp}.jpg")
                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    photoFile
                                )
                                val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                if (hasPermission) {
                                    launchTakePicture(uri)
                                } else {
                                    cameraUriToLaunch = uri
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("拍照")
                        }
                        Spacer(Modifier.height(8.dp))
                        // 从相册选择
                        OutlinedButton(
                            onClick = {
                                showChooser = false
                                galleryPicker.launch("image/*")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("从相册选择")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showChooser = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}
