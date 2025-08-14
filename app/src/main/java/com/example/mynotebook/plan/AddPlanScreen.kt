package com.example.mynotebook.plan

import android.app.DatePickerDialog
import android.widget.NumberPicker
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.compose.ui.viewinterop.AndroidView
import com.example.mynotebook.api.PlanItem
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Plans") },
                actions = {
                    TextButton(onClick = {
                        if (ui.createdAny) {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("plan_added", true)   // Today 页会刷新并弹 “Add successfully!”
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
            // 顶部：日期选择
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

            // 主体：LazyColumn — 已有计划 + 草稿编辑
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section: 已有计划
                if (ui.existing.isNotEmpty()) {
                    item { Text("Plans of the day", style = MaterialTheme.typography.titleMedium) }
                    items(ui.existing, key = { it.id }) { item ->
                        ExistingPlanRow(item)
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

                // 小贴士：没有草稿时给个“点击右下角 + 添加”的提示

                item {
                    AddCard(onClick = { vm.addDraft() })
                }
            }
        }
    }
}

@Composable
private fun ExistingPlanRow(item: PlanItem) {
    Surface(
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "%02d:%02d".format(item.hour ?: 0, item.minute ?: 0),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.width(72.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title ?: "(Untitled)", style = MaterialTheme.typography.titleMedium)
                item.details?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if ((item.alarm ?: 0) == 1) {
                Spacer(Modifier.width(8.dp))
                Text("⏰")
            }
        }
    }
}

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

/** 上下滚动的时间选择器（每个草稿一套） */
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