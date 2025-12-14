package com.mario.tanamin.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun WalletView() {
    // Orange gradient for the top
    val topGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFFFB86C), Color(0xFFFFE3A3), Color.White),
        startY = 0f,
        endY = 320f
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
                .height(220.dp)
                .background(topGradient)
                .align(Alignment.TopCenter)
        ) {}
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Wallets",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF222B45)
                )
                // Settings icon placeholder (replace with Icon if available)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⚙", fontSize = 20.sp, color = Color(0xFF222B45))
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            // Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TabPill(text = "Wallets", selected = true)
                TabPill(text = "History", selected = false)
            }
            Spacer(modifier = Modifier.height(18.dp))
            // Current Balance Card
            BalanceCard()
            Spacer(modifier = Modifier.height(18.dp))
            // Wallet cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmallWalletCard(
                    title = "Main Wallet", amount = "Rp 0", iconType = Icons.Default.Add,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                SmallWalletCard(
                    title = "Investment Wallet", amount = "Rp 0", iconType = Icons.Default.BarChart,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            // Main Wallet Details
            Text(
                text = "Main Wallet Details",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color(0xFF222B45),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            MainWalletDetailsCard(name = "Kenneth", amount = "Rp 0")
            Spacer(modifier = Modifier.height(18.dp))
            // Investment Details
            Text(
                text = "Investment Details",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color(0xFF222B45),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            InvestmentDetailsSection()
        }
    }
}

@Composable
fun TabPill(text: String, selected: Boolean) {
    val bg = if (selected) Color(0xFFFFB86C) else Color.White
    val fg = if (selected) Color.White else Color(0xFF222B45)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable { }
            .padding(horizontal = 28.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = fg, fontWeight = FontWeight.Medium, fontSize = 16.sp)
    }
}

@Composable
fun BalanceCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF3DDC97)),
        contentAlignment = Alignment.CenterStart
    ) {
        // Decorative circles (bottom left, top right)
        Box(
            modifier = Modifier
                .size(48.dp)
                .offset(x = (-16).dp, y = 60.dp)
                .background(Color(0xFF2EBE6A).copy(alpha = 0.25f), shape = CircleShape)
        ) {}
        Box(
            modifier = Modifier
                .size(36.dp)
                .offset(x = 260.dp, y = (-18).dp)
                .background(Color(0xFF2EBE6A).copy(alpha = 0.18f), shape = CircleShape)
        ) {}
        // Main content
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Current Balance",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Rp130.300",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Payments, contentDescription = null, tint = Color.White)
                }
                // Pager indicator
//                Row(
//                    modifier = Modifier.padding(top = 8.dp),
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Box(
//                        modifier = Modifier
//                            .size(width = 18.dp, height = 6.dp)
//                            .clip(RoundedCornerShape(3.dp))
//                            .background(Color.White)
//                    )
//                    Spacer(modifier = Modifier.width(4.dp))
//                    Box(
//                        modifier = Modifier
//                            .size(6.dp)
//                            .clip(CircleShape)
//                            .background(Color.White.copy(alpha = 0.5f))
//                    )
//                    Spacer(modifier = Modifier.width(4.dp))
//                    Box(
//                        modifier = Modifier
//                            .size(6.dp)
//                            .clip(CircleShape)
//                            .background(Color.White.copy(alpha = 0.5f))
//                    )
//                }
            }
            // Right arrow in a white circle
//            Box(
//                modifier = Modifier
//                    .size(36.dp)
//                    .clip(CircleShape)
//                    .background(Color.White),
//                contentAlignment = Alignment.Center
//            ) {
//                Text("→", color = Color(0xFF3DDC97), fontSize = 22.sp)
//            }
        }
    }
}

@Composable
fun SmallWalletCard(
    title: String,
    amount: String,
    iconType: ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFFFB86C)),
        contentAlignment = Alignment.TopStart
    ) {
        // Decorative circle at bottom right
        Box(
            modifier = Modifier
                .size(60.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 10.dp, y = 10.dp)
                .background(Color(0xFFFFE3A3).copy(alpha = 0.55f), shape = CircleShape)
        ) {}
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = 20.dp, top = 14.dp, bottom = 18.dp, end = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon in a white circle
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconType,
                    contentDescription = null,
                    tint = Color(0xFFFFB86C),
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                color = Color(0xFF222B45),
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = amount,
                color = Color(0xFF222B45),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun MainWalletDetailsCard(name: String, amount: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFFB86C)),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.padding(start = 20.dp)
            ) {
                Text(
                    text = name,
                    color = Color(0xFF222B45),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                Text(
                    text = amount,
                    color = Color(0xFF222B45),
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun InvestmentDetailsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Inactive/Still card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEEEEEE)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("❌", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Inactive/Still",
                        color = Color(0xFF222B45),
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Ready to Invest",
                        color = Color(0xFF8F9BB3),
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Rp 0",
                    color = Color(0xFF222B45),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
        }
        // Active/Running card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF3DDC97)),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✅", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Active/Running",
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Currently Invested",
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "No Active Investments",
                            color = Color(0xFF3DDC97),
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = {},
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF3DDC97)),
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    Text("Invest", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun WalletPreview() {
    WalletView()
}