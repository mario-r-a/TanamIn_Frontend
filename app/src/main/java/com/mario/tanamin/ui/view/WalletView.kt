package com.mario.tanamin.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.mario.tanamin.ui.viewmodel.WalletViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun WalletView(navController: NavController, walletViewModel: WalletViewModel = viewModel()) {
    // collect viewmodel state
    val pocketsState by walletViewModel.pockets.collectAsState()
    val loading by walletViewModel.isLoading.collectAsState()
    val error by walletViewModel.error.collectAsState()

    // collect reactive main total from ViewModel (ensures it's updated when pockets change)
    val mainTotal by walletViewModel.mainTotalFlow.collectAsState()

    // try to load pockets from in-memory user id when the composable enters composition
    LaunchedEffect(Unit) {
        walletViewModel.loadPocketsFromInMemoryUser()
    }

    // Format using Indonesian locale (e.g., 130.300)
    val formattedMainTotal: String = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).format(mainTotal)

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
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Wallets",
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
                Spacer(modifier = Modifier.height(16.dp))
                // Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TabPill(text = "Main", selected = true)
                    TabPill(text = "Investment", selected = false)
                }
                Spacer(modifier = Modifier.height(18.dp))
                // Main Wallet Card -> pass formatted main total
                MainWalletCard(mainTotal = formattedMainTotal)
                Spacer(modifier = Modifier.height(18.dp))
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ActionButton(text = "Add Pocket", icon = Icons.Default.Add, modifier = Modifier.weight(1f))
                    ActionButton(text = "Add Balance", icon = Icons.Default.Add, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(18.dp))
                // Pockets label
                Text(
                    text = "Pockets",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = Color(0xFF222B45),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Show loading / error / pockets
                if (loading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (error != null) {
                    Text(text = error ?: "Unknown error", color = MaterialTheme.colorScheme.error)
                } else {
                    // map DTO pockets to display model and show grid
                    val display = pocketsState.map { dp -> PocketData(dp.name, "Rp${dp.total}") }
                    PocketsGrid(display)
                }
            }
        }
    }
}

@Composable
fun TabPill(text: String, selected: Boolean) {
    val bg = if (selected) Color(0xFFFFB86C) else Color.White
    val fg = if (selected) Color.Black else Color.Black
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable { }
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = fg,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 16.sp
        )
    }
}

@Composable
fun MainWalletCard(mainTotal: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFFFB86C)),
        contentAlignment = Alignment.CenterStart
    ) {
        // Decorative circles
        Box(
            modifier = Modifier
                .size(48.dp)
                .offset(x = (-16).dp, y = 60.dp)
                .background(Color(0xFFFFE3A3).copy(alpha = 0.35f), shape = CircleShape)
        ) {}
        Box(
            modifier = Modifier
                .size(60.dp)
                .offset(x = 220.dp, y = (-18).dp)
                .background(Color(0xFFFFE3A3).copy(alpha = 0.18f), shape = CircleShape)
        ) {}
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 18.dp, bottom = 18.dp)
        ) {
            Text(
                text = "Main Wallet",
                color = Color(0xFF222B45),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Rp$mainTotal",
                color = Color(0xFF222B45),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ActionButton(text: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF37c447))
            .clickable { },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFF37c447), modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
fun PocketsGrid(pockets: List<PocketData>) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        val rows = pockets.chunked(2)
        rows.forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // first item
                PocketCard(pair[0], Modifier.weight(1f))
                // second item if exists
                if (pair.size > 1) PocketCard(pair[1], Modifier.weight(1f)) else Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

data class PocketData(val name: String, val amount: String)

@Composable
fun PocketCard(data: PocketData, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(120.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFFFB86C)),
        contentAlignment = Alignment.TopStart
    ) {
        // Decorative circle
        Box(
            modifier = Modifier
                .size(36.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 8.dp, y = 8.dp)
                .background(Color(0xFFFFE3A3).copy(alpha = 0.45f), shape = CircleShape)
        ) {}
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, top = 16.dp, bottom = 12.dp, end = 12.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFFFFB86C),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = data.name,
                color = Color(0xFF222B45),
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
            )
            Text(
                text = data.amount,
                color = Color(0xFF222B45),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun WalletPreview() {
    WalletView(navController = rememberNavController())
}