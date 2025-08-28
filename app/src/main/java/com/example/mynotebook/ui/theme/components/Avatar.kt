package com.example.mynotebook.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mynotebook.R

@Composable
fun Avatar(url: String?, name: String?, size: Dp = 40.dp) {
    val model = url?.takeIf { it.isNotBlank() }

    if (model != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(model)
                .crossfade(true)
                .build(),
            contentDescription = null,
            placeholder = painterResource(R.drawable.init),
            error = painterResource(R.drawable.init),
            fallback = painterResource(R.drawable.init),
            modifier = Modifier.size(size).clip(CircleShape)
        )
    } else {
        // 没有 URL：显示你放在 drawable 里的默认头像
        Image(
            painter = painterResource(R.drawable.init),
            contentDescription = null,
            modifier = Modifier.size(size).clip(CircleShape)
        )
        // 如果你想改成首字母占位，也可以：
        // val initials = name?.trim()?.takeIf { it.isNotEmpty() }?.take(1)?.uppercase() ?: "?"
        // Box(...){ Text(initials) }
    }
}