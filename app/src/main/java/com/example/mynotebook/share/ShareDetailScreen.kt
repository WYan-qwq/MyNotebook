package com.example.mynotebook.share

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareDetailScreen(
    shareId: Int,
    vm: ShareViewModel,
    onBack: () -> Unit
) {
    val ui by vm.ui.collectAsState()

    // 首次进入时拉列表（VM 内部自行去重即可）
    LaunchedEffect(Unit) { if (!ui.loading && ui.items.isEmpty()) vm.refresh() }

    val share = remember(ui.items, shareId) { ui.items.find { it.sharingId == shareId } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Share details") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                ui.loading && share == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                share == null -> Text("Not found", Modifier.align(Alignment.Center))
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { ShareHeaderCard(share) }
                        item {
                            Text(
                                "Plans on ${share.planDate}",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        items(share.plans, key = { it.id }) { p ->
                            PlanRowDetail(p)
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareHeaderCard(share: com.example.mynotebook.api.ShareView) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarImage(share.author.picture)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    val displayName = share.author.userName?.takeIf { it.isNotBlank() } ?: "user${share.author.userId}"
                    Text(displayName, style = MaterialTheme.typography.titleMedium)
                    Text(share.planDate, style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(10.dp))

            val title = share.share.title
            if (!title.isNullOrBlank()) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
            }

            val details = share.share.details
            if (!details.isNullOrBlank()) {
                Text(details, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** 详情页的计划行：不显示对号/叉号 */
@Composable
private fun PlanRowDetail(p: PlanBrief) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "%02d:%02d".format((p.hour ?: 0).coerceIn(0, 23), (p.minute ?: 0).coerceIn(0, 59)),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.width(72.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    p.title?.takeIf { it.isNotBlank() } ?: "(Untitled)",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            p.details?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/**
 * 头像（空值安全）。picture 为 null 或空白时使用占位图。
 * 不调用 trim()，避免 NPE。
 */
@Composable
private fun AvatarImage(
    picture: String?,
    size: Dp = 40.dp
) {
    val url = picture?.takeIf { it.isNotBlank() }

    if (url != null) {
        AsyncImage(
            model = url,
            contentDescription = "avatar",
            modifier = Modifier.size(size).clip(CircleShape),
            placeholder = painterResource(R.drawable.init), // 你的占位 png
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