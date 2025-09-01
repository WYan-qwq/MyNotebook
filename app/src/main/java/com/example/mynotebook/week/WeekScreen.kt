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
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import java.time.format.DateTimeFormatter
import androidx.compose.material.icons.outlined.Share
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekRoute(
    userId: Int,
    onNavigateToCalendar: () -> Unit, // 左上角返回/进入日历
    navController: NavHostController,
    onShareDay: (String) -> Unit,
    vm: WeekViewModel = viewModel(factory = WeekViewModel.provideFactory(userId))
) {
    val ui by vm.ui.collectAsState()
    val rangeFmt = remember { DateTimeFormatter.ofPattern("MMM d") } // Jun 30
    val monthTitleFmt = remember { DateTimeFormatter.ofPattern("MMMM") } // July
    val backStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(backStackEntry) {
        val handle = backStackEntry?.savedStateHandle
        // 来自 Add 页的周刷新（如果你之前已加）
        if (handle?.get<Boolean>("week_refresh") == true) {
            vm.setWeekByDate(ui.today)
            handle.remove<Boolean>("week_refresh")
        }
        // 来自 Calendar 的日期选择
        handle?.get<String>("jump_to_date")?.let { dateStr ->
            runCatching { LocalDate.parse(dateStr) }.getOrNull()?.let { chosen ->
                vm.setWeekByDate(chosen)
            }
            handle.remove<String>("jump_to_date")
        }
    }
    LaunchedEffect(backStackEntry) {
        val handle = backStackEntry?.savedStateHandle
        if (handle?.get<Boolean>("week_refresh") == true) {
            vm.setWeekByDate(ui.today)                  // 或 vm.setWeekByDate(LocalDate.now())
            handle.remove<Boolean>("week_refresh")
        }
    }

    val inCurrentWeek = ui.today >= ui.weekStart && ui.today <= ui.weekEnd
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
        },
        floatingActionButton = {
            if (!inCurrentWeek) {
                TextButton(
                    onClick = { vm.setWeekByDate(ui.today) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .navigationBarsPadding()        // 避免被手势条顶住
                        .padding(end = 10.dp, bottom = 16.dp) // ✅ 更靠右下；需要更低就把 bottom 再调小些
                ) {
                    Icon(
                        Icons.Outlined.Today,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)  // ✅ 小图标
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Back to today",
                        style = MaterialTheme.typography.labelSmall // ✅ 小字号
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    )
    { padding ->
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
                    val isFuture = g.date.isAfter(ui.today)
                    DaySummaryCard(
                        group = g,
                        isToday = g.date == ui.today,
                        expanded = ui.expanded == g.date,
                        onClick = { vm.toggleDay(g.date) },
                        onAction = {
                            navController.navigate("add")
                            runCatching {
                                navController.getBackStackEntry("add")
                                    .savedStateHandle["prefill_date"] = g.date.toString()
                            }
                        },
                        showAction = isFuture,
                        onShare = { onShareDay(g.date.toString()) }   // ✅ 新增：点分享
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
    onClick: () -> Unit,
    showAction: Boolean,          // ✅ 新增：是否显示右侧操作按钮（用于未来日期）
    onAction: () -> Unit,
    onShare: () -> Unit  // ✅ 新增：点击后跳转到 Add 页
) {
    // 仅有计划时展示表情；无计划不显示
    val face: String? = when {
        group.total == 0          -> null
        group.done == group.total -> "😆"
        group.ratio > 0.5         -> "🙂"
        group.ratio > 0.0         -> "🙁"
        else                      -> "😡"
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

                // 右侧：未来日期显示操作按钮；否则显示表情（仅当有计划时）
                if (showAction) {
                    val label = if (group.total == 0) "Add new plan" else "Edit"
                    TextButton(
                        onClick = onAction,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) { Text(label, style = MaterialTheme.typography.labelSmall) }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        face?.let {
                            Text(it, style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.width(6.dp))
                        }
                        IconButton(onClick = onShare) {
                            Icon(Icons.Outlined.Share, contentDescription = "Share this day")
                        }
                    }
                }
            }

            if (expanded && group.plans.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider() // ✅ Material3
                Spacer(Modifier.height(8.dp))
                // 展开：当日计划列表
                group.plans.forEach { p ->
                    // 用当天日期 + 计划时分，和当前时间比较
                    val now = LocalDateTime.now()
                    val planTime = LocalDateTime.of(
                        group.date,
                        LocalTime.of(p.hour ?: 0, p.minute ?: 0)
                    )
                    val isPast = !planTime.isAfter(now) // planTime <= now

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

                        when (p.finished ?: 0) {
                            1    -> Text("✓", color = MaterialTheme.colorScheme.primary)
                            else -> if (isPast) {
                                Text("×", color = MaterialTheme.colorScheme.error)
                            } else {
                                Spacer(Modifier.width(0.dp)) // 未来且未完成：不显示任何标记
                            }
                        }
                    }
                }
            }
        }
    }
}