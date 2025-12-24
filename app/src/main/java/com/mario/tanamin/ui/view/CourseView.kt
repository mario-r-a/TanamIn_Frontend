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
        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.surface),
        startY = 0f,
        endY = 520f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                    color = MaterialTheme.colorScheme.onBackground
                )
                // Settings Icon
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onBackground,
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
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- Dynamic levels rendering ---
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(text = "Loading levels...", color = MaterialTheme.colorScheme.onBackground)
                    }
                }
                !errorMessage.isNullOrBlank() -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(text = "Error: ${errorMessage}", color = MaterialTheme.colorScheme.error)
                    }
                }
                else -> {
                    if (levels.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text(text = "No levels available", color = MaterialTheme.colorScheme.onBackground)
                        }
                    } else {
                        levels.forEachIndexed { index, level ->
                            val unlocked = if (index == 0) true else levels.getOrNull(index - 1)?.isCompleted == true

                            val isLeft = index % 2 == 0
                            val colorTheme = if (isLeft) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            val buttonColor = if (isLeft) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary

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

            // Bottom padding for scrolling (agar konten tidak tertutup floating navbar)
            Spacer(modifier = Modifier.height(120.dp))
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
    Card(
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterStart
        ) {
             Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha=0.8f)
                            )
                        )
                    )
            )
            // Decorative overlay
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .offset(x = 100.dp, y = (-20).dp)
                    .background(Color.White.copy(alpha = 0.1f), shape = CircleShape)
            )
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = value,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
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
            .height(180.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isLeftAligned) {
            Spacer(modifier = Modifier.weight(0.3f)) // Push ke kanan
        }

        // The Card Content
        val cardColor = if (enabled) colorTheme else Color.Gray.copy(alpha = 0.4f)
        val elevation = if (enabled) 8.dp else 2.dp

        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(elevation),
            colors = CardDefaults.cardColors(containerColor = cardColor)
        ) {
            Box(
                 modifier = Modifier.fillMaxSize()
            ) {
                 // Decorative Circle (Top Right)
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 30.dp, y = (-30).dp)
                        .background(Color.White.copy(alpha = 0.15f), shape = CircleShape)
                )
                // Decorative Circle (Bottom Left)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.BottomStart)
                        .offset(x = (-20).dp, y = 20.dp)
                        .background(Color.White.copy(alpha = 0.1f), shape = CircleShape)
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if(enabled) "Current Lesson" else "Locked Lesson",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                        fontWeight = FontWeight.SemiBold,
                         modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    Column {
                        Text(
                            text = "Level $level",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = if (enabled) 1f else 0.7f),
                            letterSpacing = (-1).sp
                        )
                        Text(
                            text = title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = if (enabled) 1f else 0.8f),
                            maxLines = 2
                        )
                    }

                    Button(
                        onClick = { if (enabled) onStart() },
                        enabled = enabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (enabled) buttonColor else Color.White.copy(alpha=0.3f),
                            contentColor = if (enabled) colorTheme else Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                        modifier = Modifier.height(44.dp).align(Alignment.Start)
                    ) {
                        Text(
                            text = if (enabled) "Start Learning" else "Locked",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (enabled) {
                             Spacer(modifier = Modifier.width(8.dp))
                             Icon(Icons.Default.LocalFireDepartment, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        if (isLeftAligned) {
            Spacer(modifier = Modifier.weight(0.3f)) // Push ke kiri
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
