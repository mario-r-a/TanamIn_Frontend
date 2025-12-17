package com.mario.tanamin.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mario.tanamin.ui.viewmodel.CourseViewModel

@Composable
fun CourseView(navController: NavController) {
    val viewModel: CourseViewModel = viewModel()
    val levels by viewModel.levels.collectAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.collectAsState(initial = false)
    val errorMessage by viewModel.errorMessage.collectAsState(initial = null)
    val coins by viewModel.coins.collectAsState(initial = 0)
    val streakCount by viewModel.streak.collectAsState(initial = 0)

    // Reload data ketika user kembali dari quiz
    LaunchedEffect(Unit) {
        viewModel.loadLevels()
        viewModel.loadProfile()
    }

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
        // Gradient overlay at the top - Hiasan
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
                    value = "${streakCount} Days",
                    icon = Icons.Default.LocalFireDepartment,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Coins",
                    value = "$coins",
                    icon = Icons.Default.Paid,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Motivational Text
            Text(
                text = "Consistency is the key to success!",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF222B45)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- Dynamic levels rendering ---
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(text = "Loading levels...", color = Color(0xFF222B45))
                    }
                }
                !errorMessage.isNullOrBlank() -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(text = "Error: ${errorMessage}", color = Color.Red)
                    }
                }
                else -> {
                    if (levels.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text(text = "No levels available", color = Color(0xFF222B45))
                        }
                    } else {
                        levels.forEachIndexed { index, level ->
                            val unlocked = if (index == 0) true else levels.getOrNull(index - 1)?.isCompleted == true

                            val isLeft = index % 2 == 0
                            val colorTheme = if (isLeft) Color(0xFFFFB86C) else Color(0xFF8CD87D)
                            val buttonColor = if (isLeft) Color(0xFFd3842f) else Color(0xFF425E44)

                            CourseLevelCard(
                                level = index + 1,
                                title = level.name,
                                colorTheme = colorTheme,
                                buttonColor = buttonColor,
                                isLeftAligned = isLeft,
                                enabled = unlocked,
                                onStart = {
                                    // when enabled; navigate to QuizView later
                                    //navController.navigate("StartQuiz/${level.id}") coba ganti
                                    navController.navigate("Quiz/${level.id}/${level.name}")
                                }
                            )

                            if (index != levels.lastIndex) {
                                PathConnectorDots()
                            }
                        }
                    }
                }
            }

            // Bottom padding for scrolling
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

// ===== KOMPONEN-KOMPONEN =====

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
    isLeftAligned: Boolean,
    enabled: Boolean = true,
    onStart: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isLeftAligned) {
            Spacer(modifier = Modifier.weight(0.4f)) // Push ke kanan
        }

        // The Card Content
        val cardColor = if (enabled) colorTheme else Color.LightGray.copy(alpha = 0.6f)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(cardColor)
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
                    color = Color(0xFF222B45).copy(alpha = if (enabled) 1f else 0.6f),
                    fontWeight = FontWeight.Medium
                )

                Column {
                    Text(
                        text = "Level $level",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF222B45).copy(alpha = if (enabled) 1f else 0.6f)
                    )
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF222B45).copy(alpha = if (enabled) 1f else 0.6f)
                    )
                }

                Button(
                    onClick = { if (enabled) onStart() },
                    enabled = enabled,
                    colors = ButtonDefaults.buttonColors(containerColor = if (enabled) buttonColor else Color.Gray),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = if (enabled) "Start Course" else "Locked",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = if (enabled) 1f else 0.9f)
                    )
                }
            }
        }

        if (isLeftAligned) {
            Spacer(modifier = Modifier.weight(0.4f)) // Push ke kiri
        }
    }
}

@Composable
fun PathConnectorDots() { // ini buat titik-titik nya di antara level
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
