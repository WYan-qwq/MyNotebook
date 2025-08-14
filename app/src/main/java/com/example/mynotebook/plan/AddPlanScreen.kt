package com.example.mynotebook.plan

import android.app.DatePickerDialog
import android.widget.NumberPicker
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.viewinterop.AndroidView
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlanScreen(
    userId: Int,
    onDone: () -> Unit,
    vm: AddPlanViewModel = viewModel(factory = AddPlanViewModel.provideFactory(userId))
) {
    val ui by vm.ui.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Plan") },
                actions = {
                    if (ui.justCreated != null) {
                        TextButton(onClick = onDone) { Text("Done") }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // Date picker row
            Text("Date", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    val d = ui.date
                    DatePickerDialog(
                        context,
                        { _, y, m, day -> vm.setDate(LocalDate.of(y, m + 1, day)) },
                        d.year, d.monthValue - 1, d.dayOfMonth
                    ).show()
                }
            ) {
                Text(ui.date.format(DateTimeFormatter.ISO_DATE))
            }

            Spacer(Modifier.height(16.dp))
            Text("Todo", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            // “添加一条”的卡片（点击出现编辑框）
            Surface(
                tonalElevation = 1.dp,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { vm.openEditor() }
            ) {
                Box(Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                    Text("+  Add item", style = MaterialTheme.typography.titleLarge)
                }
            }

            // 编辑框
            if (ui.showEditor) {
                Spacer(Modifier.height(16.dp))
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
                            // 左侧：时间滚轮
                            TimePickers(
                                hour = ui.hour,
                                minute = ui.minute,
                                onHourChange = vm::setHour,
                                onMinuteChange = vm::setMinute
                            )
                            Spacer(Modifier.width(12.dp))

                            // 右侧：输入
                            Column(Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = ui.title,
                                    onValueChange = vm::setTitle,
                                    label = { Text("Title") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = ui.details,
                                    onValueChange = vm::setDetails,
                                    label = { Text("Details") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = ui.alarm,
                                        onCheckedChange = { vm.toggleAlarm() }
                                    )
                                    Text("Alarm")
                                }
                            }

                            // 右下角：操作
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                IconButton(
                                    onClick = { vm.submit() },
                                    enabled = !ui.loading
                                ) {
                                    Icon(Icons.Outlined.Check, contentDescription = "Confirm")
                                }
                                IconButton(
                                    onClick = { vm.closeEditor() },
                                    enabled = !ui.loading
                                ) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Cancel")
                                }
                            }
                        }

                        if (ui.loading) {
                            LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
                        }
                        ui.error?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                        ui.justCreated?.let {
                            Text(
                                "Created: ${it.title ?: "(Untitled)"} at %02d:%02d"
                                    .format(it.hour ?: 0, it.minute ?: 0),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 左侧“上下滚动”的时间选择：用 Android 原生 NumberPicker 包一层 */
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