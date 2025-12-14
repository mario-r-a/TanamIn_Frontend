package com.mario.tanamin.ui.route

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

    val topBarTitle = when (currentRoute) {
        AppView.Login.name -> AppView.Login.title
        AppView.Home.name -> AppView.Home.title
        AppView.Wallet.name -> AppView.Wallet.title
        else -> "TanamIn"
    }

    Scaffold(
        topBar = {
            MyTopAppBar(title = topBarTitle)
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