package com.mario.tanamin.ui.route

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mario.tanamin.ui.view.CourseView
import com.mario.tanamin.ui.view.LoginView
//import com.mario.tanamin.ui.view.HomeView
import com.mario.tanamin.ui.view.WalletView
import com.mario.tanamin.ui.view.ProfileView
import com.mario.tanamin.ui.view.PocketDetailView
import com.mario.tanamin.ui.view.StartQuizView
import com.mario.tanamin.ui.view.ThemeShopScreen
import com.mario.tanamin.ui.viewmodel.AppViewModel

enum class AppView(val title: String, val icon: ImageVector? = null) {
    Login("Login"),
    Home("Home", Icons.Filled.Home),
    Wallet("Wallet", Icons.Filled.AccountBalanceWallet),
    Course("Course", Icons.Filled.Map),
    Profile("Profile", Icons.Filled.Person),
    ThemeShop("ThemeShop", null)

}

data class BottomNavItem(val view: AppView, val label: String)

@Composable
fun MyBottomNavBar(
    navController: NavHostController,
    currentDestination: NavDestination?,
    items: List<BottomNavItem>,
    modifier: Modifier = Modifier
) {
    // Logic: Bottom bar hanya muncul jika route saat ini ada di dalam list bottomNavItems
    // (Artinya: Login tidak akan menampilkan bottom bar)
    val showBottomBar = items.any { it.view.name == currentDestination?.route }

    if (showBottomBar) {
        NavigationBar {
            items.forEach { item ->
                NavigationBarItem(
                    icon = { Icon(item.view.icon!!, contentDescription = item.label) },
                    label = { Text(item.label) },
                    selected = currentDestination?.hierarchy?.any { it.route == item.view.name } == true,
                    onClick = {
                        navController.navigate(item.view.name) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TanamInAppRoute(
    appViewModel: AppViewModel = viewModel() // Default to new instance if not provided, but usually provided by Activity 
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    // Check if current route is PocketDetail
    val isPocketDetailView = currentRoute?.startsWith("PocketDetail/") == true

    // Check if current route is Quiz
    val isStartQuizView = currentRoute?.startsWith("Quiz/") == true

    // Menambahkan Course ke list menu bawah
    val bottomNavItems = listOf(
        BottomNavItem(AppView.Home, "Home"),
        BottomNavItem(AppView.Wallet, "Wallets"),
        BottomNavItem(AppView.Course, "Course"),
        BottomNavItem(AppView.Profile, "Profile")
    )

    Scaffold(
        bottomBar = {
            MyBottomNavBar(
                navController = navController,
                currentDestination = currentDestination,
                items = bottomNavItems
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                modifier = Modifier.padding(innerPadding),
                navController = navController,
                startDestination = AppView.Login.name,
            ) {
                composable(route = AppView.Login.name) {
                    LoginView(
                        navController = navController,
                        loginViewModel = viewModel()
                    )
                }
                composable(route = AppView.Home.name) {
                    // HomeView(navController = navController)
                    // Sementara text dulu agar tidak error saat navigasi
                    Text("Home Screen Placeholder", modifier = Modifier.padding(50.dp))
                }
                composable(route = AppView.Wallet.name) {
                    WalletView(navController = navController)
                }
                composable(route = AppView.Course.name) {
                    CourseView(navController = navController)
                }
                composable(route = AppView.Profile.name) {
                    ProfileView(navController = navController)
                }
                composable(route = AppView.ThemeShop.name) {
                    // Use the existing appViewModel passed to TanamInAppRoute
                    ThemeShopScreen(
                        navController = navController,
                        appViewModel = appViewModel 
                    )
                }
                composable(
                    route = "PocketDetail/{pocketId}",
                    arguments = listOf(navArgument("pocketId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val pocketId = backStackEntry.arguments?.getInt("pocketId") ?: 0
                    PocketDetailView(
                        navController = navController,
                        pocketId = pocketId
                    )
                }
                composable(
                    route = "Quiz/{levelId}/{levelName}",
                    arguments = listOf(
                        navArgument("levelId") { type = NavType.IntType },
                        navArgument("levelName") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val levelId = backStackEntry.arguments?.getInt("levelId") ?: 0
                    val levelName = backStackEntry.arguments?.getString("levelName") ?: "Unknown Level"
                    StartQuizView(
                        navController = navController,
                        levelId = levelId,
                        levelName = levelName,
                        viewModel = viewModel()
                    )
                }
            }

            // Floating back button for PocketDetail and StartQuiz view
            if (isPocketDetailView || isStartQuizView) {
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp, top = 60.dp)
                        .size(40.dp)
                        .background(Color.White, shape = CircleShape)
                        .clickable { navController.popBackStack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
