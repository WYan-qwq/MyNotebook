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
                                nav.navigate("today/${user.id}") {
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
                                nav.navigate("today/${user.id}") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onBackToLogin = { nav.popBackStack() }
                        )
                    }

                    composable(
                        route = "today/{userId}",
                        arguments = listOf(navArgument("userId") { type = NavType.IntType })
                    ) { backStack ->
                        val userId = backStack.arguments?.getInt("userId") ?: 0
                        TodayPlansRoute(userId = userId)
                    }
                }
            }
        }
    }
}