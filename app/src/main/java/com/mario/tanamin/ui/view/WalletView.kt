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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.mario.tanamin.ui.model.PocketModel
import com.mario.tanamin.ui.viewmodel.WalletViewModel
import java.text.NumberFormat
import java.util.Locale

// Tabs enum
private enum class WalletTab { Main, Investment }


@Composable
fun WalletView(
    navController: NavController,
    walletViewModel: WalletViewModel = viewModel()
) {
    // Collect model-level data from ViewModel
    val mainTotal by walletViewModel.mainTotalFlow.collectAsState()
    val activeMain by walletViewModel.activeMainPockets.collectAsState()
    val investments by walletViewModel.investmentPockets.collectAsState()
    val investmentTotal by walletViewModel.investmentTotalFlow.collectAsState()
    val loading by walletViewModel.isLoading.collectAsState()

    // UI tab state
    var selectedTab by remember { mutableStateOf(WalletTab.Main) }

    val formattedMainTotal = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).format(mainTotal)
    val formattedInvestmentTotal = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).format(investmentTotal)

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
                    modifier = Modifier
                        .fillMaxWidth()
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
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { navController.navigate("settings") }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TabPill(text = "Main", selected = (selectedTab == WalletTab.Main)) { selectedTab = WalletTab.Main }
                    TabPill(text = "Investment", selected = (selectedTab == WalletTab.Investment)) { selectedTab = WalletTab.Investment }
                }
                Spacer(modifier = Modifier.height(18.dp))

                // Content switch: entirely UI-driven; VM provides models
                when (selectedTab) {
                    WalletTab.Main -> MainContent(mainTotal = formattedMainTotal)
                    WalletTab.Investment -> InvestmentContent(totalInvestedFormatted = formattedInvestmentTotal)
                }
            }

            // Show loading / pockets
            if (loading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                // Use VM-provided model lists
                when (selectedTab) {
                    WalletTab.Main -> item { PocketsGridFromModels(pockets = activeMain, navController = navController) }
                    WalletTab.Investment -> item { PocketsGridFromModels(pockets = investments, navController = navController) }
                }
            }
        }
    }
}

@Composable
private fun MainContent(mainTotal: String) {
    // Main-specific layout: keeps the MainWalletCard, action buttons and label
    Column(modifier = Modifier.fillMaxWidth()) {
        MainWalletCard(mainTotal = mainTotal)
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ActionButton(text = "Add Pocket", icon = Icons.Default.Add, modifier = Modifier.weight(1f))
            ActionButton(text = "Add Balance", icon = Icons.Default.Add, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Pockets",
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = Color(0xFF222B45),
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

@Composable
private fun InvestmentContent(totalInvestedFormatted: String) {
    // Investment-specific layout: same style as Main wallet
    Column(modifier = Modifier.fillMaxWidth()) {
        InvestmentWalletCard(formattedTotal = totalInvestedFormatted)
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ActionButton(text = "Budgeting", icon = Icons.Default.Add, modifier = Modifier.weight(1f))
            ActionButton(text = "Add Balance", icon = Icons.Default.Add, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Pockets",
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = Color(0xFF222B45),
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

@Composable
fun InvestmentWalletCard(formattedTotal: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFFFB86C)),
        contentAlignment = Alignment.CenterStart
    ) {
        // Decorative circles - matching the design
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
                text = "Investment Wallet",
                color = Color(0xFF222B45),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Rp$formattedTotal",
                color = Color(0xFF222B45),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


@Composable
fun PocketsGridFromModels(pockets: List<PocketModel>, navController: NavController) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        val rows: List<List<PocketModel>> = pockets.chunked(2)
        rows.forEach { pair: List<PocketModel> ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                PocketCardFromModel(pair[0], Modifier.weight(1f), navController)
                if (pair.size > 1) {
                    PocketCardFromModel(pair[1], Modifier.weight(1f), navController)
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun PocketCardFromModel(data: PocketModel, modifier: Modifier = Modifier, navController: NavController) {
    val formatted = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).format(data.total)
    Box(
        modifier = modifier
            .height(120.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFFFB86C))
            .clickable { navController.navigate("PocketDetail/${data.id}") },
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
                text = "Rp$formatted",
                color = Color(0xFF222B45),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun TabPill(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) Color(0xFFFFB86C) else Color.White
    val fg = Color.Black
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick)
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
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color(0xFF37c447),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun WalletPreview() {
    WalletView(navController = rememberNavController())
}