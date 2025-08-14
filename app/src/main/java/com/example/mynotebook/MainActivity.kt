package com.example.mynotebook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.rememberNavController
import com.example.mynotebook.auth.LoginViewModel
import com.example.mynotebook.auth.RegisterViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.mynotebook.auth.LoginScreen
import com.example.mynotebook.auth.RegisterScreen
import com.example.mynotebook.home.HomeRoot
import com.example.mynotebook.plan.TodayPlansRoute

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
                            onLoggedIn = { user ->
                                // ⚠️ 这里一定要传 id（Int）
                                nav.navigate("home/${user.id}") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("register") {
                        val vm: RegisterViewModel = viewModel()
                        RegisterScreen(
                            vm = vm,
                            onRegistered = { user ->
                                nav.navigate("home/${user.id}") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onBackToLogin = { nav.popBackStack() }
                        )
                    }

                    composable(
                        route = "home/{userId}",
                        arguments = listOf(navArgument("userId") { type = NavType.IntType })
                    ) { backStack ->
                        val userId = backStack.arguments?.getInt("userId") ?: 0
                        HomeRoot(userId = userId)
                    }
                }
            }
        }
    }
}
