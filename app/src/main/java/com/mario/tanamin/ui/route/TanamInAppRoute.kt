package com.mario.tanamin.ui.route

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map // Icon untuk Course
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
import com.mario.tanamin.ui.view.CourseView // Import CourseView
import com.mario.tanamin.ui.view.LoginView
//import com.mario.tanamin.ui.view.HomeView
import com.mario.tanamin.ui.view.WalletView
import com.mario.tanamin.ui.view.ProfileView
import com.mario.tanamin.ui.view.PocketDetailView

enum class AppView(val title: String, val icon: ImageVector? = null) {
    Login("Login"),
    Home("Home", Icons.Filled.Home),
    Wallet("Wallet", Icons.Filled.AccountBalanceWallet),
    Course("Course", Icons.Filled.Map), // Menambahkan Enum Course
    Profile("Profile", Icons.Filled.Person)
}

data class BottomNavItem(val view: AppView, val label: String)

@Composable
fun MyBottomNavBar(
    navController: NavHostController,
    currentDestination: NavDestination?,
    items: List<BottomNavItem>
) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TanamInAppRoute() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    // Check if current route is PocketDetail
    val isPocketDetailView = currentRoute?.startsWith("PocketDetail/") == true

    // Menambahkan Course ke list menu bawah
    val bottomNavItems = listOf(
        BottomNavItem(AppView.Home, "Home"),
        BottomNavItem(AppView.Wallet, "Wallets"),
        BottomNavItem(AppView.Course, "Course"),
        BottomNavItem(AppView.Profile, "Profile")
    )

    // Check for showBottomBar
     val showBottomBar = bottomNavItems.any { it.view.name == currentRoute }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    MyBottomNavBar(
                        navController = navController,
                        currentDestination = currentDestination,
                        items = bottomNavItems
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                modifier = Modifier.padding(innerPadding),
                navController = navController,
                startDestination = AppView.Login.name // Start di Login
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
                    Text("Home Screen Placeholder. Route: $currentRoute, ShowBar: $showBottomBar", modifier = Modifier.padding(50.dp))
                }
                composable(route = AppView.Wallet.name) {
                    WalletView(navController = navController)
                }
                // Mendaftarkan CourseView ke NavHost
                composable(route = AppView.Course.name) {
                    CourseView(navController = navController)
                }
                composable(route = AppView.Profile.name) {
                    ProfileView(navController = navController)
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
            }
        }

        // Floating back button for PocketDetail view
        if (isPocketDetailView) {
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
                    tint = Color(0xFFFFB86C)
                )
            }
        }
    }
}
