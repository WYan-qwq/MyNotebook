package com.example.mynotebook.week

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.mynotebook.home.HomeTab
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthCalendarScreen(
    navController: NavHostController
) {
    val today = remember { LocalDate.now() }
    var currentYm by remember { mutableStateOf(YearMonth.from(today)) }

    val firstDay = currentYm.atDay(1)
    val lastDay = currentYm.atEndOfMonth()
    val firstGridDay = firstDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val lastGridDay = lastDay.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

    val days: List<LocalDate> = remember(currentYm) {
        val total = java.time.temporal.ChronoUnit.DAYS.between(firstGridDay, lastGridDay) + 1
        (0 until total).map { firstGridDay.plusDays(it) }
    }

    val monthTitle = remember(currentYm) {
        currentYm.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Outlined.ChevronLeft, contentDescription = "Back")
                    }
                },
                actions = {
                    // 月份切换
                    IconButton(onClick = { currentYm = currentYm.minusMonths(1) }) {
                        Icon(Icons.Outlined.ChevronLeft, contentDescription = "Prev month")
                    }
                    Text(monthTitle, modifier = Modifier.padding(horizontal = 8.dp))
                    IconButton(onClick = { currentYm = currentYm.plusMonths(1) }) {
                        Icon(Icons.Outlined.ChevronRight, contentDescription = "Next month")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp)
        ) {
            // 星期栏（从周一开始）
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun").forEach {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.widthIn(min = 40.dp).weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            // 日历网格：按 7 天一行切片
            days.chunked(7).forEach { week ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    week.forEach { day ->
                        val isInMonth = day.month == currentYm.month
                        val isPast = isInMonth && day.isBefore(today)
                        val isToday = day.isEqual(today)

                        val baseAlpha = when {
                            !isInMonth -> 0.35f      // 非当月
                            isPast -> 0.55f          // 当月已过去的日期变浅
                            else -> 1f
                        }

                        // 单元格
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            tonalElevation = if (isToday) 3.dp else 0.dp,
                            border = if (isToday) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .alpha(baseAlpha)
                                .clickable {
                                    // 选择日期：把日期回传给 Week 页，然后返回
                                    runCatching {
                                        navController.getBackStackEntry(HomeTab.Week.route)
                                            .savedStateHandle["jump_to_date"] = day.toString()
                                    }
                                    navController.popBackStack()
                                }
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = day.dayOfMonth.toString(),
                                    style = if (isToday)
                                        MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    else
                                        MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}