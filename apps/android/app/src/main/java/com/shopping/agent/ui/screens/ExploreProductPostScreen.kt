package com.shopping.agent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.shopping.agent.data.mock.MockExplorePosts
import com.shopping.agent.data.model.ExplorePost
import com.shopping.agent.ui.components.GradientTopBar
import com.shopping.agent.ui.theme.*

@Composable
fun ExploreProductPostScreen(
    postId: String,
    onBack: () -> Unit,
) {
    val post = MockExplorePosts.posts.find { it.postId == postId } ?: return
    var currentImageIndex by remember { mutableStateOf(0) }
    val images = post.product.images

    Column(modifier = Modifier.fillMaxSize().background(Neutral50)) {
        // 渐变条 + 返回箭头
        GradientTopBar(icons = {
            IconButton(onClick = onBack, modifier = Modifier.size(34.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Neutral700, modifier = Modifier.size(26.dp))
            }
        })

        // 作者信息行
        Row(
            modifier = Modifier.fillMaxWidth().background(Neutral0).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = PrimaryLight) {
                Box(contentAlignment = Alignment.Center) {
                    Text(post.author.name.take(1), color = Primary,
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(post.author.name, style = MaterialTheme.typography.titleMedium, color = Neutral900, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.MoreHoriz, "更多", tint = Neutral500)
            }
        }

        // 可滚动内容
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            // 商品大图
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(model = images[currentImageIndex], contentDescription = post.product.title,
                    modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f), contentScale = ContentScale.Crop)
                if (images.size > 1) {
                    Row(modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Box(modifier = Modifier.width(60.dp).fillMaxHeight().clickable { if (currentImageIndex > 0) currentImageIndex-- })
                        Box(modifier = Modifier.width(60.dp).fillMaxHeight().clickable { if (currentImageIndex < images.size - 1) currentImageIndex++ })
                    }
                }
            }

            // 分页指示点
            if (images.size > 1) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center) {
                    images.forEachIndexed { index, _ ->
                        Box(modifier = Modifier.padding(horizontal = 3.dp).size(if (index == currentImageIndex) 8.dp else 6.dp)
                            .clip(CircleShape).background(if (index == currentImageIndex) Primary else Neutral300))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // 正文（左对齐，非卡片）
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(post.content.title, style = MaterialTheme.typography.titleLarge, color = Neutral900, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                post.content.body.forEach { paragraph ->
                    Text(paragraph, style = MaterialTheme.typography.bodyLarge, color = Neutral700,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.6)
                    Spacer(Modifier.height(12.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
