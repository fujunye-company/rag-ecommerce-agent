package com.shopping.agent.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.shopping.agent.data.remote.AudioClient
import com.shopping.agent.ui.theme.*
import com.shopping.agent.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()
    val audioClient = remember { AudioClient() }

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
        try {
            cameraUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            android.util.Log.e("ChatInputBar", "Camera launch failed", e)
            cameraUri = null
            android.widget.Toast.makeText(context, "拍照搜索启动失败，请稍后重试", android.widget.Toast.LENGTH_SHORT).show()
        }
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

    // 本地后端 ASR 录音
    var isRecording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }

    fun stopAndTranscribe() {
        val activeRecorder = recorder ?: return
        val audioFile = recordingFile ?: return
        try {
            activeRecorder.stop()
            activeRecorder.release()
        } catch (e: Exception) {
            android.util.Log.e("ChatInputBar", "Audio recorder stop failed", e)
            try {
                activeRecorder.release()
            } catch (_: Exception) {
            }
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
                    chatViewModel.onInputChange(text)
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatInputBar", "Local ASR failed", e)
                android.widget.Toast.makeText(context, "本地语音识别失败，请检查后端服务", android.widget.Toast.LENGTH_SHORT).show()
            } finally {
                audioFile.delete()
            }
        }
    }

    fun startRecording() {
        try {
            val audioFile = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
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
            android.util.Log.e("ChatInputBar", "Audio recorder start failed", e)
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
                        onClick = { onVoiceClick() },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            if (isRecording) "结束录音" else "语音输入",
                            tint = if (isRecording) Primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                                photoFile.createNewFile()
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
