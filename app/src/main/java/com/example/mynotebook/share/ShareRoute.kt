package com.example.mynotebook.share

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.mynotebook.R
import com.example.mynotebook.api.PlanBrief
import com.example.mynotebook.api.ShareView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareRoute(
    vm: ShareViewModel,
    onShowMore: (ShareView) -> Unit    // ← 点击“Show more”跳新页面
) {
    val ui by vm.ui.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Share") }) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                ui.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                ui.error != null -> Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(ui.error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = { vm.refresh() }) { Text("Retry") }
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(ui.items, key = { it.sharingId }) { share ->
                        ShareCard(share = share, onShowMore = { onShowMore(share) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareCard(
    share: ShareView,
    onShowMore: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            // 顶部：头像 + 名称 + 日期
            Row(verticalAlignment = Alignment.CenterVertically) {
                AuthorAvatar(share.author.picture)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = share.author.userName?.takeIf { it.isNotBlank() } ?: "user${share.author.userId}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(share.planDate, style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(Modifier.height(10.dp))

            // 分享标题/内容
            share.share.title?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
            }
            share.share.details?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
            }

            // 计划预览：前两条
            val preview = share.plans.take(2)
            preview.forEach { PlanRow(it) }

            // Show more：跳转新页面（外部处理导航）
            if (share.plans.size > 2) {
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = onShowMore) {
                    Text("Show more")
                }
            }
        }
    }
}

@Composable
private fun PlanRow(p: PlanBrief) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "%02d:%02d".format((p.hour ?: 0).coerceIn(0, 23), (p.minute ?: 0).coerceIn(0, 59)),
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(64.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(p.title?.takeIf { it.isNotBlank() } ?: "(Untitled)")
            p.details?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
        // ✅ 不显示对号/叉号（未来未完成也不显示）
    }
}

/** 头像兜底：为空或空串用本地占位图，防止 NPE */
@Composable
private fun AuthorAvatar(
    picture: String?,           // 允许为 null
    size: Dp = 40.dp
) {
    // 👇 绝不再对 null 调用 trim；isNullOrBlank() 统一兜底
    val url = picture?.takeUnless { it.isBlank() }

    if (url != null) {
        AsyncImage(
            model = url,
            contentDescription = "avatar",
            modifier = Modifier.size(size).clip(CircleShape),
            placeholder = painterResource(R.drawable.init), // 你的默认头像
            error = painterResource(R.drawable.init),
            contentScale = ContentScale.Crop
        )
    } else {
        Image(
            painter = painterResource(R.drawable.init),
            contentDescription = "avatar",
            modifier = Modifier.size(size).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}