package com.shopping.agent.ui.components

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.shopping.agent.data.voice.VoiceRecorder
import com.shopping.agent.ui.theme.*
import com.shopping.agent.viewmodel.ChatViewModel
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatInputBar(
    chatViewModel: ChatViewModel,
    onSendRequested: (() -> Unit)? = null,
    placeholder: String = "输入商品需求...",
    showIcons: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val uiState by chatViewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf(uiState.inputText) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showChooser by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var cameraUriToLaunch by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val voiceRecorder = remember { VoiceRecorder() }
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableStateOf(0) }
    var amplitude by remember { mutableStateOf(0) }

    fun stopRecordingAndSend() {
        try {
            val result = voiceRecorder.stop()
            isRecording = false
            recordingSeconds = 0
            amplitude = 0
            chatViewModel.sendVoice(result.file, result.durationSec)
        } catch (e: Exception) {
            android.util.Log.e("ChatInputBar", "Audio recorder stop failed", e)
            voiceRecorder.cancel()
            isRecording = false
            android.widget.Toast.makeText(context, "录音时间太短，请重试", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(isRecording) {
        while (isRecording) {
            recordingSeconds = (voiceRecorder.durationMs / 1000L).toInt()
            amplitude = voiceRecorder.amplitude()
            if (recordingSeconds >= 60) {
                stopRecordingAndSend()
                break
            }
            delay(250)
        }
    }

    fun startRecording() {
        try {
            voiceRecorder.start(context)
            recordingSeconds = 0
            amplitude = 0
            isRecording = true
        } catch (e: Exception) {
            android.util.Log.e("ChatInputBar", "Audio recorder start failed", e)
            android.widget.Toast.makeText(context, "录音启动失败，请稍后重试", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val voicePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startRecording()
        else android.widget.Toast.makeText(context, "需要麦克风权限才能使用语音输入", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun onVoiceClick() {
        if (isRecording) {
            stopRecordingAndSend()
            return
        }
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) startRecording()
        else voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            try {
                val tempFile = File(context.cacheDir, "gallery_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(it)?.use { inp ->
                    tempFile.outputStream().use { out -> inp.copyTo(out) }
                }
                chatViewModel.sendImage(tempFile)
            } catch (e: Exception) {
                android.util.Log.e("ChatInputBar", "Image save failed", e)
                android.widget.Toast.makeText(context, "图片处理失败，请重试", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

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

    if (uiState.inputText.isEmpty() && inputText.isNotEmpty()) {
        inputText = ""
    }

    Column {
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
                        uiState.searchStatus.ifBlank { "正在拍照找货..." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { selectedImageUri = null }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, "取消", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        if (isRecording) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "● 正在录音 ${recordingSeconds.coerceAtLeast(0)}s / 60s",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Spacer(Modifier.width(12.dp))
                    LinearProgressIndicator(
                        progress = { (amplitude / 32767f).coerceIn(0f, 1f) },
                        modifier = Modifier.weight(1f).height(4.dp),
                    )
                }
            }
        }

        Surface(
            shadowElevation = 3.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = modifier.imePadding()
        ) {
            Row(
                Modifier
                    .padding(horizontal = Dimens.space3, vertical = Dimens.space2)
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showIcons) {
                    IconButton(onClick = { showChooser = true }, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.CameraAlt, "拍照找货", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { newText ->
                        inputText = newText
                        chatViewModel.onInputChange(newText)
                    },
                    enabled = !uiState.isStreaming && !isRecording,
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
                if (showIcons) {
                    IconButton(onClick = { onVoiceClick() }, modifier = Modifier.size(44.dp)) {
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
                    enabled = inputText.isNotBlank() && !uiState.isStreaming && !isRecording,
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

        if (showChooser) {
            AlertDialog(
                onDismissRequest = { showChooser = false },
                icon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                title = { Text("拍照找货") },
                text = { Text("选择获取图片的方式") },
                confirmButton = {
                    Column {
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
