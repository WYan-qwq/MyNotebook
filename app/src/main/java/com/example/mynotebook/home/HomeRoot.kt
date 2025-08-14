package com.example.mynotebook.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mynotebook.plan.TodayPlansRoute

@Composable
fun HomeRoot(userId: Int) {
    val innerNav = rememberNavController()
    val backStackEntry by innerNav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route.orEmpty()

    Scaffold(
        bottomBar = {
            NavigationBar {
                HomeTab.bottomItems.forEach { tab: HomeTab ->
                    NavigationBarItem(
                        selected = currentRoute.startsWith(tab.route),
                        onClick = {
                            innerNav.navigate(tab.route) {
                                popUpTo(innerNav.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) }
                    )
                }
            }
        },
        floatingActionButton = {
            // 中间加号：不要放到底栏 items 里，这里单独做
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .size(56.dp)
                    .shadow(8.dp, MaterialTheme.shapes.medium)
                    .clickable { innerNav.navigate("add") }
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Add, contentDescription = "添加计划")
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        NavHost(
            navController = innerNav,
            startDestination = HomeTab.Today.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(HomeTab.Today.route) { TodayPlansRoute(userId = userId) }
            composable(HomeTab.Week.route)  { WeekStub() }
            composable("add")               { AddPlanStub(onDone = { innerNav.popBackStack() }) }
            composable(HomeTab.Share.route) { ShareStub() }
            composable(HomeTab.Me.route)    { ProfileStub() }
        }
    }
}

@Composable private fun WeekStub() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("按周查看（TODO）") } }
@Composable private fun ShareStub() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("分享计划（TODO）") } }
@Composable private fun ProfileStub() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("我的（TODO）") } }
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun AddPlanStub(onDone: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("添加计划") }) }) { p ->
        Box(Modifier.padding(p), contentAlignment = Alignment.Center) {
            Button(onClick = onDone) { Text("保存（占位）") }
        }
    }
}