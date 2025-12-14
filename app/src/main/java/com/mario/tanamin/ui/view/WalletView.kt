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
import com.mario.tanamin.ui.viewmodel.WalletViewModel
import java.text.NumberFormat
import java.util.Locale

// Simple local model used only within this view to render pocket cards
data class PocketData(val name: String, val amount: String)

// Tabs enum
private enum class WalletTab { Main, Investment }


@Composable
fun WalletView(
    navController: NavController,
    walletViewModel: WalletViewModel = viewModel()
) {
    // Collect only active Main pockets as required
    val activeMain by walletViewModel.activeMainPockets.collectAsState()
    val investments by walletViewModel.investmentPockets.collectAsState()
    val loading by walletViewModel.isLoading.collectAsState()
    val mainTotal by walletViewModel.mainTotalFlow.collectAsState()

    // UI tab state
    var selectedTab by remember { mutableStateOf(WalletTab.Main) }

    LaunchedEffect(Unit) {
        walletViewModel.loadPocketsFromInMemoryUser()
    }

    val formattedMainTotal = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).format(mainTotal)

    val topGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFFFB86C), Color(0xFFFFE3A3), Color.White),
        startY = 0f,
        endY = 520f
    )

    // Build pocket data list based on selected tab
    val pocketDataList: List<PocketData> = if (selectedTab == WalletTab.Main) {
        activeMain.map { p ->
            PocketData(
                p.name,
                "Rp${NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).format(p.total)}"
            )
        }
    } else {
        investments.map { p ->
            PocketData(
                p.name,
                "Rp${NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).format(p.total)}"
            )
        }
    }

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

                // Content switch: completely different views per tab
                when (selectedTab) {
                    WalletTab.Main -> {
                        MainContent(mainTotal = formattedMainTotal)
                    }
                    WalletTab.Investment -> {
                        // compute total directly from investments (they contain numeric totals)
                        val investmentTotal: Long = investments.sumOf { it.total }
                        InvestmentContent(totalInvested = investmentTotal)
                    }
                }
            }

            // Show loading / error / pockets
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
                // The content composables above have already emitted the visible UI. We still display
                // the list part for each tab as a separate item so scrolling works smoothly.
                when (selectedTab) {
                    WalletTab.Main -> item { PocketsGrid(pockets = pocketDataList) }
                    WalletTab.Investment -> item { InvestmentList(pockets = pocketDataList) }
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
private fun InvestmentContent(totalInvested: Long) {
    // Investment-specific layout: different summary card, different actions, different label
    Column(modifier = Modifier.fillMaxWidth()) {
        InvestmentSummaryCard(totalInvested = totalInvested)
        Spacer(modifier = Modifier.height(12.dp))
        InvestmentActions()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your Investments",
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = Color(0xFF222B45),
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

@Composable
fun InvestmentSummaryCard(totalInvested: Long) {
    val formatted = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).format(totalInvested)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFEDE7F6)),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Investment Wallet", fontWeight = FontWeight.Medium, color = Color(0xFF222B45))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Rp$formatted", fontWeight = FontWeight.Bold, fontSize = 26.sp, color = Color(0xFF222B45))
        }
    }
}

@Composable
fun InvestmentActions() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF7E57C2))
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Invest", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFB39DDB))
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Withdraw", color = Color.White, fontWeight = FontWeight.Bold)
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

@Composable
fun PocketsGrid(pockets: List<PocketData>) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        val rows: List<List<PocketData>> = pockets.chunked(2)
        rows.forEach { pair: List<PocketData> ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                PocketCard(pair[0], Modifier.weight(1f))
                if (pair.size > 1) {
                    PocketCard(pair[1], Modifier.weight(1f))
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}


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

@Composable
fun InvestmentList(pockets: List<PocketData>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        pockets.forEach { p ->
            InvestmentCard(data = p)
        }
    }
}

@Composable
fun InvestmentCard(data: PocketData) {
    // Different styling for investment items (full-width card)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFEFEFEF)),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF777777),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = data.name, fontWeight = FontWeight.Medium, color = Color(0xFF222B45))
                Text(text = data.amount, fontWeight = FontWeight.Bold, color = Color(0xFF222B45))
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun WalletPreview() {
    WalletView(navController = rememberNavController())
}