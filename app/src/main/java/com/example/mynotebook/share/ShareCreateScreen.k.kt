package com.example.mynotebook.share

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.mynotebook.api.PlanBrief
import com.example.mynotebook.api.RetrofitClient
import com.example.mynotebook.api.ShareCreateRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareCreateScreen(
    userId: Int,
    date: String,                // yyyy-MM-dd
    navController: NavController,
    shareVm: ShareViewModel,
    onDone: () -> Unit           // 成功后回调（跳转到 Share 列表）
) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var plans by remember { mutableStateOf<List<PlanBrief>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    // 拉取这天的计划
    LaunchedEffect(userId, date) {
        loading = true
        error = null
        try {
            val resp = RetrofitClient.api.getPlansByDate(userId, date)
            if (resp.isSuccessful) {
                plans = resp.body().orEmpty()
            } else {
                error = "Load plans failed: ${resp.code()}"
            }
        } catch (e: Exception) {
            error = e.message ?: "Network error"
        } finally {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Share $date") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = details,
                onValueChange = { details = it },
                label = { Text("Details (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Text("Plans on $date", style = MaterialTheme.typography.titleMedium)
            if (loading && plans.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (error != null && plans.isEmpty()) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(plans, key = { it.id }) { PlanPreviewRow(it) }
                    item { Text("... ...  (press to share below)") }
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        loading = true
                        error = null
                        try {
                            val req = ShareCreateRequest(
                                userId = userId,
                                planDate = date,                      // "yyyy-MM-dd"
                                title = title.ifBlank { null },
                                details = details.ifBlank { null }
                            )

                            val resp = RetrofitClient.api.createShare(req)
                            if (!resp.isSuccessful) {
                                throw RuntimeException("Create failed: HTTP ${resp.code()}")
                            }

                            // 创建成功：刷新 Share 列表（带上 userId 以便同步 liked 状态）
                            shareVm.refresh(userId)

                            // 通知外部做导航（HomeRoot 里的 onDone 会 popBackStack + 跳 Share）
                            onDone()
                        } catch (e: Exception) {
                            error = e.message ?: "Network error"
                        } finally {
                            loading = false
                        }
                    }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (loading) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Confirm share")
            }

            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun PlanPreviewRow(p: PlanBrief) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "%02d:%02d".format((p.hour ?: 0).coerceIn(0, 23), (p.minute ?: 0).coerceIn(0, 59)),
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(64.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(p.title?.takeIf { it.isNotBlank() } ?: "(Untitled)")
            p.details?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}