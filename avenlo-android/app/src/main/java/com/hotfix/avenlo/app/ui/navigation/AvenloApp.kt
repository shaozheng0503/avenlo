package com.hotfix.avenlo.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hotfix.avenlo.app.ui.screens.CaptureScreen
import com.hotfix.avenlo.app.ui.screens.CollectionsScreen
import com.hotfix.avenlo.app.ui.screens.DetailScreen
import com.hotfix.avenlo.app.ui.screens.HomeScreen
import com.hotfix.avenlo.app.ui.screens.MineScreen
import com.hotfix.avenlo.app.ui.screens.ReviewScreen
import com.hotfix.avenlo.app.ui.screens.SearchScreen
import com.hotfix.avenlo.app.ui.screens.SplashScreen
import com.hotfix.avenlo.app.ui.theme.AvenloTokens

/** 底部导航定义：房子/圆点/图表/人像（图标按 .fig 视觉），标签为推断值 */
private data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val tabs = listOf(
    BottomTab(Routes.HOME, "首页", Icons.Filled.Home),
    BottomTab(Routes.REVIEW, "记录", Icons.Filled.PlayCircle),
    BottomTab(Routes.REVIEW, "统计", Icons.Filled.BarChart),
    BottomTab(Routes.MINE, "我的", Icons.Filled.Person),
)

@Composable
fun AvenloApp() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    // 顶层 Tab 页集合；详情/搜索/灵感集/捕捉为二级页，无底导
    val tabRoutes = listOf(Routes.HOME, Routes.REVIEW, Routes.MINE)
    val showBottomBar = currentRoute in tabRoutes

    Scaffold(
        containerColor = AvenloTokens.Bg,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = AvenloTokens.Surface, tonalElevation = 0.dp) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AvenloTokens.Primary,
                                selectedTextColor = AvenloTokens.Primary,
                                unselectedIconColor = AvenloTokens.TextDisabled,
                                unselectedTextColor = AvenloTokens.TextDisabled,
                                indicatorColor = AvenloTokens.Primary.copy(alpha = 0.10f),
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(bottom = if (showBottomBar) padding.calculateBottomPadding() else 0.dp)) {
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
            ) {
                composable(Routes.SPLASH) { SplashScreen(onStart = { navController.navigate(Routes.HOME) { popUpTo(Routes.SPLASH) { inclusive = true } } }) }
                composable(Routes.HOME) { HomeScreen(navController) }
                composable(Routes.DETAIL) { entry ->
                    val id = entry.arguments?.getString("ideaId") ?: "idea_01"
                    DetailScreen(navController, ideaId = id)
                }
                composable(Routes.COLLECTIONS) { CollectionsScreen(navController) }
                composable(Routes.SEARCH) { SearchScreen(navController) }
                composable(Routes.REVIEW) { ReviewScreen(navController) }
                composable(Routes.MINE) { MineScreen(navController) }
                composable(Routes.CAPTURE) { CaptureScreen(navController) }
            }
        }
    }
}
