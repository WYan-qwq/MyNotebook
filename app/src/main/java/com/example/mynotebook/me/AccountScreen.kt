package com.example.mynotebook.me

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.mynotebook.R
import com.example.mynotebook.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    userId: Int,
    vm: MeViewModel,
    onBack: () -> Unit
) {
    val ui by vm.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current   // ✅ 需要 ContentResolver

    // 进入页面拉取资料
    LaunchedEffect(userId) { vm.load(userId) }

    // 选择相册图片
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { vm.uploadAvatar(userId, it, context.contentResolver) } // ✅ 传入 cr
    }

    // toast -> Snackbar
    LaunchedEffect(ui.toast) {
        ui.toast?.let {
            snackbar.showSnackbar(it)
            vm.clearToast()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My account") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 头像 + 相机按钮
            Box(contentAlignment = Alignment.BottomEnd) {
                val avatar = ui.profile?.picture
                if (avatar.isNullOrBlank()) {
                    Image(
                        painter = painterResource(R.drawable.init),
                        contentDescription = "avatar",
                        modifier = Modifier.size(96.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    AsyncImage(
                        model = toFullUrl(avatar),
                        contentDescription = "avatar",
                        modifier = Modifier.size(96.dp).clip(CircleShape),
                        placeholder = painterResource(R.drawable.init),
                        error = painterResource(R.drawable.init),
                        contentScale = ContentScale.Crop
                    )
                }

                AssistChip(
                    onClick = { pickImageLauncher.launch("image/*") },
                    label = { Text("Change") },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                )
            }

            Spacer(Modifier.height(24.dp))

            // 修改用户名
            OutlinedTextField(
                value = ui.nameInput,
                onValueChange = vm::setName,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Username") },
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { vm.saveName(userId) },
                enabled = !ui.saving && ui.nameInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (ui.saving) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("Save name")
            }

            Spacer(Modifier.height(24.dp))
            Divider()
            Spacer(Modifier.height(24.dp))

            // 修改密码
            OutlinedTextField(
                value = ui.passwordInput,
                onValueChange = vm::setPwd,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("New password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { vm.savePassword(userId) },
                enabled = !ui.saving && ui.passwordInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (ui.saving) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("Save password")
            }

            if (ui.loading) {
                Spacer(Modifier.height(24.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            ui.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/** 把相对路径转成完整 URL（例如 `/files/avatars/a.jpg` -> `BASE_URL/files/avatars/a.jpg`） */
private fun toFullUrl(path: String?): String? {
    if (path.isNullOrBlank()) return null
    val p = path.trim()
    return if (p.startsWith("http", ignoreCase = true)) p
    else BuildConfig.BASE_URL.trimEnd('/') + "/" + p.trimStart('/')
}