package com.example.mynotebook.me

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.mynotebook.BuildConfig
import com.example.mynotebook.R
import com.example.mynotebook.api.ProfileResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeRoute(
    userId: Int,
    onOpenAccount: () -> Unit,
    appNav: NavHostController,
    vm: MeViewModel = viewModel()
) {
    val ui by vm.ui.collectAsState()

    LaunchedEffect(userId) { vm.load(userId) }

    var showFeedback by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Me") }) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                ui.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                ui.error != null -> Text(
                    ui.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> {
                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        Header(profile = ui.profile)

                        Spacer(Modifier.height(16.dp))
                        SettingsItem(title = "My account", onClick = onOpenAccount)
                        SettingsItem(title = "Help", onClick = { showHelp = true })
                        SettingsItem(title = "Feedback", onClick = { showFeedback = true })
                        SettingsItem(title = "Logout", onClick = {
                            // 直接跳到登录界面，并清空返回栈，避免点返回又回到“我”页
                            appNav.navigate("login") {
                                popUpTo(appNav.graph.findStartDestination().id) {
                                    inclusive = false  // 起点是 login，本身不要移除
                                }
                                launchSingleTop = true
                            }}
                        )
                    }
                }
            }
        }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("Help") },
            text = { Text("A quick guide will be added later. Stay tuned!") },
            confirmButton = { TextButton(onClick = { showHelp = false }) { Text("OK") } }
        )
    }

    if (showFeedback) {
        FeedbackDialog(onDismiss = { showFeedback = false })
    }
}

@Composable
private fun Header(profile: ProfileResponse?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val size = 72.dp
        if (profile?.picture.isNullOrBlank()) {
            Image(
                painter = androidx.compose.ui.res.painterResource(R.drawable.init),
                contentDescription = null,
                modifier = Modifier.size(size).clip(CircleShape)
            )
        } else {
            AsyncImage(
                model = toFullUrl(profile!!.picture),
                contentDescription = null,
                modifier = Modifier.size(size).clip(CircleShape),
                placeholder = androidx.compose.ui.res.painterResource(R.drawable.init),
                error = androidx.compose.ui.res.painterResource(R.drawable.init)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                profile?.userName?.ifBlank { "User ${profile.id}" } ?: "User ${profile?.id ?: ""}",
                style = MaterialTheme.typography.titleLarge
            )
            profile?.email?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsItem(title: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = { Icon(Icons.Outlined.ChevronRight, contentDescription = null) },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    )
    HorizontalDivider()
}

@Composable
private fun FeedbackDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val email = "wyan17@sheffield.ac.uk"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Feedback") },
        text = { Text("If you have any questions or suggestions,\nplease send an email to $email") },
        confirmButton = {
            TextButton(onClick = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:$email")
                    putExtra(Intent.EXTRA_SUBJECT, "MyNotebook Feedback")
                }
                runCatching { ctx.startActivity(intent) }
                    .onFailure { Toast.makeText(ctx, "No email app found", Toast.LENGTH_SHORT).show() }
                onDismiss()
            }) { Text("Email") }
        },
        dismissButton = {
            TextButton(onClick = {
                clipboard.setText(AnnotatedString(email))
                Toast.makeText(ctx, "Email copied", Toast.LENGTH_SHORT).show()
                onDismiss()
            }) { Text("Copy") }
        }
    )
}

/** 把相对路径转成完整 URL（例如 `/files/avatars/a.jpg` -> `BASE_URL/files/avatars/a.jpg`） */
private fun toFullUrl(path: String?): String? {
    if (path.isNullOrBlank()) return null
    val p = path.trim()
    return if (p.startsWith("http", ignoreCase = true)) p
    else BuildConfig.BASE_URL.trimEnd('/') + "/" + p.trimStart('/')
}
