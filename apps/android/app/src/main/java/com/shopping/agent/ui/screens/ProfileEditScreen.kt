package com.shopping.agent.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.Gravity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.shopping.agent.data.local.UserRepository
import com.shopping.agent.ui.components.GradientTopBar
import com.shopping.agent.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/** 头像图片最大尺寸（像素），超过此尺寸将被降采样压缩 */
private const val MAX_AVATAR_DIM = 480
/** 头像 JPEG 压缩质量（0-100），越低文件越小 */
private const val AVATAR_JPEG_QUALITY = 80

/**
 * 在屏幕中央显示 Toast 提示。
 * @param context 上下文
 * @param message 提示文本
 * @param duration 显示时长，默认短时
 */
private fun showCenteredToast(context: android.content.Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
    val toast = Toast.makeText(context, message, duration)
    toast.setGravity(Gravity.CENTER, 0, 0)
    toast.show()
}

/**
 * 计算 Bitmap 降采样的比例因子，使解码后图片不超过目标尺寸。
 * 返回值总是 2 的幂（1, 2, 4, 8...），符合 `BitmapFactory.Options.inSampleSize` 的规范。
 */
private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height, width) = options.outHeight to options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

/**
 * 从 Content URI 加载图片并降采样解码，限制最大尺寸为 [maxDim]。
 * 用于相册选图和相机拍照文件读取，避免加载超大图片导致 OOM。
 *
 * 先将整个文件读入 ByteArray，再通过 [decodeSampledBitmapFromBytes] 两次解码：
 * 第一次仅读取元数据获取尺寸，第二次按计算出的采样率解码为 Bitmap。
 * ContentResolver 的 InputStream 可能不支持 seek，因此采用 copy-to-bytes 方式保证可靠性。
 */
private fun decodeSampledBitmapFromUri(context: android.content.Context, uri: Uri, maxDim: Int): Bitmap? {
    return try {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        decodeSampledBitmapFromBytes(bytes, maxDim)
    } catch (_: Exception) {
        null
    }
}

/**
 * 从字节数组降采样解码 Bitmap，限制最大尺寸为 [maxDim]。
 * 用于从 SQLite BLOB 读取头像时避免 OOM。
 */
private fun decodeSampledBitmapFromBytes(bytes: ByteArray, maxDim: Int): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        options.inSampleSize = calculateInSampleSize(options, maxDim, maxDim)
        options.inJustDecodeBounds = false
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    } catch (_: Exception) {
        null
    }
}

/**
 * 将 Bitmap 等比缩放到不超过 [maxDim] 的尺寸，用于存入数据库前压缩图片。
 * 若原始尺寸已在限制内则直接返回原图。
 */
private fun compressBitmapToMaxDim(source: Bitmap, maxDim: Int): Bitmap {
    val width = source.width
    val height = source.height
    if (width <= maxDim && height <= maxDim) return source
    val scale = maxDim.toFloat() / maxOf(width, height)
    val newW = (width * scale).toInt()
    val newH = (height * scale).toInt()
    return Bitmap.createScaledBitmap(source, newW, newH, true)
}

/**
 * 个人信息编辑页面 — 用户可在此页面查看和修改头像、昵称、性别、年龄段。
 *
 * 功能模块：
 * - 头像：圆形显示，点击弹出来源选择器（拍照/相册），选择后进入裁剪界面
 * - 昵称：点击弹出编辑弹窗，限制 1-30 个字符，确认时校验
 * - 拾物号：展示用户唯一标识（"sw" UUID），不可修改
 * - 性别：RadioButton 选择器（男/女）
 * - 年龄段：RadioButton 选择器（0-9 ~ 100及以上），选项列表在 25% 高滑动容器内
 *
 * 头像在组件内部自行从 SQLite 加载（与 ProfileScreen 同构），不依赖父级传入。
 *
 * @param onBack 返回按钮回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { UserRepository(context) }

    var profile by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var avatarBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var showAvatarSourceSheet by remember { mutableStateOf(false) }
    var showNicknameSheet by remember { mutableStateOf(false) }
    var showGenderSheet by remember { mutableStateOf(false) }
    var showAgeRangeSheet by remember { mutableStateOf(false) }
    var showCropScreen by remember { mutableStateOf(false) }
    var cropSourceBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = cameraUri
        if (success && uri != null) {
            try {
                val bitmap = decodeSampledBitmapFromUri(context, uri, MAX_AVATAR_DIM)
                if (bitmap != null) {
                    cropSourceBitmap = bitmap
                    showCropScreen = true
                }
            } catch (_: Exception) {}
            try { uri.path?.let { File(it).delete() } } catch (_: Exception) {}
        }
        cameraUri = null
    }

    /** 打开系统相机：创建临时文件 → FileProvider URI → launch */
    fun openCamera(
        ctx: android.content.Context,
        launcher: androidx.activity.result.ActivityResultLauncher<Uri>,
        onUriReady: (Uri) -> Unit,
    ) {
        try {
            ctx.cacheDir.mkdirs()
            val file = File(ctx.cacheDir, "avatar_capture_${System.currentTimeMillis()}.jpg")
            file.createNewFile()
            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
            onUriReady(uri)
            launcher.launch(uri)
        } catch (_: Exception) {
            showCenteredToast(ctx, "无法打开相机")
        }
    }

    /** 相机权限请求 launcher — 用户授权后自动打开相机 */
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            openCamera(context, cameraLauncher) { uri -> cameraUri = uri }
        } else {
            showCenteredToast(context, "需要相机权限才能拍照")
        }
    }

    // 加载用户画像和头像（与 ProfileScreen 同构：DB 读取均在 IO 线程，异常安全）
    LaunchedEffect(Unit) {
        try {
            val p = withContext(Dispatchers.IO) { repository.getUserProfile() }
            profile = p
        } catch (_: Exception) {
            profile = emptyMap()
        }
        try {
            val avatarBytes = withContext(Dispatchers.IO) { repository.getUserAvatar() }
            if (avatarBytes != null && avatarBytes.isNotEmpty()) {
                val androidBmp = withContext(Dispatchers.IO) {
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(avatarBytes, 0, avatarBytes.size, opts)
                    opts.inSampleSize = calculateInSampleSize(opts, MAX_AVATAR_DIM, MAX_AVATAR_DIM)
                    opts.inJustDecodeBounds = false
                    opts.inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                    BitmapFactory.decodeByteArray(avatarBytes, 0, avatarBytes.size, opts)
                }
                if (androidBmp != null) {
                    avatarBitmap = androidBmp.asImageBitmap()
                }
            }
        } catch (_: Exception) {
            avatarBitmap = null
        }
    }

    /** 从数据库重新加载用户画像，必须在 Main 线程调用（内部自动切 IO） */
    fun refreshProfile() {
        scope.launch {
            try {
                val p = withContext(Dispatchers.IO) { repository.getUserProfile() }
                profile = p
            } catch (_: Exception) {
                // 加载失败时保留现有数据
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = decodeSampledBitmapFromUri(context, uri, MAX_AVATAR_DIM)
                if (bitmap != null) {
                    cropSourceBitmap = bitmap
                    showCropScreen = true
                } else {
                    showCenteredToast(context, "图片加载失败，请重试")
                }
            } catch (_: Exception) {
                showCenteredToast(context, "图片加载失败，请重试")
            }
        }
    }

    if (showCropScreen && cropSourceBitmap != null) {
        ImageCropScreen(
            sourceBitmap = cropSourceBitmap!!,
            onCancel = {
                showCropScreen = false
                cropSourceBitmap = null
            },
            onConfirm = { croppedBitmap ->
                scope.launch(Dispatchers.IO) {
                    try {
                        val compressed = compressBitmapToMaxDim(croppedBitmap, MAX_AVATAR_DIM)
                        val stream = ByteArrayOutputStream()
                        compressed.compress(Bitmap.CompressFormat.JPEG, AVATAR_JPEG_QUALITY, stream)
                        val bytes = stream.toByteArray()
                        stream.close()
                        repository.updateUserAvatar(bytes)
                        // 保存完成后切回主线程：关闭裁剪界面 + 刷新头像显示
                        withContext(Dispatchers.Main) {
                            showCropScreen = false
                            cropSourceBitmap = null
                            avatarBitmap = compressed.asImageBitmap()
                            showCenteredToast(context, "头像已更新")
                        }
                    } catch (_: Exception) {
                        withContext(Dispatchers.Main) {
                            showCropScreen = false
                            cropSourceBitmap = null
                            showCenteredToast(context, "头像保存失败")
                        }
                    }
                }
            }
        )
        return
    }

    val userId = profile["id"] ?: ""
    val nickname = profile["nickname"] ?: ""
    val gender = profile["gender"] ?: ""
    val ageRange = profile["age_range"] ?: ""

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        GradientTopBar(icons = {
            IconButton(onClick = onBack, modifier = Modifier.size(34.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(26.dp))
            }
            Text("个人信息", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(start = 4.dp))
        })

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier.size(96.dp).clickable { showAvatarSourceSheet = true },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.outlineVariant,
                shadowElevation = 2.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val bmp = avatarBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp,
                            contentDescription = "头像",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "上传头像",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Card(
                shape = RadiusLg,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    ProfileRow(title = "昵称", value = nickname, onClick = { showNicknameSheet = true })
                    ProfileDivider()
                    ProfileRow(title = "拾物号", value = userId, showArrow = false, onClick = {
                        showCenteredToast(context, "拾物号为拾物APP用户唯一标识，暂不支持修改。", Toast.LENGTH_LONG)
                    })
                    ProfileDivider()
                    ProfileRow(title = "性别", value = when (gender) {
                        "male" -> "男"
                        "female" -> "女"
                        else -> ""
                    }, onClick = { showGenderSheet = true })
                    ProfileDivider()
                    ProfileRow(title = "年龄段", value = ageRange, onClick = { showAgeRangeSheet = true })
                }
            }
        }
    }

    if (showAvatarSourceSheet) {
        AvatarSourceBottomSheet(
            onTakePhoto = {
                showAvatarSourceSheet = false
                val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) {
                    openCamera(context, cameraLauncher) { uri -> cameraUri = uri }
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            onChooseFromAlbum = {
                showAvatarSourceSheet = false
                try {
                    galleryLauncher.launch("image/*")
                } catch (_: Exception) {
                    showCenteredToast(context, "无法打开相册")
                }
            },
            onCancel = { showAvatarSourceSheet = false },
            onDismiss = { showAvatarSourceSheet = false },
        )
    }

    if (showNicknameSheet) {
        NicknameEditSheet(
            currentNickname = nickname,
            onSave = { newNickname ->
                showNicknameSheet = false
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            repository.updateUserProfile(mapOf("nickname" to newNickname))
                        }
                        refreshProfile()
                        showCenteredToast(context, "昵称已更新")
                    } catch (_: Exception) {
                        showCenteredToast(context, "昵称更新失败，请重试")
                    }
                }
            },
            onDismiss = { showNicknameSheet = false },
        )
    }

    if (showGenderSheet) {
        GenderPickerSheet(
            currentGender = gender,
            onConfirm = { newGender ->
                showGenderSheet = false
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            repository.updateUserProfile(mapOf("gender" to newGender))
                        }
                        refreshProfile()
                        showCenteredToast(context, "性别已更新")
                    } catch (_: Exception) {
                        showCenteredToast(context, "性别更新失败，请重试")
                    }
                }
            },
            onDismiss = { showGenderSheet = false },
        )
    }

    if (showAgeRangeSheet) {
        AgeRangePickerSheet(
            currentAgeRange = ageRange,
            onConfirm = { newAgeRange ->
                showAgeRangeSheet = false
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            repository.updateUserProfile(mapOf("age_range" to newAgeRange))
                        }
                        refreshProfile()
                        showCenteredToast(context, "年龄段已更新")
                    } catch (_: Exception) {
                        showCenteredToast(context, "年龄段更新失败，请重试")
                    }
                }
            },
            onDismiss = { showAgeRangeSheet = false },
        )
    }
}

/**
 * 个人信息卡片中的单行选项 — 选项名靠左，值靠右，右侧可选箭头。
 * @param title 选项名
 * @param value 当前值
 * @param showArrow 是否显示右侧 ">" 箭头
 * @param onClick 点击回调
 */
@Composable
private fun ProfileRow(
    title: String,
    value: String,
    showArrow: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            color = if (value.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 200.dp),
        )
        if (showArrow) {
            Spacer(Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "进入", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

/** 个人信息卡片中选项行之间的分割线 */
@Composable
private fun ProfileDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

/**
 * 头像来源选择器 — 底部弹出，包含拍照、相册两个操作和一个取消按钮。
 *
 * 布局：两个独立圆角卡片，卡片 1 包含拍照+相册，卡片 2 单独包含取消，符合 iOS 风格。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AvatarSourceBottomSheet(
    onTakePhoto: () -> Unit,
    onChooseFromAlbum: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Card(
                shape = RadiusLg,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) {
                Column {
                    TextButton(
                        onClick = onTakePhoto,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    ) { Text("拍照", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface) }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    TextButton(
                        onClick = onChooseFromAlbum,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    ) { Text("从手机相册选择", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface) }
                }
            }
            Spacer(Modifier.height(12.dp))
            Card(
                shape = RadiusLg,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                ) { Text("取消", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

/**
 * 昵称编辑弹窗 — ModalBottomSheet 形式。
 * 输入框不限制输入长度，点击"保存"时校验是否超过 30 个字符，超过则 Toast 提示。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NicknameEditSheet(
    currentNickname: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var nickname by remember { mutableStateOf(currentNickname) }
    val context = LocalContext.current

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
                    "修改昵称",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.Center),
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterEnd).size(32.dp),
                ) {
                    Icon(Icons.Default.Close, "关闭", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            val horizontalPadding = Modifier.padding(horizontal = (0.07f * 360).dp)
            Row(
                modifier = horizontalPadding.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("昵称", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(0.2f))
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    placeholder = { Text("请输入昵称", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RadiusMd,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                    trailingIcon = {
                        if (nickname.isNotEmpty()) {
                            IconButton(onClick = { nickname = "" }, modifier = Modifier.size(20.dp)) {
                                Box(
                                    modifier = Modifier.size(18.dp).background(MaterialTheme.colorScheme.outline, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Default.Close, "清空", tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    },
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "昵称限制1-30个字符，一个汉字为1个字符",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = horizontalPadding,
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (nickname.length > 30) {
                        showCenteredToast(context, "你提交的昵称不能使用，请更换其他昵称后再试")
                        return@Button
                    }
                    onSave(nickname)
                },
                enabled = nickname.isNotEmpty(),
                modifier = horizontalPadding.fillMaxWidth().height(48.dp),
                shape = RadiusMd,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = OnPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.outline,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text("保存", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

/**
 * 性别选择器 — ModalBottomSheet + RadioButton 列表（男/女），底部确认按钮。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenderPickerSheet(
    currentGender: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedGender by remember { mutableStateOf(currentGender) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    "选择性别",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.Center),
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterEnd).size(32.dp),
                ) {
                    Icon(Icons.Default.Close, "关闭", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                listOf("male" to "男", "female" to "女").forEach { (key, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { selectedGender = key }
                            .padding(vertical = 14.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedGender == key,
                            onClick = { selectedGender = key },
                            colors = RadioButtonDefaults.colors(selectedColor = Primary),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { onConfirm(selectedGender) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(48.dp),
                shape = RadiusMd,
                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
            ) {
                Text("确认", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

/**
 * 年龄段选择器 — ModalBottomSheet + 11 个 RadioButton 选项（0-9 ~ 100及以上，10 岁间隔）。
 * 选项列表包裹在 25% 屏幕高的可滚动容器内，确认按钮在下方固定位置。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgeRangePickerSheet(
    currentAgeRange: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val ageRanges = listOf("0-9", "10-19", "20-29", "30-39", "40-49", "50-59", "60-69", "70-79", "80-89", "90-99", "100及以上")
    var selectedAgeRange by remember { mutableStateOf(currentAgeRange) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    "选择年龄段",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.Center),
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterEnd).size(32.dp),
                ) {
                    Icon(Icons.Default.Close, "关闭", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                }
            }

            Spacer(Modifier.height(4.dp))

            Column(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.25f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                ageRanges.forEach { range ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { selectedAgeRange = range }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedAgeRange == range,
                            onClick = { selectedAgeRange = range },
                            colors = RadioButtonDefaults.colors(selectedColor = Primary),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(range, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { onConfirm(selectedAgeRange) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(48.dp),
                shape = RadiusMd,
                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
            ) {
                Text("确认", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

/**
 * 图片裁剪全屏界面 — 黑色背景，支持双指缩放/拖拽，中心叠加白色圆形裁剪指示框。
 * 顶部栏：取消(左) | 修剪图片(中) | 确定(右)。
 * 确定时以裁剪框中心为基准从原图截取正方形区域。
 */
@Composable
private fun ImageCropScreen(
    sourceBitmap: Bitmap,
    onCancel: () -> Unit,
    onConfirm: (Bitmap) -> Unit,
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onCancel) {
                    Text("取消", color = Color.White, fontSize = 16.sp)
                }
                Text("修剪图片", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                TextButton(onClick = {
                    val bm = sourceBitmap
                    val srcW = bm.width.toFloat()
                    val srcH = bm.height.toFloat()
                    val cropDiameterPx = (300.dp.value * 3).coerceAtMost(srcW).coerceAtMost(srcH)
                    val cropRadius = cropDiameterPx / 2f / scale
                    val centerSrcX = srcW / 2f - offsetX / scale
                    val centerSrcY = srcH / 2f - offsetY / scale
                    val left = (centerSrcX - cropRadius).toInt().coerceIn(0, bm.width - 1)
                    val top = (centerSrcY - cropRadius).toInt().coerceIn(0, bm.height - 1)
                    val size = (cropRadius * 2).toInt().coerceAtMost(bm.width - left).coerceAtMost(bm.height - top)
                    val cropped = Bitmap.createBitmap(bm, left, top, size, size)
                    onConfirm(cropped)
                }) {
                    Text("确定", color = Color.White, fontSize = 16.sp)
                }
            }

            Box(
                modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 5f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX
                        translationY = offsetY
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        bitmap = sourceBitmap.asImageBitmap(),
                        contentDescription = "裁剪源图片",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }

                val cropDiameter = 300.dp
                Canvas(modifier = Modifier.size(cropDiameter)) {
                    val circleRadius = size.minDimension / 2f
                    drawCircle(
                        color = Color.White.copy(alpha = 0.3f),
                        radius = circleRadius,
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }
        }
    }
}
