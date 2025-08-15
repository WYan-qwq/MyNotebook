package com.example.mynotebook.plan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.mynotebook.api.PlanItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExistingPlanEditableRow(
    plan: PlanItem,
    onSaveEdit: (id: Int, hour: Int, minute: Int, title: String, details: String?, alarm: Int) -> Unit,
    modifier: Modifier = Modifier
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

    // 编辑态临时值（无时间限制）
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
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(Modifier.padding(16.dp)) {
            // 顶部：时间 + 标题 + 完成状态 + 下拉箭头
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "%02d:%02d".format(hour, minute),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.width(72.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    title.ifBlank { "(Untitled)" },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                if ((plan.finished ?: 0) == 1) {
                    Text("✓", color = MaterialTheme.colorScheme.primary)
                } else {
                    Text("×", color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Outlined.ExpandMore, contentDescription = null, modifier = Modifier.rotate(rotate))
            }

            // 展开区域：详情/编辑
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))

                    if (!editing) {
                        // 只读详情
                        plan.details?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(6.dp))
                        }
                        if ((plan.alarm ?: 0) == 1) {
                            Text("⏰ Alarm set", style = MaterialTheme.typography.labelMedium)
                        }
                        // 右下角 Edit（无背景）
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
                        // 编辑 UI —— 与 Add 一致，但时间无限制
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row {
                                // Hour
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
                                        (0..23).forEach {
                                            DropdownMenuItem(
                                                text = { Text("%02d".format(it)) },
                                                onClick = { hour = it; hourMenu = false }
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                // Minute
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