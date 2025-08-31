package com.example.mynotebook.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mynotebook.plan.AddPlanScreen
import com.example.mynotebook.plan.TodayPlansRoute
import com.example.mynotebook.share.ShareDetailScreen
import com.example.mynotebook.share.ShareRoute
import com.example.mynotebook.share.ShareViewModel
import com.example.mynotebook.week.MonthCalendarScreen
import com.example.mynotebook.week.WeekRoute

@Composable
fun HomeRoot(userId: Int) {
    val innerNav = rememberNavController()
    val backStackEntry by innerNav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route.orEmpty()
    val snackbarHostState = remember { SnackbarHostState() }
    val shareViewModel: ShareViewModel = viewModel()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },

        // ✅ 用 BottomAppBar 托住底部操作，但不在这里放 FAB
        bottomBar = {
            BottomAppBar(
                actions = {
                    BottomBarAction(
                        tab = HomeTab.Today,
                        selected = currentRoute.startsWith(HomeTab.Today.route),
                        onClick = {
                            innerNav.navigate(HomeTab.Today.route) {
                                popUpTo(innerNav.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    BottomBarAction(
                        tab = HomeTab.Week,
                        selected = currentRoute.startsWith(HomeTab.Week.route),
                        onClick = {
                            innerNav.navigate(HomeTab.Week.route) {
                                popUpTo(innerNav.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    Spacer(Modifier.weight(1f))
                    BottomBarAction(
                        tab = HomeTab.Share,
                        selected = currentRoute.startsWith(HomeTab.Share.route),
                        onClick = {
                            innerNav.navigate(HomeTab.Share.route) {
                                popUpTo(innerNav.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    BottomBarAction(
                        tab = HomeTab.Me,
                        selected = currentRoute.startsWith(HomeTab.Me.route),
                        onClick = {
                            innerNav.navigate(HomeTab.Me.route) {
                                popUpTo(innerNav.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            )
        },

        // ✅ 把 FAB 放到 Scaffold，并指定居中；再向下偏移一点形成“镶嵌”
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = { innerNav.navigate("add") },
                modifier = Modifier
                    .offset(y = 65.dp)           // 下沉到 BottomAppBar 里，数值可微调 18–32dp
                    .navigationBarsPadding()     // 避免被系统手势条顶住
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add")
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        NavHost(
            navController = innerNav,
            startDestination = HomeTab.Today.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(HomeTab.Today.route) {
                TodayPlansRoute(
                    userId = userId,
                    navController = innerNav,
                    snackbarHostState = snackbarHostState
                )
            }
            composable(HomeTab.Week.route)  {
                WeekRoute(
                    userId = userId,
                    navController = innerNav,                      // ✅ 传入
                    onNavigateToCalendar = { innerNav.navigate("calendar") }
                )
            }
            composable("calendar") {
                MonthCalendarScreen(navController = innerNav)
            }
            composable("add") {
                AddPlanScreen(
                    userId = userId,
                    navController = innerNav,
                    onDone = { innerNav.popBackStack() }
                )
            }
            composable(HomeTab.Share.route) { ShareRoute(
                vm = shareViewModel,
                userId = userId,
                onOpenDetail = { id ->
                    innerNav.navigate("shareDetail/$id")
                }
            ) }
            composable(
                route = "shareDetail/{id}",
                arguments = listOf(navArgument("id") { type = NavType.IntType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments!!.getInt("id")
                ShareDetailScreen(
                    shareId = id,
                    vm = shareViewModel,          // 复用同一个 VM，点赞状态能同步
                    onBack = { innerNav.popBackStack() }
                )
            }
            composable(HomeTab.Me.route)    { ProfileStub() }
        }
    }
}

@Composable
private fun BottomBarAction(
    tab: HomeTab,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = if (selected) MaterialTheme.colorScheme.primary else LocalContentColor.current
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(contentColor = color),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(tab.icon, contentDescription = tab.title)
            Text(
                tab.title,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable private fun ShareStub() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("sharing（TODO）") }
}
@Composable private fun ProfileStub() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("me（TODO）") }
}