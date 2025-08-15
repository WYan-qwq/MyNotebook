package com.example.mynotebook.plan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.mynotebook.api.PlanItem
import kotlinx.coroutines.launch
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
    var selected by remember { mutableStateOf<PlanItem?>(null) }
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(backStackEntry) {
        val handle = backStackEntry?.savedStateHandle
        if (handle?.get<Boolean>("plan_added") == true) {
            vm.refresh()
            scope.launch { snackbarHostState.showSnackbar("Add successfully!") }
            handle.remove<Boolean>("plan_added") // 防止重复触发
        }
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
                ui.items.isEmpty() -> Text("No plans for today,click + to add plans.", Modifier.align(Alignment.Center))
                else -> LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(ui.items, key = { it.id }) { item ->
                        PlanRow(item) { selected = item }
                        Divider()
                    }
                }
            }

            selected?.let { item ->
                AlertDialog(
                    onDismissRequest = { selected = null },
                    title = { Text(item.title ?: "(Untitled)") },
                    text = {
                        Column {
                            Text("Time: ${formatTime(item.hour, item.minute)}")
                            Spacer(Modifier.height(8.dp))
                            Text(item.details ?: "(No details)")
                        }
                    },
                    confirmButton = { TextButton({ selected = null }) { Text("Close") } }
                )
            }
        }
    }
}

@Composable
private fun PlanRow(item: PlanItem, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            formatTime(item.hour, item.minute),
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(72.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(item.title ?: "(Untitled)", style = MaterialTheme.typography.bodyLarge)
    }
}

private fun formatTime(hour: Int?, minute: Int?): String {
    val h = (hour ?: 0).coerceIn(0, 23)
    val m = (minute ?: 0).coerceIn(0, 59)
    return "%02d:%02d".format(h, m)
}