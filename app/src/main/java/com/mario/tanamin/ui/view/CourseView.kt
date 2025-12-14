package com.mario.tanamin.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@Composable
fun CourseView(navController: NavController) {
    // Gradient background similar to WalletView styling
    val topGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFfeac57), Color(0xFFffca91), Color.White),
        startY = 0f,
        endY = 400f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Gradient overlay at the top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(topGradient)
                .align(Alignment.TopCenter)
        ) {}

        // Main Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // 1. Top Bar (Title & Settings)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Courses",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF222B45)
                )
                // Settings Icon
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color(0xFF222B45),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Stats Row (Streak & Coins)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    title = "Streak",
                    value = "0 Days",
                    icon = Icons.Default.LocalFireDepartment,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Coins",
                    value = "0",
                    icon = Icons.Default.Diamond,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 4. Recommended Courses Title
            Text(
                text = "Recommended Courses",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222B45)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 5. Course Levels (The Winding Path)
            // Menggunakan loop atau manual call untuk 5 level
            // Level 1 (Left - Orange)
            CourseLevelCard(
                level = 1,
                title = "Investment Basics",
                colorTheme = Color(0xFFFFB86C), // Orange
                buttonColor = Color(0xFF8CD87D), // Greenish button
                isLeftAligned = true
            )

            PathConnectorDots() // Dots between levels

            // Level 2 (Right - Green)
            CourseLevelCard(
                level = 2,
                title = "Investment Basics",
                colorTheme = Color(0xFF8CD87D), // Green
                buttonColor = Color(0xFF425E44), // Darker Green button
                isLeftAligned = false
            )

            PathConnectorDots()

            // Level 3 (Left - Orange)
            CourseLevelCard(
                level = 3,
                title = "Investment Basics",
                colorTheme = Color(0xFFFFB86C),
                buttonColor = Color(0xFF8CD87D),
                isLeftAligned = true
            )

            PathConnectorDots()

            // Level 4 (Right - Green)
            CourseLevelCard(
                level = 4,
                title = "Investment Basics",
                colorTheme = Color(0xFF8CD87D),
                buttonColor = Color(0xFF425E44),
                isLeftAligned = false
            )

            PathConnectorDots()

            // Level 5 (Left - Orange/Blue variation? Let's stick to Orange pattern)
            CourseLevelCard(
                level = 5,
                title = "Investment Basics",
                colorTheme = Color(0xFFFFB86C),
                buttonColor = Color(0xFF8CD87D),
                isLeftAligned = true
            )

            // Bottom padding for scrolling
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

// --- Reusable Components ---

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFFB86C)), // Orange base
        contentAlignment = Alignment.CenterStart
    ) {
        // Decorative overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.2f))
                    )
                )
        )
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF222B45),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    color = Color(0xFF222B45),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
                    fontSize = 18.sp,
                    color = Color(0xFF222B45),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CourseLevelCard(
    level: Int,
    title: String,
    colorTheme: Color,
    buttonColor: Color,
    isLeftAligned: Boolean
) {
    // Card Width logic: The card takes about 60-70% of width, remaining is spacer
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isLeftAligned) {
            Spacer(modifier = Modifier.weight(0.4f)) // Push to right
        }

        // The Card Content
        Box(
            modifier = Modifier
                .weight(1f) // Takes more space
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(colorTheme)
        ) {
            // Decorative Circle (Top Right)
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 20.dp, y = (-20).dp)
                    .background(Color.White.copy(alpha = 0.2f), shape = CircleShape)
            )
            // Decorative Circle (Bottom Left)
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-10).dp, y = 10.dp)
                    .background(Color.White.copy(alpha = 0.2f), shape = CircleShape)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Complete courses to earn gems!",
                    fontSize = 11.sp,
                    color = Color(0xFF222B45),
                    fontWeight = FontWeight.Medium
                )

                Column {
                    Text(
                        text = "Level $level",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF222B45)
                    )
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF222B45)
                    )
                }

                Button(
                    onClick = { /* Handle Start Course */ },
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = "Start Course",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        if (isLeftAligned) {
            Spacer(modifier = Modifier.weight(0.4f)) // Push content to left
        }
    }
}

@Composable
fun PathConnectorDots() {
    // Creates the visual "..." connection between cards
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray.copy(alpha = 0.5f))
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CoursePreview() {
    CourseView(navController = rememberNavController())
}