package com.mario.tanamin.ui.route

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
import com.mario.tanamin.ui.view.LoginView
import com.mario.tanamin.ui.view.WalletView
import com.mario.tanamin.ui.view.PocketDetailView

enum class AppView(val title: String, val icon: ImageVector? = null) {
    Login("Login"),
    Home("Home", Icons.Filled.Home),
    Wallet("Wallet", Icons.Filled.AccountBalanceWallet)
}

data class BottomNavItem(val view: AppView, val label: String)

@Composable
fun TanamInAppRoute() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem(AppView.Home, "Home"),
        BottomNavItem(AppView.Wallet, "Wallet")
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
            startDestination = AppView.Login.name
        ) {
            composable(route = AppView.Login.name) {
                LoginView(
                    navController = navController,
                    loginViewModel = viewModel()
                )
            }
            composable(route = AppView.Home.name) {
//                HomeView(navController = navController)
            }
            composable(route = AppView.Wallet.name) {
                WalletView(navController = navController)
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
                        Text("Invalid pocket ID", color = Color.Red)
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
    // Only show bottom bar if not on Login
    if (items.any { it.view.name == currentDestination?.route }) {
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
                color = Color(0xFF222B45)
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF222B45)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFFFFB86C)
        )
    )
}

