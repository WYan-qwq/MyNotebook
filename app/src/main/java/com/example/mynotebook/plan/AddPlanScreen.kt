package com.example.mynotebook.plan

import android.app.DatePickerDialog
import android.widget.NumberPicker
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.compose.ui.viewinterop.AndroidView
import com.example.mynotebook.api.PlanItem
import com.example.mynotebook.home.HomeTab
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlanScreen(
    userId: Int,
    navController: NavHostController,
    onDone: () -> Unit,
    vm: AddPlanViewModel = viewModel(factory = AddPlanViewModel.provideFactory(userId))
) {
    val ui by vm.ui.collectAsState()
    val ctx = LocalContext.current
    var editedAny by remember { mutableStateOf(false) } // 本页面是否编辑过已有计划

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Plans") },
                actions = {
                    TextButton(onClick = {
                        // ✅ 返回前通知 Today 与 Week 刷新
                        if (ui.createdAny || editedAny) {
                            // Today
                            runCatching {
                                navController.getBackStackEntry(HomeTab.Today.route)
                                    .savedStateHandle["plan_added"] = true
                            }
                            // Week
                            runCatching {
                                navController.getBackStackEntry(HomeTab.Week.route)
                                    .savedStateHandle["week_refresh"] = true
                            }
                        }
                        onDone()
                    }) { Text("Done") }
                }
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            Row(
                Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Date", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(12.dp))
                OutlinedButton(
                    onClick = {
                        val d = ui.date
                        DatePickerDialog(
                            ctx,
                            { _, y, m, day -> vm.setDate(LocalDate.of(y, m + 1, day)) },
                            d.year, d.monthValue - 1, d.dayOfMonth
                        ).show()
                    }
                ) { Text(ui.date.format(DateTimeFormatter.ISO_DATE)) }
                if (ui.loadingList) {
                    Spacer(Modifier.width(12.dp))
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                }
            }
            ui.listError?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // 主体：LazyColumn — 已有计划(可编辑) + 草稿编辑
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section: 已有计划（✅ 可展开编辑，无时间限制）
                if (ui.existing.isNotEmpty()) {
                    item { Text("Plans of the day", style = MaterialTheme.typography.titleMedium) }
                    items(ui.existing, key = { it.id }) { item ->
                        ExistingPlanEditableRow(
                            plan = item,
                            onSaveEdit = { id, h, m, title, details, alarm ->
                                vm.updatePlan(id, h, m, title, details, alarm)
                                editedAny = true
                            }
                        )
                    }
                }

                // Section: 新增草稿
                item { Text("Add new", style = MaterialTheme.typography.titleMedium) }
                items(ui.drafts, key = { it.id }) { draft ->
                    DraftEditorRow(
                        draft = draft,
                        onHour = { vm.setDraftHour(draft.id, it) },
                        onMinute = { vm.setDraftMinute(draft.id, it) },
                        onTitle = { vm.setDraftTitle(draft.id, it) },
                        onDetails = { vm.setDraftDetails(draft.id, it) },
                        onToggleAlarm = { vm.toggleDraftAlarm(draft.id) },
                        onConfirm = { vm.submitDraft(draft.id) },
                        onCancel = { vm.removeDraft(draft.id) }
                    )
                }

                item { AddCard(onClick = { vm.addDraft() }) }
            }
        }
    }
}

/* ---------------- 已有计划：可展开编辑（无时间限制） ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExistingPlanEditableRow(
    plan: PlanItem,
    onSaveEdit: (id: Int, hour: Int, minute: Int, title: String, details: String?, alarm: Int) -> Unit
) {
    var expanded by remember(plan.id) { mutableStateOf(false) }
    var editing by remember(plan.id) { mutableStateOf(false) }

    val rotate by animateFloatAsState(if (expanded) 180f else 0f, label = "arrow")
    val borderColor =
        if (expanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        else MaterialTheme.colorScheme.outline
    val containerColor =
        if (expanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
        else MaterialTheme.colorScheme.surface

    // 编辑态临时值
    var hour by remember(plan.id) { mutableStateOf((plan.hour ?: 0).coerceIn(0, 23)) }
    var minute by remember(plan.id) { mutableStateOf((plan.minute ?: 0).coerceIn(0, 59)) }
    var title by remember(plan.id) { mutableStateOf(plan.title.orEmpty()) }
    var details by remember(plan.id) { mutableStateOf(plan.details.orEmpty()) }
    var alarm by remember(plan.id) { mutableStateOf((plan.alarm ?: 0) == 1) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = if (expanded) 1.dp else 0.dp,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "%02d:%02d".format(plan.hour ?: 0, plan.minute ?: 0),
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
                Icon(Icons.Outlined.ExpandMore, contentDescription = null, modifier = Modifier.rotate(rotate))
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))

                    if (!editing) {
                        // 详情
                        plan.details?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(6.dp))
                        }
                        if ((plan.alarm ?: 0) == 1) {
                            Text("⏰ Alarm set", style = MaterialTheme.typography.labelMedium)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(
                                onClick = {
                                    // 进入编辑时带入当前值
                                    hour = (plan.hour ?: 0).coerceIn(0, 23)
                                    minute = (plan.minute ?: 0).coerceIn(0, 59)
                                    title = plan.title.orEmpty()
                                    details = plan.details.orEmpty()
                                    alarm = (plan.alarm ?: 0) == 1
                                    editing = true
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) { Text("Edit", style = MaterialTheme.typography.labelLarge) }
                        }
                    } else {
                        // 编辑 —— 无时间限制
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row {
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
                                    ExposedDropdownMenu(
                                        expanded = hourMenu,
                                        onDismissRequest = { hourMenu = false }
                                    ) {
                                        (0..23).forEach {
                                            DropdownMenuItem(
                                                text = { Text("%02d".format(it)) },
                                                onClick = { hour = it; hourMenu = false }
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
                                    ExposedDropdownMenu(
                                        expanded = minuteMenu,
                                        onDismissRequest = { minuteMenu = false }
                                    ) {
                                        (0..59).forEach {
                                            DropdownMenuItem(
                                                text = { Text("%02d".format(it)) },
                                                onClick = { minute = it; minuteMenu = false }
                                            )
                                        }
                                    }
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
                                    enabled = title.isNotBlank(),
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

/* ---------------- 新增草稿行（保持不变） ---------------- */

@Composable
private fun DraftEditorRow(
    draft: DraftPlan,
    onHour: (Int) -> Unit,
    onMinute: (Int) -> Unit,
    onTitle: (String) -> Unit,
    onDetails: (String) -> Unit,
    onToggleAlarm: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimePickers(
                    hour = draft.hour,
                    minute = draft.minute,
                    onHourChange = onHour,
                    onMinuteChange = onMinute
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = draft.title,
                        onValueChange = onTitle,
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = draft.details,
                        onValueChange = onDetails,
                        label = { Text("Details") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = draft.alarm, onCheckedChange = { onToggleAlarm() })
                        Text("Alarm")
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = onConfirm, enabled = !draft.submitting) {
                        Icon(Icons.Outlined.Check, contentDescription = "Confirm")
                    }
                    IconButton(onClick = onCancel, enabled = !draft.submitting) {
                        Icon(Icons.Outlined.Close, contentDescription = "Cancel")
                    }
                }
            }
            if (draft.submitting) {
                LinearProgressIndicator(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
            draft.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/* ---------------- 数字滚轮时间选择（草稿用） ---------------- */

@Composable
private fun TimePickers(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface, RectangleShape)
            .padding(4.dp)
    ) {
        AndroidView(
            factory = { ctx ->
                NumberPicker(ctx).apply {
                    minValue = 0; maxValue = 23
                    value = hour
                    setOnValueChangedListener { _, _, newVal -> onHourChange(newVal) }
                    wrapSelectorWheel = true
                }
            },
            update = { it.value = hour }
        )
        Spacer(Modifier.width(4.dp))
        AndroidView(
            factory = { ctx ->
                NumberPicker(ctx).apply {
                    minValue = 0; maxValue = 59
                    value = minute
                    setOnValueChangedListener { _, _, newVal -> onMinuteChange(newVal) }
                    wrapSelectorWheel = true
                }
            },
            update = { it.value = minute }
        )
        Spacer(Modifier.width(8.dp))
        Text("%02d:%02d".format(hour, minute), fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun AddCard(onClick: () -> Unit) {
    Surface(
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
            Text("+  Add new plan")
        }
    }
}
