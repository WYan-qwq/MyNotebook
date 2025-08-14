package com.example.mynotebook.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Share
import androidx.compose.ui.graphics.vector.ImageVector

// 不要使用 data object，普通 object 就好，兼容性更好
sealed class HomeTab(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Today : HomeTab("today",  "today", Icons.Outlined.CalendarToday)
    object Week  : HomeTab("week",   "week", Icons.Outlined.DateRange)
    object Share : HomeTab("share",  "sharing", Icons.Outlined.Share)
    object Me    : HomeTab("me",     "me",     Icons.Outlined.AccountCircle)

    companion object {
        val bottomItems: List<HomeTab> = listOf(Today, Week, Share, Me)
    }
}
