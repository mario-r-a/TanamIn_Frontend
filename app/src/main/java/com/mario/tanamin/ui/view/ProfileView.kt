package com.mario.tanamin.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.mario.tanamin.ui.viewmodel.ProfileUiState
import com.mario.tanamin.ui.viewmodel.ProfileViewModel

@Composable
fun ProfileView(
    navController: NavController,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Top Gradient matching WalletView but adapted if needed (User image looks similar)
    val topGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFFFB86C), Color(0xFFFFE3A3), Color.White),
        startY = 0f,
        endY = 520f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Gradient Header Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(topGradient)
                .align(Alignment.TopCenter)
        ) {}

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Profile",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF222B45)
                    )
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color(0xFF222B45),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                when (val state = uiState) {
                    is ProfileUiState.Loading -> {
                        Box(modifier = Modifier.height(500.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is ProfileUiState.Error -> {
                        Box(modifier = Modifier.height(500.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Error: ${state.message}", color = Color.Red)
                                Button(onClick = { viewModel.fetchProfile() }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                    is ProfileUiState.Success -> {
                        val profile = state.profile
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE0E0E0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = profile.name.firstOrNull()?.toString()?.uppercase() ?: "U",
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // Active Theme Card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFB86C)), // Orange
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = "Active Theme",
                                        tint = Color(0xFF222B45)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Active Theme",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF222B45),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "Default", // In future map profile.activeThemeId to Name
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Normal,
                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                        color = Color(0xFF222B45)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Profile Information Card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFB86C)),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Profile Information",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF222B45)
                                        )
                                        Text(
                                            text = "Edit",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF222B45)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    ProfileField(label = "Name", value = profile.name)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    ProfileField(label = "Email", value = profile.email)
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // Theme Shop Card
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(Color(0xFF66BB6A), Color(0xFFFFCC80)) // Green to Orange
                                        )
                                    )
                                    .clickable { 
                                        // Update this with actual route later
                                        // navController.navigate("themeshop") 
                                    }
                                    .padding(horizontal = 24.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.ShoppingBag,
                                            contentDescription = "Theme Shop",
                                            tint = Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                            text = "Theme Shop",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Go to Shop",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileField(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF222B45)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFFFE3A3).copy(alpha = 0.5f))
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            Text(
                text = value,
                fontSize = 14.sp,
                color = Color(0xFF222B45)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileViewPreview() {
    MaterialTheme {
        ProfileView(navController = rememberNavController())
    }
}
