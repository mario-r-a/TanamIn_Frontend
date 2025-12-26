package com.mario.tanamin.ui.view

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mario.tanamin.ui.viewmodel.PocketDetailViewModel
import com.mario.tanamin.ui.model.PocketModel
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import com.mario.tanamin.ui.model.PocketTransactionModel
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import com.mario.tanamin.data.dto.DataPocketHistoryResponse
import com.mario.tanamin.data.dto.DataTransactionResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PocketDetailView(
    navController: NavController,
    pocketId: Int,
    viewModel: PocketDetailViewModel = viewModel(),
    onSell: () -> Unit = {},
    onBuy: () -> Unit = {}
) {
    val pocket by viewModel.pocket.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val availableTargets by viewModel.availableTargets.collectAsState()
    val allTransactions by viewModel.transactions.collectAsState()
    val uiTransactions by viewModel.uiTransactions.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    // coroutineScope not needed here (snackbar uses LaunchedEffect)

    // Collect one-shot messages from the ViewModel
    LaunchedEffect(Unit) {
        viewModel.messageFlow.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    var showMoveDialog by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var showBuyDialog by remember { mutableStateOf(false) }
    var showSellDialog by remember { mutableStateOf(false) }

    LaunchedEffect(pocketId) {
        viewModel.loadPocket(pocketId)
        viewModel.loadTransactions(pocketId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Large decorative circle at bottom
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(y = 350.dp)
                .align(Alignment.BottomCenter)
                .background(MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape)
        )

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (error != null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = error ?: "Unknown error",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadPocket(pocketId) }) {
                                Text("Retry")
                            }
                        }
                    }
                } else if (pocket != null) {
                    PocketDetailContent(
                        pocket = pocket!!,
                        transactions = uiTransactions, // Pass uiTransactions
                        allTransactions = allTransactions, // Pass original transaction list
                        onMoveClicked = { showMoveDialog = true },
                        onWithdrawClicked = { showWithdrawDialog = true },
                        onSellClicked = { showSellDialog = true },
                        onBuyClicked = { showBuyDialog = true },
                        contentPadding = innerPadding
                    )
                }

                // Dialog outside pocket != null check to avoid recomposition issues
                if (showMoveDialog && pocket != null) {
                    MoveMoneyDialog(
                        availableTargets = availableTargets,
                        maxAmount = pocket!!.total,
                        onDismiss = { showMoveDialog = false },
                        onMoveSuspend = { toPocketId, amount ->
                            // delegate to ViewModel suspend function
                            viewModel.transferMoneySuspend(toPocketId, amount)
                        }
                    )
                }

                // Withdraw dialog
                if (showWithdrawDialog && pocket != null) {
                    WithdrawDialog(
                        pocket = pocket!!,
                        onDismiss = { showWithdrawDialog = false },
                        onWithdrawSuspend = { amount ->
                            viewModel.withdrawMoneySuspend(amount)
                        }
                    )
                }

                // Buy Investment dialog
                if (showBuyDialog && pocket != null) {
                    BuyInvestmentDialog(
                        pocket = pocket!!,
                        onDismiss = { showBuyDialog = false },
                        onBuySuspend = { name, nominal, pricePerUnit, unitAmount ->
                            viewModel.buyInvestmentSuspend(name, nominal, pricePerUnit, unitAmount)
                        }
                    )
                }

                // Sell Investment dialog
                if (showSellDialog && pocket != null) {
                    SellInvestmentDialog(
                        pocket = pocket!!,
                        allTransactions = allTransactions,
                        onDismiss = { showSellDialog = false },
                        onSellSuspend = { investmentName, sellPrice, unitAmount, originalBuyCost ->
                            viewModel.sellInvestmentSuspend(investmentName, sellPrice, unitAmount, originalBuyCost)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PocketDetailContent(
    pocket: PocketModel,
    transactions: List<PocketTransactionModel>, // Use PocketTransactionModel
    allTransactions: List<DataTransactionResponse>, // Use correct DTO type
    onMoveClicked: () -> Unit,
    onWithdrawClicked: () -> Unit,
    onSellClicked: () -> Unit,
    onBuyClicked: () -> Unit,
    contentPadding: PaddingValues = PaddingValues()
) {
    val formattedTotal = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).format(pocket.total)

    // Decide button label and behaviour
    val walletType = pocket.walletType.trim()
    val pocketNameLower = pocket.name.lowercase(Locale.getDefault())
    // Normalize name: remove non-alphanumeric, collapse spaces, lowercase for robust comparison
    // Remove unused normalizedName variable

    // Determine which button to show:
    // - Main => Move Money
    // - Investment & name contains "active" => Sell Investment
    // - Investment & name contains "inactive" => Move to Main (calls onBuyClicked)
    // - Investment & name equals "Inactive Investment" => show two buttons (Move Money + Sell Investment)
    // Be permissive: walletType may be 'Investment', 'Investments', or similar; match on 'invest'
    val isInvestment = walletType.contains("invest", ignoreCase = true)
    val isMain = walletType.contains("main", ignoreCase = true)
    // Make sure 'inactive' does not accidentally match 'active' ("inactive" contains "active").
    val isActiveInvestment = isInvestment && pocketNameLower.contains("active") && !pocketNameLower.contains("inactive")
    // Consider inactive if name explicitly contains 'inactive' (robust to plural/suffixes)
    val isInactiveInvestment = isInvestment && pocketNameLower.contains("inactive")

    // Debug log to help verify which branch is active at runtime
    Log.d("PocketDetailView", "pocket='${pocket.name}' walletType='${walletType}' isMain=$isMain isInvestment=$isInvestment isActiveInvestment=$isActiveInvestment isInactiveInvestment=$isInactiveInvestment")

    val buttonText = when {
        isMain -> "Move Money"
        isActiveInvestment -> "Sell Investment"
        isInactiveInvestment -> "Move to Main"
        isInvestment -> "Investment Action"
        else -> "Action"
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Orange header section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(bottom = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding() // respect the status bar, so the orange header visually starts under it
                    .padding(horizontal = 16.dp), // add horizontal padding so header content (buttons) aren't flush to edge
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = pocket.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Rp$formattedTotal",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Action Button (behaviour depends on pocket type/name)
                // If this is an inactive Investment pocket, show two actions side-by-side:
                //  - Move Money (opens Move dialog)
                //  - Sell Investment (calls onSellClicked)
                if (isInactiveInvestment) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Move Money button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White)
                                .clickable { onMoveClicked() }
                                .padding(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("Move Money", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                            }
                        }

                        // Buy Investment button (was Sell Investment)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onBuyClicked() }
                                .padding(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("Buy Investment", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                } else if (isMain) {
                    // For main pockets, show Withdraw and Move Money side by side with the same color scheme as inactive investments
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Withdraw button (left, surfaceVariant)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onWithdrawClicked() }
                                .padding(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("Withdraw", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                        }
                        // Move Money button (right, white)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White)
                                .clickable { onMoveClicked() }
                                .padding(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("Move Money", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White)
                            .clickable {
                                when {
                                    isMain -> onMoveClicked()
                                    isActiveInvestment -> onSellClicked()
                                    isInactiveInvestment -> onBuyClicked()
                                    isInvestment -> onSellClicked()
                                    else -> { /* no-op */ }
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = buttonText,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- Active Investments Section (only for active investment pocket) ---
        val containsInvest = pocket.walletType.contains("invest", ignoreCase = true)
        val pocketNameContainsActive = pocket.name.contains("active", ignoreCase = true)
        val pocketNameContainsInactive = pocket.name.contains("inactive", ignoreCase = true)

        Log.d("PocketDetailView", "=== Checking Active Investment Section ===")
        Log.d("PocketDetailView", "Pocket ID: ${pocket.id}, Name: ${pocket.name}")
        Log.d("PocketDetailView", "walletType: '${pocket.walletType}', isActive: ${pocket.isActive}")
        Log.d("PocketDetailView", "containsInvest: $containsInvest, pocketNameContainsActive: $pocketNameContainsActive, pocketNameContainsInactive: $pocketNameContainsInactive")

        // Only show in pockets that are Investment type, have isActive=true,
        // AND the name contains "Active" but NOT "Inactive"
        val isActiveInvestmentPocket = containsInvest && pocket.isActive && pocketNameContainsActive && !pocketNameContainsInactive
        Log.d("PocketDetailView", "isActiveInvestmentPocket: $isActiveInvestmentPocket")

        if (isActiveInvestmentPocket) {
            Log.d("PocketDetailView", "=== Active Investment Pocket Detected - SHOWING SECTION ===")
            Log.d("PocketDetailView", "Total transactions loaded: ${allTransactions.size}")

            // Log all transactions for debugging
            allTransactions.forEachIndexed { index, tx ->
                Log.d("PocketDetailView", "Transaction $index: action=${tx.action}, name=${tx.name}, pocketId=${tx.pocketId}, toPocketId=${tx.toPocketId}, unitAmount=${tx.unitAmount}, nominal=${tx.nominal}")
            }

            // Filter transactions that are related to this pocket AND are Buy/Sell actions
            val relevantTransactions = allTransactions.filter {
                val isPocketRelated = it.pocketId == pocket.id || it.toPocketId == pocket.id
                val isBuyOrSell = it.action.equals("Buy", ignoreCase = true) || it.action.equals("Sell", ignoreCase = true)
                isPocketRelated && isBuyOrSell
            }

            Log.d("PocketDetailView", "Relevant Buy/Sell transactions for pocket ${pocket.id}: ${relevantTransactions.size}")
            relevantTransactions.forEach { tx ->
                Log.d("PocketDetailView", "  -> ${tx.action}: ${tx.name}, units=${tx.unitAmount}, nominal=${tx.nominal}")
            }

            // Group by investment name and calculate totals
            val investments = relevantTransactions
                .groupBy { it.name }
                .mapNotNull { (investmentName, txs) ->
                    Log.d("PocketDetailView", "Calculating for investment: $investmentName (${txs.size} transactions)")

                    // Calculate total units: Buy adds units, Sell subtracts units
                    val totalUnits = txs.sumOf { tx ->
                        val units = tx.unitAmount.toLong()
                        if (tx.action.equals("Buy", ignoreCase = true)) {
                            Log.d("PocketDetailView", "  Buy: +$units units")
                            units
                        } else {
                            Log.d("PocketDetailView", "  Sell: -$units units")
                            -units
                        }
                    }

                    // Calculate total investment value: Buy adds value, Sell subtracts value
                    val totalPrice = txs.sumOf { tx ->
                        val price = tx.nominal.toLong()
                        if (tx.action.equals("Buy", ignoreCase = true)) price else -price
                    }

                    // Calculate average price per unit (only if we have units)
                    val pricePerUnit = if (totalUnits > 0L) totalPrice / totalUnits else 0L

                    Log.d("PocketDetailView", "  Result for $investmentName: totalUnits=$totalUnits, totalPrice=$totalPrice, pricePerUnit=$pricePerUnit")

                    // Only include investments where we still own units
                    if (totalUnits > 0L) {
                        investmentName to Triple(totalUnits, totalPrice, pricePerUnit)
                    } else {
                        Log.d("PocketDetailView", "  Skipping $investmentName (no units owned)")
                        null
                    }
                }

            Log.d("PocketDetailView", "Total active investments to display: ${investments.size}")

            if (investments.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Active Investments",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    investments.forEach { (investmentName, triple) ->
                        ActiveInvestmentDisplay(
                            name = investmentName,
                            units = triple.first,
                            totalPrice = triple.second,
                            pricePerUnit = triple.third
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                // Show placeholder when no investments
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Active Investments",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "No active investments yet. Buy investments from your inactive investment pocket.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Fixed History header (stays visible while list scrolls)
        Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp)) {
            Text(
                text = "History",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 42.dp)
            )
        }

        // Scrollable list of history items only (header is outside so it won't scroll)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            if (transactions.isEmpty()) {
                item {
                    Text(
                        text = "No transactions found.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }
            } else {
                items(transactions.size) { index ->
                    val transaction = transactions[index]
                    TransactionHistoryItem(
                        description = transaction.type + (transaction.otherPocketName?.let { " ($it)" } ?: ""),
                        date = formatTransactionDate(transaction.date),
                        amount = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).format(transaction.amount),
                        label = transaction.action
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun ActiveInvestmentDisplay(name: String, units: Long, totalPrice: Long, pricePerUnit: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, shape = RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Units Owned: $units",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Price/Unit: Rp${NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).format(pricePerUnit)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "Total Value",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Rp${NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).format(totalPrice)}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveMoneyDialog(
    availableTargets: List<PocketModel>,
    maxAmount: Long? = null,
    onDismiss: () -> Unit,
    onMoveSuspend: suspend (toPocketId: Int, amount: Long) -> Boolean
) {
    var amountText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedTarget by remember { mutableStateOf<PocketModel?>(if (availableTargets.isNotEmpty()) availableTargets[0] else null) }
    val coroutineScope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val maxAmountValue = maxAmount ?: 0L

    // Generate dynamic quick amounts based on available balance
    val quickAmounts = when {
        maxAmountValue >= 500000L -> listOf(50000L, 100000L, 200000L, 500000L)
        maxAmountValue >= 200000L -> listOf(20000L, 50000L, 100000L, 200000L)
        maxAmountValue >= 100000L -> listOf(10000L, 25000L, 50000L, 100000L)
        maxAmountValue >= 50000L -> listOf(10000L, 20000L, 30000L, 50000L)
        maxAmountValue >= 20000L -> listOf(5000L, 10000L, 15000L, 20000L)
        maxAmountValue > 0L -> listOf(
            (maxAmountValue * 0.25).toLong(),
            (maxAmountValue * 0.5).toLong(),
            (maxAmountValue * 0.75).toLong(),
            maxAmountValue
        )
        else -> emptyList()
    }

    // helper to parse amount
    fun parsedAmount(): Long {
        return amountText.replace("[^0-9]".toRegex(), "").toLongOrNull() ?: 0L
    }

    val amount = parsedAmount()
    val exceedsMax = maxAmount != null && amount > maxAmount
    val isConfirmEnabled = !isProcessing && selectedTarget != null && amount > 0L && !exceedsMax

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                // validate again
                localError = null
                if (selectedTarget == null) {
                    localError = "Select a target pocket"
                    return@TextButton
                }
                if (amount <= 0L) {
                    localError = "Enter a valid amount"
                    return@TextButton
                }
                if (exceedsMax) {
                    localError = "Amount exceeds available balance"
                    return@TextButton
                }

                // Call suspend function
                coroutineScope.launch {
                    isProcessing = true
                    val toId = selectedTarget!!.id
                    val success = try {
                        onMoveSuspend(toId, amount)
                    } catch (e: Exception) {
                        false
                    }
                    isProcessing = false
                    if (success) {
                        onDismiss()
                    }
                }

            }, enabled = isConfirmEnabled) {
                if (isProcessing) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                else Text("Move")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Move Money") },
        text = {
            Column {
                // Amount input field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = exceedsMax
                )
                if (maxAmount != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Available: Rp${NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).format(maxAmount)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Quick amount buttons
                if (quickAmounts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Quick amounts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickAmounts.forEach { quickAmount ->
                            OutlinedButton(
                                onClick = {
                                    amountText = quickAmount.toString()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = when {
                                        quickAmount >= 1000000L -> "${quickAmount / 1000000}M"
                                        quickAmount >= 1000L -> "${quickAmount / 1000}K"
                                        else -> "$quickAmount"
                                    },
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Target pocket selector
                Spacer(modifier = Modifier.height(12.dp))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = selectedTarget?.name ?: "Select target",
                        onValueChange = {},
                        label = { Text("Target Pocket") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        availableTargets.forEach { pocket ->
                            DropdownMenuItem(
                                text = { Text(pocket.name) },
                                onClick = {
                                    selectedTarget = pocket
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Error message
                if (localError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = localError ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawDialog(
    pocket: PocketModel,
    onDismiss: () -> Unit,
    onWithdrawSuspend: suspend (amount: Long) -> Boolean
) {
    var amountText by remember { mutableStateOf("") }
    var sliderValue by remember { mutableStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val maxAmount = pocket.total

    // Generate dynamic quick amounts based on available balance
    val quickAmounts = when {
        maxAmount >= 500000L -> listOf(50000L, 100000L, 200000L, 500000L)
        maxAmount >= 200000L -> listOf(20000L, 50000L, 100000L, 200000L)
        maxAmount >= 100000L -> listOf(10000L, 25000L, 50000L, 100000L)
        maxAmount >= 50000L -> listOf(10000L, 20000L, 30000L, 50000L)
        maxAmount >= 20000L -> listOf(5000L, 10000L, 15000L, 20000L)
        else -> listOf(
            (maxAmount * 0.25).toLong(),
            (maxAmount * 0.5).toLong(),
            (maxAmount * 0.75).toLong(),
            maxAmount
        )
    }

    // Helper to parse amount
    fun parsedAmount(): Long {
        return amountText.replace("[^0-9]".toRegex(), "").toLongOrNull() ?: 0L
    }

    // Update slider when text changes
    LaunchedEffect(amountText) {
        val amount = parsedAmount()
        if (maxAmount > 0) {
            sliderValue = (amount.toFloat() / maxAmount.toFloat()).coerceIn(0f, 1f)
        }
    }

    val amount = parsedAmount()
    val exceedsMax = amount > maxAmount
    val isConfirmEnabled = !isProcessing && amount > 0L && !exceedsMax

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    localError = null
                    if (amount <= 0L) {
                        localError = "Enter a valid amount"
                        return@TextButton
                    }
                    if (exceedsMax) {
                        localError = "Amount exceeds available balance"
                        return@TextButton
                    }

                    coroutineScope.launch {
                        isProcessing = true
                        val success = try {
                            onWithdrawSuspend(amount)
                        } catch (e: Exception) {
                            false
                        }
                        isProcessing = false
                        if (success) {
                            onDismiss()
                        }
                    }
                },
                enabled = isConfirmEnabled
            ) {
                if (isProcessing) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                else Text("Withdraw")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Withdraw Money") },
        text = {
            Column {
                // Amount input field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = exceedsMax
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Available: Rp${NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).format(maxAmount)}",
                    style = MaterialTheme.typography.bodySmall
                )

                // Slider
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Slide to adjust amount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = sliderValue,
                    onValueChange = { newValue ->
                        sliderValue = newValue
                        val newAmount = (newValue * maxAmount).toLong()
                        amountText = newAmount.toString()
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick amount buttons
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Quick amounts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickAmounts.forEach { quickAmount ->
                        OutlinedButton(
                            onClick = {
                                amountText = quickAmount.toString()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = when {
                                    quickAmount >= 1000000L -> "${quickAmount / 1000000}M"
                                    quickAmount >= 1000L -> "${quickAmount / 1000}K"
                                    else -> "$quickAmount"
                                },
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Error message
                if (localError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = localError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyInvestmentDialog(
    pocket: PocketModel,
    onDismiss: () -> Unit,
    onBuySuspend: suspend (name: String, nominal: Long, pricePerUnit: Long, unitAmount: Long) -> Boolean
) {
    var investmentName by remember { mutableStateOf("") }
    var pricePerUnitText by remember { mutableStateOf("") }
    var unitAmountText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val maxAmount = pocket.total

    // Helper to parse amount
    fun parsedAmount(text: String): Long {
        return text.replace("[^0-9]".toRegex(), "").toLongOrNull() ?: 0L
    }

    val pricePerUnit = parsedAmount(pricePerUnitText)
    val unitAmount = parsedAmount(unitAmountText)
    val totalAmount = pricePerUnit * unitAmount
    val exceedsMaxAmount = totalAmount > maxAmount
    val isConfirmEnabled = !isProcessing && investmentName.isNotBlank() && pricePerUnit > 0L && unitAmount > 0L && !exceedsMaxAmount

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    localError = null
                    if (investmentName.isBlank()) {
                        localError = "Please enter an investment name"
                        return@TextButton
                    }
                    if (pricePerUnit <= 0L) {
                        localError = "Please enter a valid price per unit"
                        return@TextButton
                    }
                    if (unitAmount <= 0L) {
                        localError = "Please enter a valid unit amount"
                        return@TextButton
                    }
                    if (exceedsMaxAmount) {
                        localError = "Total amount exceeds available balance"
                        return@TextButton
                    }

                    coroutineScope.launch {
                        isProcessing = true
                        val success = try {
                            onBuySuspend(investmentName, totalAmount, pricePerUnit, unitAmount)
                        } catch (e: Exception) {
                            localError = e.message
                            false
                        }
                        isProcessing = false
                        if (success) {
                            onDismiss()
                        }
                    }
                },
                enabled = isConfirmEnabled
            ) {
                if (isProcessing) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                else Text("Buy")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Buy Investment") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Investment Name
                OutlinedTextField(
                    value = investmentName,
                    onValueChange = {
                        investmentName = it
                        localError = null
                    },
                    label = { Text("Investment Name") },
                    placeholder = { Text("e.g., BBCA, Gold, etc.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Price per unit
                OutlinedTextField(
                    value = pricePerUnitText,
                    onValueChange = {
                        pricePerUnitText = it
                        localError = null
                    },
                    label = { Text("Price per Unit") },
                    placeholder = { Text("Enter price per unit") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    prefix = { Text("Rp ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Unit amount
                OutlinedTextField(
                    value = unitAmountText,
                    onValueChange = {
                        unitAmountText = it
                        localError = null
                    },
                    label = { Text("Number of Units") },
                    placeholder = { Text("Enter number of units") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Show calculated total and available balance
                if (pricePerUnit > 0 && unitAmount > 0) {
                    HorizontalDivider()
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Purchase Summary",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Amount:", fontSize = 13.sp)
                            Text(
                                text = "Rp${NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).format(totalAmount)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (exceedsMaxAmount) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Available Balance:", fontSize = 13.sp)
                            Text(
                                text = "Rp${NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).format(maxAmount)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (exceedsMaxAmount) {
                            Text(
                                text = "⚠ Insufficient balance",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Error message
                if (localError != null) {
                    Text(
                        text = localError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    )
}

@Composable
fun TransactionHistoryItem(
    description: String,
    date: String,
    amount: String,
    label: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = description,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = date,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = amount,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

// Helper to format date string to a user-friendly format
private fun formatTransactionDate(dateString: String): String {
    return try {
        val odt = OffsetDateTime.parse(dateString)
        odt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
    } catch (e: DateTimeParseException) {
        dateString // fallback to original if parsing fails
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellInvestmentDialog(
    pocket: PocketModel,
    allTransactions: List<DataTransactionResponse>,
    onDismiss: () -> Unit,
    onSellSuspend: suspend (investmentName: String, sellPrice: Long, unitAmount: Long, originalBuyCost: Long) -> Boolean
) {
    val coroutineScope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    // Calculate available investments from transactions
    val availableInvestments = remember(allTransactions, pocket.id) {
        val investmentTransactions = allTransactions.filter {
            it.action == "Buy" || it.action == "Sell"
        }

        val investmentMap = mutableMapOf<String, MutableList<DataTransactionResponse>>()
        investmentTransactions.forEach { tx ->
            val name = tx.name
            investmentMap.getOrPut(name) { mutableListOf() }.add(tx)
        }

        investmentMap.mapNotNull { (investmentName, transactions) ->
            var totalUnits = 0L
            var totalCost = 0L
            transactions.forEach { tx ->
                when (tx.action) {
                    "Buy" -> {
                        totalUnits += tx.unitAmount
                        totalCost += tx.nominal
                    }
                    "Sell" -> {
                        totalUnits -= tx.unitAmount
                        totalCost -= tx.nominal
                    }
                }
            }
            if (totalUnits > 0L) {
                Triple(investmentName, totalUnits, totalCost)
            } else {
                null
            }
        }
    }

    var selectedInvestmentIndex by remember { mutableStateOf(0) }
    var expanded by remember { mutableStateOf(false) }
    var sellPriceText by remember { mutableStateOf("") }
    var unitAmountText by remember { mutableStateOf("") }

    val selectedInvestment = if (availableInvestments.isNotEmpty() && selectedInvestmentIndex < availableInvestments.size) {
        availableInvestments[selectedInvestmentIndex]
    } else null

    fun parsedAmount(text: String): Long {
        return text.replace("[^0-9]".toRegex(), "").toLongOrNull() ?: 0L
    }

    val sellPrice = parsedAmount(sellPriceText)
    val unitAmount = parsedAmount(unitAmountText)
    val maxUnits = selectedInvestment?.second ?: 0L
    val originalBuyCost = selectedInvestment?.third ?: 0L
    val unitCost = if (maxUnits > 0L) originalBuyCost / maxUnits else 0L
    val totalOriginalCost = unitCost * unitAmount

    val exceedsMaxUnits = unitAmount > maxUnits
    val isConfirmEnabled = !isProcessing && selectedInvestment != null && sellPrice > 0L && unitAmount > 0L && !exceedsMaxUnits

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    localError = null
                    if (selectedInvestment == null) {
                        localError = "Please select an investment"
                        return@TextButton
                    }
                    if (sellPrice <= 0L) {
                        localError = "Please enter a valid sell price per unit"
                        return@TextButton
                    }
                    if (unitAmount <= 0L) {
                        localError = "Please enter a valid unit amount"
                        return@TextButton
                    }
                    if (exceedsMaxUnits) {
                        localError = "Unit amount exceeds available units"
                        return@TextButton
                    }

                    coroutineScope.launch {
                        isProcessing = true
                        val success = try {
                            onSellSuspend(selectedInvestment.first, sellPrice, unitAmount, totalOriginalCost)
                        } catch (e: Exception) {
                            localError = e.message
                            false
                        }
                        isProcessing = false
                        if (success) {
                            onDismiss()
                        }
                    }
                },
                enabled = isConfirmEnabled
            ) {
                if (isProcessing) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                else Text("Sell")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Sell Investment") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (availableInvestments.isEmpty()) {
                    Text(
                        text = "No active investments to sell",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    // Investment Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedInvestment?.first ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Investment") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            availableInvestments.forEachIndexed { index, (name, units, _) ->
                                DropdownMenuItem(
                                    text = { Text("$name ($units units)") },
                                    onClick = {
                                        selectedInvestmentIndex = index
                                        expanded = false
                                        localError = null
                                    }
                                )
                            }
                        }
                    }

                    // Show available units
                    if (selectedInvestment != null) {
                        Text(
                            text = "Available Units: ${selectedInvestment.second}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Sell Price Per Unit
                    OutlinedTextField(
                        value = sellPriceText,
                        onValueChange = {
                            sellPriceText = it
                            localError = null
                        },
                        label = { Text("Sell Price Per Unit") },
                        placeholder = { Text("Enter sell price") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        prefix = { Text("Rp ") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Unit Amount
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = unitAmountText,
                            onValueChange = {
                                unitAmountText = it
                                localError = null
                            },
                            label = { Text("Units to Sell") },
                            placeholder = { Text("Enter units") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            isError = exceedsMaxUnits
                        )

                        // Sell All button
                        Button(
                            onClick = {
                                unitAmountText = maxUnits.toString()
                                localError = null
                            },
                            enabled = maxUnits > 0L,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("All")
                        }
                    }

                    // Preview
                    if (sellPrice > 0L && unitAmount > 0L && !exceedsMaxUnits) {
                        HorizontalDivider()
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Transaction Preview",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Sell Value:", fontSize = 13.sp)
                                Text(
                                    text = "Rp${NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).format(sellPrice * unitAmount)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF37c447)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Original Cost:", fontSize = 13.sp)
                                Text(
                                    text = "Rp${NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).format(totalOriginalCost)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val profit = (sellPrice * unitAmount) - totalOriginalCost
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Profit/Loss:", fontSize = 13.sp)
                                Text(
                                    text = "Rp${NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).format(profit)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (profit >= 0) Color(0xFF37c447) else Color(0xFFE74C3C)
                                )
                            }
                        }
                    }

                    // Error message
                    if (localError != null) {
                        Text(
                            text = localError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    )
}
