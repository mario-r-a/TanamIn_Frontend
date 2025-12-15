package com.mario.tanamin.ui.route

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map // Icon untuk Course
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.Alignment
import com.mario.tanamin.ui.view.CourseView // Import CourseView
import com.mario.tanamin.ui.view.LoginView
import com.mario.tanamin.ui.view.WalletView
import com.mario.tanamin.ui.view.PocketDetailView
import com.mario.tanamin.ui.view.ProfileView

enum class AppView(val icon: ImageVector? = null) {
    Login(null),
    Home(Icons.Filled.Home),
    Wallet(Icons.Filled.AccountBalanceWallet),
    Course(Icons.Filled.Map), // Menambahkan Enum Course
    Profile(Icons.Filled.Person)
}

data class BottomNavItem(val view: AppView, val label: String)

@Composable
fun TanamInAppRoute() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = navBackStackEntry?.destination?.route

    // Menambahkan Course ke list menu bawah
    val bottomNavItems = listOf(
        BottomNavItem(AppView.Home, "Home"),
        BottomNavItem(AppView.Wallet, "Wallets"),
        BottomNavItem(AppView.Course, "Course")
    )

    // Determine if we should show TopBar (only for PocketDetail)
    val showTopBar = currentRoute?.startsWith("PocketDetail/") == true

    // Get pocket name for TopBar title if on PocketDetail
    val topBarTitle = if (showTopBar) {
        "Pocket Details"
    } else {
        ""
    }

    Scaffold(
        topBar = {
            if (showTopBar) {
                PocketDetailTopBar(
                    title = topBarTitle,
                    onBackClick = { navController.navigateUp() }
                )
            }
        },
        bottomBar = {
            MyBottomNavigationBar(
                navController = navController,
                currentDestination = currentDestination,
                items = bottomNavItems
            )
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
                Text("Home Screen Placeholder", modifier = Modifier.padding(50.dp))
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
            composable(route = "PocketDetail/{pocketId}") { backStackEntry ->
                val pocketId = backStackEntry.arguments?.getString("pocketId")?.toIntOrNull()
                if (pocketId != null) {
                    PocketDetailView(
                        navController = navController,
                        pocketId = pocketId
                    )
                } else {
                    // Error: invalid pocket ID
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Invalid pocket ID", color = androidx.compose.ui.graphics.Color.Red)
                    }
                }
            }
        }
    }
}

    @Composable
    fun MyBottomNavigationBar(
        navController: NavHostController,
        currentDestination: NavDestination?,
        items: List<BottomNavItem>
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
    fun PocketDetailTopBar(
        title: String,
        onBackClick: () -> Unit
    ) {
        TopAppBar(
            title = {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color(0xFF222B45)
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = androidx.compose.ui.graphics.Color(0xFF222B45)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = androidx.compose.ui.graphics.Color(0xFFFFB86C)
            )
        )
    }

