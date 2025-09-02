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
import com.example.mynotebook.me.AccountScreen
import com.example.mynotebook.me.MeRoute
import com.example.mynotebook.plan.AddPlanScreen
import com.example.mynotebook.plan.TodayPlansRoute
import com.example.mynotebook.share.ShareCreateScreen
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
            // 小号 FAB，更接近你第二张图的大小
            SmallFloatingActionButton(
                onClick = { innerNav.navigate("add") },
                modifier = Modifier
                    // 略微下沉一点点，让它更贴近底栏（按需要微调 6~14dp）
                    .offset(y = 50.dp)
            ) {
                // 再把内部的 + 图标也略缩小
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = "Add",
                    modifier = Modifier.size(40.dp)   // 默认 24dp，缩小到 18dp
                )
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
                    onNavigateToCalendar = { innerNav.navigate("calendar") },
                    onShareDay = { date -> innerNav.navigate("shareCreate/$date") }
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
                    userId = userId,
                    vm = shareViewModel,          // 复用同一个 VM，点赞状态能同步
                    onBack = { innerNav.popBackStack() }
                )
            }
            composable(
                route = "shareCreate/{date}",
                arguments = listOf(navArgument("date") { type = NavType.StringType })
            ) { backStackEntry ->
                val date = backStackEntry.arguments!!.getString("date")!!
                ShareCreateScreen(
                    userId = userId,
                    date = date,
                    navController = innerNav,
                    shareVm = shareViewModel,
                    onDone = {
                        innerNav.popBackStack()
                        // 成功后跳到 Share 列表
                        innerNav.navigate(HomeTab.Share.route) {
                            popUpTo(innerNav.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(HomeTab.Me.route)    { MeRoute(
                userId = userId,
                onOpenMyShare = { innerNav.navigate("myShare") },
                onOpenAccount = { innerNav.navigate("account") }
            )}
            composable("account") {
                AccountScreen(
                    userId = userId,
                    onBack = { innerNav.popBackStack() }
                )
            }

// 你可以先占位一个我的分享页（后续可换成真实筛选）：
            composable("myShare") {
                // 这里暂时跳到 Share 列表并在 ShareRoute 里按 userId 过滤；
                // 若目前 listShares 还不支持 userId，你先放一个占位页：
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("My share (coming soon)")
                }
            }
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