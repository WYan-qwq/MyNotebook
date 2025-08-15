package com.example.mynotebook.week

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.format.DateTimeFormatter
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekRoute(
    userId: Int,
    onNavigateToCalendar: () -> Unit, // 左上角返回/进入日历
    vm: WeekViewModel = viewModel(factory = WeekViewModel.provideFactory(userId))
) {
    val ui by vm.ui.collectAsState()
    val rangeFmt = remember { DateTimeFormatter.ofPattern("MMM d") } // Jun 30
    val monthTitleFmt = remember { DateTimeFormatter.ofPattern("MMMM") } // July

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // 如果一周跨月，这里显示“当前月”，你的草图是“显示今天所在的月”
                    Text(ui.today.format(monthTitleFmt))
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateToCalendar) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Calendar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Week range + arrows
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { vm.prevWeek() }) {
                    Icon(Icons.Outlined.ChevronLeft, contentDescription = "Prev week")
                }
                Text(
                    "${ui.weekStart.format(rangeFmt)} - ${ui.weekEnd.format(rangeFmt)}",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = { vm.nextWeek() }) {
                    Icon(Icons.Outlined.ChevronRight, contentDescription = "Next week")
                }
            }

            if (ui.loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            ui.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(ui.groups, key = { it.date }) { g ->
                    DaySummaryCard(
                        group = g,
                        isToday = g.date == ui.today,
                        expanded = ui.expanded == g.date,
                        onClick = { vm.toggleDay(g.date) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DaySummaryCard(
    group: DayGroup,
    isToday: Boolean,
    expanded: Boolean,
    onClick: () -> Unit
) {
    val face = when {
        group.total == 0           -> "😱"
        group.done == group.total  -> "😆"
        group.ratio > 0.5          -> "🙂"
        group.ratio > 0.0          -> "🙁"
        else                       -> "😡"
    }

    val bg = if (isToday)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    else Color.Unspecified

    Surface(
        tonalElevation = if (isToday) 2.dp else 0.dp,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 左侧：日期（天）
                Text(
                    group.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(56.dp)
                )
                Spacer(Modifier.width(8.dp))

                // 中间：统计
                Text(
                    "${group.total} plans   ${group.done} complete",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                // 右侧：表情
                Text(face, style = MaterialTheme.typography.titleLarge)
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Divider()
                Spacer(Modifier.height(8.dp))
                // 展开：当日计划列表
                group.plans.forEach { p ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "%02d:%02d".format(p.hour ?: 0, p.minute ?: 0),
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(64.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.title ?: "(Untitled)")
                            p.details?.takeIf { it.isNotBlank() }?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if ((p.finished ?: 0) == 0) {
                            Text("×", color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("✓", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}