package com.example.mynotebook.share

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import com.example.mynotebook.R
import com.example.mynotebook.api.PlanBrief
import com.example.mynotebook.api.ShareView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareRoute(
    vm: ShareViewModel,
    onShowMore: (ShareView) -> Unit
) {
    val ui by vm.ui.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Share") }) }) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                ui.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                ui.error != null -> Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
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
                        ShareCard(
                            share = share,
                            onShowMore = { onShowMore(share) } // 点击整卡或按钮都跳详情
                        )
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onShowMore) // ✅ 整个卡片可点击
    ) {
        Column(Modifier.padding(16.dp)) {
            // 顶部：头像 + 名称 + 日期
            Row(verticalAlignment = Alignment.CenterVertically) {
                AuthorAvatar(share.author.picture)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = share.author.userName?.takeIf { it.isNotBlank() }
                            ?: "user${share.author.userId}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(share.planDate, style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(Modifier.height(10.dp))

            // 分享标题/内容
            val title = share.share.title
            if (!title.isNullOrBlank()) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
            }
            share.share.details?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
            }

            // 计划预览：前两条
            val preview = share.plans.take(2)
            preview.forEach { PlanRow(it) }

            if (share.plans.size > 2) {
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = onShowMore) { Text("Click to see plan details") }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()

            // ✅ 右下角：likes / comments
            val metaColor = MaterialTheme.colorScheme.onSurfaceVariant
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.FavoriteBorder,
                    contentDescription = "likes",
                    modifier = Modifier.size(16.dp),
                    tint = metaColor
                )
                Spacer(Modifier.width(4.dp))
                Text("${share.likes}", style = MaterialTheme.typography.labelSmall, color = metaColor)

                Spacer(Modifier.width(12.dp))

                Icon(
                    Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "comments",
                    modifier = Modifier.size(16.dp),
                    tint = metaColor
                )
                Spacer(Modifier.width(4.dp))
                Text("${share.comments}", style = MaterialTheme.typography.labelSmall, color = metaColor)
            }
        }
    }
}

@Composable
private fun PlanRow(p: PlanBrief) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
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
        // 不显示对号/叉号
    }
}

/** 头像兜底：为空或空串用本地占位图，避免 NPE */
@Composable
private fun AuthorAvatar(
    picture: String?,           // 允许为 null
    size: Dp = 40.dp
) {
    val url = picture?.takeUnless { it.isBlank() }
    if (url != null) {
        AsyncImage(
            model = url,
            contentDescription = "avatar",
            modifier = Modifier.size(size).clip(CircleShape),
            placeholder = painterResource(R.drawable.init),
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