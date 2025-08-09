package com.example.mynotebook

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.mynotebook.auth.LoginViewModel
import com.example.mynotebook.auth.RegisterViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.mynotebook.auth.LoginScreen
import com.example.mynotebook.auth.RegisterScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val nav = rememberNavController()

                NavHost(navController = nav, startDestination = "login") {
                    composable("login") {
                        val vm: LoginViewModel = viewModel()
                        LoginScreen(
                            vm = vm,
                            onNavigateToRegister = { nav.navigate("register") },
                            onLoggedIn = { email -> nav.navigate("home/$email") { popUpTo("login") { inclusive = true } } }
                        )
                    }
                    composable("register") {
                        val vm: RegisterViewModel = viewModel()
                        RegisterScreen(
                            vm = vm,
                            onRegistered = { email -> nav.navigate("home/$email") { popUpTo("login") { inclusive = true } } },
                            onBackToLogin = { nav.popBackStack() }
                        )
                    }
                    composable("home/{email}") { backStackEntry ->
                        val email = backStackEntry.arguments?.getString("email") ?: ""
                        HomeScreen(email = email)
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(email: String) {
    Scaffold { padding ->
        Box(Modifier.padding(padding)) {
            Text(text = "Hello, $email")
        }
    }
}