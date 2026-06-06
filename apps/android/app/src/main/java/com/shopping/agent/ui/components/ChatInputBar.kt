package com.shopping.agent.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.speech.RecognizerIntent
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    // 语音识别
    val speechRecognizer = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull() ?: return@rememberLauncherForActivityResult
            inputText = spoken
            chatViewModel.onInputChange(spoken)
        }
    }
    var pendingVoiceIntent by remember { mutableStateOf<Intent?>(null) }
    val voicePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingVoiceIntent?.let { intent ->
                try {
                    speechRecognizer.launch(intent)
                } catch (e: Exception) {
                    android.util.Log.e("ChatInputBar", "Speech recognizer launch failed", e)
                    android.widget.Toast.makeText(context, "语音输入启动失败，请稍后重试", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            android.widget.Toast.makeText(context, "需要麦克风权限才能使用语音输入", android.widget.Toast.LENGTH_SHORT).show()
        }
        pendingVoiceIntent = null
    }

    fun launchVoiceInput(intent: Intent) {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            try {
                speechRecognizer.launch(intent)
            } catch (e: Exception) {
                android.util.Log.e("ChatInputBar", "Speech recognizer launch failed", e)
                android.widget.Toast.makeText(context, "语音输入启动失败，请稍后重试", android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
            pendingVoiceIntent = intent
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
                    val hasSpeech = remember {
                        android.speech.SpeechRecognizer.isRecognitionAvailable(context)
                    }
                    IconButton(
                        onClick = {
                            if (hasSpeech) {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "说出你想买的商品...")
                                }
                                launchVoiceInput(intent)
                            } else {
                                android.widget.Toast.makeText(context, "语音识别不可用", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.Mic, "语音输入", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
