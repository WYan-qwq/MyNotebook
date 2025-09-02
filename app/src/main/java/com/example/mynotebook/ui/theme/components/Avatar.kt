package com.example.mynotebook.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mynotebook.BuildConfig
import com.example.mynotebook.R

private fun toFullUrl(picture: String?): String? {
    val raw = picture?.trim().orEmpty()
    if (raw.isBlank()) return null
    if (raw.startsWith("http://") || raw.startsWith("https://")) return raw
    val base = BuildConfig.BASE_URL.trimEnd('/')
    return if (raw.startsWith("/")) "$base$raw" else "$base/$raw"
}

/** 统一的作者头像组件 */
@Composable
fun AuthorAvatar(
    picture: String?,          // 允许为 null
    size: Dp = 40.dp
) {
    val url = toFullUrl(picture)
    if (url != null) {
        AsyncImage(
            model = url,
            contentDescription = "avatar",
            modifier = Modifier
                .size(size)
                .clip(CircleShape),
            // 也可以设置 placeholder / error，占位图资源按你项目调整
            // placeholder = painterResource(R.drawable.xxx),
            // error = painterResource(R.drawable.xxx),
        )
    } else {
        // 占位圆形头像
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                modifier = Modifier
                    .size(size * 0.6f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}