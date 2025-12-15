package com.mario.tanamin.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.mario.tanamin.ui.model.PocketModel
import com.mario.tanamin.ui.viewmodel.WalletViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale


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
    val budgetingPercentage by walletViewModel.budgetingPercentage.collectAsState()

    // Collect selected screen from VM
    val selectedScreen by walletViewModel.selectedScreen.collectAsState()

    // Add balance dialog state
    var showAddBalanceDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Load profile on first composition
    LaunchedEffect(Unit) {
        walletViewModel.loadProfile()
    }

    // Collect messages from ViewModel
    LaunchedEffect(Unit) {
        walletViewModel.messageFlow.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

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

                // Tabs — now wired to ViewModel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TabPill(text = "Main", selected = (selectedScreen == WalletViewModel.WalletScreen.Main)) { walletViewModel.setSelectedScreen(WalletViewModel.WalletScreen.Main) }
                    TabPill(text = "Investment", selected = (selectedScreen == WalletViewModel.WalletScreen.Investment)) { walletViewModel.setSelectedScreen(WalletViewModel.WalletScreen.Investment) }
                }
                Spacer(modifier = Modifier.height(18.dp))

                // Content switch handled by VM-level state
                when (selectedScreen) {
                    WalletViewModel.WalletScreen.Main -> MainWalletView(
                        mainTotal = formattedMainTotal,
                        onAddBalanceClick = { showAddBalanceDialog = true }
                    )
                    WalletViewModel.WalletScreen.Investment -> InvestmentWalletView(totalInvestedFormatted = formattedInvestmentTotal)
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
                when (selectedScreen) {
                    WalletViewModel.WalletScreen.Main -> item { PocketsGridFromModels(pockets = activeMain, navController = navController) }
                    WalletViewModel.WalletScreen.Investment -> item { PocketsGridFromModels(pockets = investments, navController = navController) }
                }
            }
        }

        // Snackbar host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Add Balance Dialog
    if (showAddBalanceDialog) {
        AddBalanceDialog(
            budgetingPercentage = budgetingPercentage,
            onDismiss = { showAddBalanceDialog = false },
            onConfirm = { amount, percentage ->
                scope.launch {
                    val success = walletViewModel.addBalance(amount, percentage)
                    if (success) {
                        showAddBalanceDialog = false
                    }
                }
            }
        )
    }
}

@Composable
private fun MainWalletView(mainTotal: String, onAddBalanceClick: () -> Unit = {}) {
    // Main-specific layout: keeps the MainWalletCard, action buttons and label
    Column(modifier = Modifier.fillMaxWidth()) {
        MainWalletCard(mainTotal = mainTotal)
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ActionButton(text = "Add Pocket", icon = Icons.Default.Add, modifier = Modifier.weight(1f))
            ActionButton(text = "Add Balance", icon = Icons.Default.Add, modifier = Modifier.weight(1f), onClick = onAddBalanceClick)
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
private fun InvestmentWalletView(totalInvestedFormatted: String) {
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
fun ActionButton(text: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF37c447))
            .clickable { onClick() },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBalanceDialog(
    budgetingPercentage: Int?,
    onDismiss: () -> Unit,
    onConfirm: (amount: Long, percentage: Int) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var useProfilePercentage by remember { mutableStateOf(true) }
    var customPercentageText by remember { mutableStateOf(budgetingPercentage?.toString() ?: "70") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val parsedAmount = amountText.toLongOrNull() ?: 0L
    val percentage = if (useProfilePercentage) {
        budgetingPercentage ?: 70
    } else {
        customPercentageText.toIntOrNull() ?: 70
    }

    // Calculate split amounts
    val toMain = (parsedAmount * percentage / 100)
    val toInvestment = parsedAmount - toMain

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Balance",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Amount input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMessage = null
                    },
                    label = { Text("Amount") },
                    placeholder = { Text("Enter amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = errorMessage != null,
                    supportingText = if (errorMessage != null) {
                        { Text(errorMessage!!, color = MaterialTheme.colorScheme.error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Budgeting percentage selection
                Text(
                    text = "Budgeting Distribution",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )

                // Use profile percentage option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { useProfilePercentage = true }
                        .background(if (useProfilePercentage) Color(0xFFFFE3A3).copy(alpha = 0.3f) else Color.Transparent)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = useProfilePercentage,
                        onClick = { useProfilePercentage = true }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Use Profile Setting",
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${budgetingPercentage ?: 70}% to Main Wallet",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Custom percentage option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { useProfilePercentage = false }
                        .background(if (!useProfilePercentage) Color(0xFFFFE3A3).copy(alpha = 0.3f) else Color.Transparent)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !useProfilePercentage,
                        onClick = { useProfilePercentage = false }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Custom Percentage",
                        fontWeight = FontWeight.Medium
                    )
                }

                // Custom percentage input (only shown when custom is selected)
                if (!useProfilePercentage) {
                    OutlinedTextField(
                        value = customPercentageText,
                        onValueChange = { customPercentageText = it },
                        label = { Text("Percentage to Main Wallet") },
                        placeholder = { Text("Enter percentage (0-100)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        suffix = { Text("%") }
                    )
                }

                // Preview of split
                if (parsedAmount > 0) {
                    HorizontalDivider()
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Distribution Preview",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color(0xFF666666)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Main Wallet ($percentage%)",
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Rp${NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).format(toMain)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF37c447)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Inactive Investments (${100 - percentage}%)",
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Rp${NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).format(toInvestment)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFFFFB86C)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Validate inputs
                    if (amountText.isBlank()) {
                        errorMessage = "Please enter an amount"
                        return@TextButton
                    }
                    if (parsedAmount <= 0) {
                        errorMessage = "Amount must be greater than zero"
                        return@TextButton
                    }
                    val finalPercentage = if (useProfilePercentage) {
                        budgetingPercentage ?: 70
                    } else {
                        customPercentageText.toIntOrNull() ?: run {
                            errorMessage = "Invalid percentage"
                            return@TextButton
                        }
                    }
                    if (finalPercentage < 0 || finalPercentage > 100) {
                        errorMessage = "Percentage must be between 0 and 100"
                        return@TextButton
                    }

                    onConfirm(parsedAmount, finalPercentage)
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFF37c447)
                )
            ) {
                Text("Add Balance")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.Gray
                )
            ) {
                Text("Cancel")
            }
        }
    )
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun WalletPreview() {
    WalletView(navController = rememberNavController())
}