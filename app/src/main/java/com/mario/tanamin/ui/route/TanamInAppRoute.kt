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

enum class AppView(val icon: ImageVector? = null) {
    Login(null),
    Home(Icons.Filled.Home),
    Wallet(Icons.Filled.AccountBalanceWallet),
    Course(Icons.Filled.Map),
    Profile(Icons.Filled.Person)
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
        // Floating pill-style bottom bar with shadow and rounded corners
        // Keep navigation logic identical to previous implementation
        val selectedColor = Color(0xFFFFB86C) // accent orange
        val unselectedColor = Color(0xFFBDBDBD) // muted grey
        Surface(
            shape = RoundedCornerShape(36.dp),
            color = Color(0xFFF7F6F2),
            tonalElevation = 8.dp,
            shadowElevation = 10.dp,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(84.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.view.name } == true
                    val iconTint by animateColorAsState(if (selected) selectedColor else unselectedColor)
                    val labelColor by animateColorAsState(if (selected) selectedColor else Color(0xFF7A7A7A))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                navController.navigate(item.view.name) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = item.view.icon ?: Icons.Filled.Home,
                            contentDescription = item.label,
                            tint = iconTint,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.label,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = labelColor
                        )
                    }
                }
            }
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

    // Check if current route is Quiz
    val isStartQuizView = currentRoute?.startsWith("Quiz/") == true

    // Menambahkan Course ke list menu bawah
    val bottomNavItems = listOf(
        BottomNavItem(AppView.Home, "Home"),
        BottomNavItem(AppView.Wallet, "Wallets"),
        BottomNavItem(AppView.Course, "Course"),
        BottomNavItem(AppView.Profile, "Profile")
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // NavHost tanpa Scaffold, sehingga konten tidak dibatasi oleh bottomBar padding
        NavHost(
            navController = navController,
            startDestination = AppView.Login.name, // Start di Login
            modifier = Modifier.fillMaxSize()
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
                //route = "Quiz/{levelId}", coba-coba ganti
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

        // Navbar di-overlay di atas konten (floating on top)
        MyBottomNavBar(
            navController = navController,
            currentDestination = currentDestination,
            items = bottomNavItems,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

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
                    tint = Color(0xFFFFB86C)
                )
            }
        }
    }
}
