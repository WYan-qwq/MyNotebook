package com.example.mynotebook.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mynotebook.api.UserResponse

@Composable
fun RegisterScreen(
    vm: RegisterViewModel,
    onRegistered: (UserResponse) -> Unit,
    onBackToLogin: () -> Unit
) {
    val ui by vm.ui.collectAsState()

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Create your account", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = ui.email, onValueChange = vm::onEmailChange,
                modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Email") }
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = ui.password, onValueChange = vm::onPasswordChange,
                modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Password") }
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = ui.userName, onValueChange = vm::onUserNameChange,
                modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Name (optional)") }
            )
            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { vm.register(onRegistered) },
                enabled = !ui.loading && ui.email.isNotBlank() && ui.password.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (ui.loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Sign up")
            }

            TextButton(onClick = onBackToLogin) { Text("Back to login") }

            ui.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}