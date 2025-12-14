package com.mario.tanamin.ui.route

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mario.tanamin.ui.view.LoginView
//import com.mario.tanamin.ui.view.HomeView
import com.mario.tanamin.ui.view.WalletView
import com.mario.tanamin.ui.viewmodel.LoginViewModel

enum class AppView(val title: String) {
    Login("Login"),
    Home("Home"),
    Wallet("Wallet")
}

@Composable
fun TanamInAppRoute() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            FloatingBottomBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    if (route != null && route != currentRoute) {
                        navController.navigate(route) {
                            popUpTo(AppView.Login.name) { inclusive = false }
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
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
//                    HomeView(navController = navController)
                }
                composable(route = AppView.Wallet.name) {
                    WalletView(navController = navController)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTopAppBar(
    title: String,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF1A1A1A)
        ),
        modifier = modifier
    )
}

@Composable
fun FloatingBottomBar(
    currentRoute: String?,
    onNavigate: (String?) -> Unit
) {
    val items = listOf(
        Triple(AppView.Home.name, "Home", Icons.Default.Home),
        Triple(AppView.Wallet.name, "Wallet", Icons.Default.AccountBalanceWallet)
    )
    // Only show on Home/Wallet, not Login
    if (currentRoute == AppView.Login.name) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            shape = MaterialTheme.shapes.large,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { (route, label, icon) ->
                    NavigationBarItem(
                        selected = currentRoute == route,
                        onClick = { onNavigate(route) },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (currentRoute == route) Color(0xFFFF9800) else Color(0xFFBDBDBD)
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                color = if (currentRoute == route) Color(0xFFFF9800) else Color(0xFFBDBDBD),
                                fontSize = 12.sp
                            )
                        },
                        alwaysShowLabel = true
                    )
                }
            }
        }
    }
}
