package com.example.mynotebook.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    // 主色：影响 Filled Button、Switch 等
    primary = Brown500,
    onPrimary = Color.White,
    primaryContainer = Brown600,
    onPrimaryContainer = Color.White,

    // 次级色：影响部分控件的次要样式
    secondary = Brown700,
    onSecondary = Color.White,
    secondaryContainer = Sand,
    onSecondaryContainer = Color(0xFF3B2A17),

    // 第三色（可做强调）
    tertiary = Sand,
    onTertiary = Color(0xFF3B2A17),

    // 背景/表面：奶白
    background = MilkWhite,
    onBackground = TextPrimary,
    surface = MilkWhite,
    onSurface = TextPrimary,
    surfaceVariant = MilkWhite2,
    onSurfaceVariant = TextSecondary,

    outline = OutlineColor,

    // 错误色（保留默认或自定义）
    error = Color(0xFFB00020),
    onError = Color.White,
)
@Composable
fun MyNotebookTheme(   // ← 如需保持原有函数名，改成你项目原先的名字
    content: @Composable () -> Unit
) {
    // 这里不启用动态取色，确保不会被系统配色覆盖
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        content = content
    )
}