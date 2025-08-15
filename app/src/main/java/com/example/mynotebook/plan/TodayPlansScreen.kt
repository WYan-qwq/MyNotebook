package com.example.mynotebook.plan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.mynotebook.api.PlanItem
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayPlansRoute(
    userId: Int,
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    vm: TodayPlansViewModel = viewModel(factory = TodayPlansViewModel.provideFactory(userId))
) {
    val ui by vm.ui.collectAsState()
    val scope = rememberCoroutineScope()

    // 从 Add 返回后刷新
    val backStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(backStackEntry) {
        val handle = backStackEntry?.savedStateHandle
        if (handle?.get<Boolean>("plan_added") == true) {
            vm.refresh()
            scope.launch { snackbarHostState.showSnackbar("Add successfully!") }
            handle.remove<Boolean>("plan_added")
        }
    }

    val expandedIds = remember { mutableStateListOf<Int>() }
    val plans = remember(ui.items) {
        ui.items.sortedWith(compareBy({ it.hour ?: 0 }, { it.minute ?: 0 }, { it.id }))
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text("Today's Plans • ${ui.date.format(DateTimeFormatter.ISO_DATE)}")
            })
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                ui.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                ui.error != null -> Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(ui.error ?: "Error", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = { vm.refresh() }) { Text("Retry") }
                }
                plans.isEmpty() -> Text(
                    "No plans for today, click + to add plans.",
                    Modifier.align(Alignment.Center)
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(plans, key = { it.id }) { item ->
                        val expanded = expandedIds.contains(item.id)
                        PlanExpandableRow(
                            plan = item,
                            expanded = expanded,
                            onToggle = {
                                if (expanded) expandedIds.remove(item.id) else expandedIds.add(item.id)
                            },
                            onFinish = { id ->
                                vm.markFinished(id)
                                scope.launch { snackbarHostState.showSnackbar("Finished!") }
                            },
                            onSaveEdit = { id, hour, minute, title, details, alarm ->
                                vm.updatePlan(id, hour, minute, title, details, alarm)
                                scope.launch { snackbarHostState.showSnackbar("Updated!") }
                            },
                            onDelete = { id ->
                                vm.deletePlan(id)
                                scope.launch { snackbarHostState.showSnackbar("Deleted") }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanExpandableRow(
    plan: PlanItem,
    expanded: Boolean,
    onToggle: () -> Unit,
    onFinish: (Int) -> Unit,
    onSaveEdit: (id: Int, hour: Int, minute: Int, title: String, details: String?, alarm: Int) -> Unit,
    onDelete: (Int) -> Unit
) {
    val rotate by animateFloatAsState(if (expanded) 180f else 0f, label = "arrowRotate")
    val borderColor = if (expanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    else MaterialTheme.colorScheme.outline
    val containerColor = if (expanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
    else MaterialTheme.colorScheme.surface

    val now = LocalTime.now()
    val h0 = (plan.hour ?: 0).coerceIn(0, 23)
    val m0 = (plan.minute ?: 0).coerceIn(0, 59)
    val isExpired = now.hour > h0 || (now.hour == h0 && now.minute >= m0)
    val isFinished = (plan.finished ?: 0) == 1

    var editing by remember(plan.id) { mutableStateOf(false) }

    var title by remember(plan.id) { mutableStateOf(plan.title.orEmpty()) }
    var details by remember(plan.id) { mutableStateOf(plan.details.orEmpty()) }
    var alarm by remember(plan.id) { mutableStateOf((plan.alarm ?: 0) == 1) }
    var hour by remember(plan.id) { mutableStateOf(h0) }
    var minute by remember(plan.id) { mutableStateOf(m0) }

    val allowedHours = remember(now) { (now.hour..23).toList() }
    val allowedMinutes = remember(now, hour) {
        if (hour == now.hour) (now.minute..59).toList() else (0..59).toList()
    }
    val timeValid = hour in allowedHours && minute in allowedMinutes

    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = if (expanded) 1.dp else 0.dp,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "%02d:%02d".format(h0, m0),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.width(72.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    plan.title ?: "(Untitled)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))

                when {
                    isFinished -> Text("✓", color = MaterialTheme.colorScheme.primary)
                    !isExpired -> TextButton(
                        onClick = { onFinish(plan.id) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) { Text("Finish", style = MaterialTheme.typography.labelLarge) }
                    else -> Text("×", color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.width(8.dp))
                Icon(Icons.Outlined.ExpandMore, contentDescription = null, modifier = Modifier.rotate(rotate))
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))

                    if (!editing) {
                        plan.details?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(6.dp))
                        }
                        if ((plan.alarm ?: 0) == 1) {
                            Text("⏰ Alarm set", style = MaterialTheme.typography.labelMedium)
                        }

                        // ✅ 可编辑时（未完成且未过期）显示 Edit + Delete（无背景）
                        if (!isFinished && !isExpired) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(
                                    onClick = {
                                        editing = true
                                        title = plan.title.orEmpty()
                                        details = plan.details.orEmpty()
                                        alarm = (plan.alarm ?: 0) == 1
                                        hour = h0
                                        minute = m0
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) { Text("Edit", style = MaterialTheme.typography.labelLarge) }

                                Spacer(Modifier.width(8.dp))

                                TextButton(
                                    onClick = { onDelete(plan.id) },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) { Text("Delete", style = MaterialTheme.typography.labelLarge) }
                            }
                        }
                    } else {
                        // 编辑 UI（限制不能选过去时间）
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                var hourMenu by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = hourMenu,
                                    onExpandedChange = { hourMenu = it },
                                    modifier = Modifier.width(100.dp)
                                ) {
                                    OutlinedTextField(
                                        readOnly = true,
                                        value = "%02d".format(hour),
                                        onValueChange = {},
                                        label = { Text("Hour") },
                                        modifier = Modifier.menuAnchor()
                                    )
                                    ExposedDropdownMenu(expanded = hourMenu, onDismissRequest = { hourMenu = false }) {
                                        allowedHours.forEach {
                                            DropdownMenuItem(
                                                text = { Text("%02d".format(it)) },
                                                onClick = {
                                                    hour = it
                                                    if (minute !in allowedMinutes) minute = allowedMinutes.first()
                                                    hourMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                var minuteMenu by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = minuteMenu,
                                    onExpandedChange = { minuteMenu = it },
                                    modifier = Modifier.width(110.dp)
                                ) {
                                    OutlinedTextField(
                                        readOnly = true,
                                        value = "%02d".format(minute),
                                        onValueChange = {},
                                        label = { Text("Minute") },
                                        modifier = Modifier.menuAnchor()
                                    )
                                    ExposedDropdownMenu(expanded = minuteMenu, onDismissRequest = { minuteMenu = false }) {
                                        allowedMinutes.forEach {
                                            DropdownMenuItem(
                                                text = { Text("%02d".format(it)) },
                                                onClick = { minute = it; minuteMenu = false }
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                if (!(hour in allowedHours && minute in allowedMinutes)) {
                                    Text("Past time", color = MaterialTheme.colorScheme.error)
                                }
                            }

                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("Title") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = details,
                                onValueChange = { details = it },
                                label = { Text("Details") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = alarm, onCheckedChange = { alarm = it })
                                Spacer(Modifier.width(4.dp))
                                Text("Alarm")
                            }

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { editing = false }) { Text("Cancel") }
                                Spacer(Modifier.width(8.dp))
                                TextButton(
                                    enabled = (hour in allowedHours && minute in allowedMinutes) && title.isNotBlank(),
                                    onClick = {
                                        onSaveEdit(
                                            plan.id,
                                            hour,
                                            minute,
                                            title.trim(),
                                            details.trim().ifBlank { null },
                                            if (alarm) 1 else 0
                                        )
                                        editing = false
                                    }
                                ) { Text("Save") }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(hour: Int?, minute: Int?): String {
    val h = (hour ?: 0).coerceIn(0, 23)
    val m = (minute ?: 0).coerceIn(0, 59)
    return "%02d:%02d".format(h, m)
}