package com.mario.tanamin.ui.view

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    val uiTransactions by viewModel.uiTransactions.collectAsState() // Use uiTransactions

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
                        onMoveClicked = { showMoveDialog = true },
                        onWithdrawClicked = { showWithdrawDialog = true },
                        onSellClicked = onSell,
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
            }
        }
    }
}

@Composable
private fun PocketDetailContent(
    pocket: PocketModel,
    transactions: List<PocketTransactionModel>, // Use PocketTransactionModel
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
    val normalizedName = pocket.name
        .lowercase(Locale.getDefault())
        .replace(Regex("[^a-z0-9\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

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
    var amountText by remember { mutableStateOf("") }
    var pricePerUnitText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val maxAmount = pocket.total

    // Helper to parse amount
    fun parsedAmount(text: String): Long {
        return text.replace("[^0-9]".toRegex(), "").toLongOrNull() ?: 0L
    }

    val amount = parsedAmount(amountText)
    val pricePerUnit = parsedAmount(pricePerUnitText)
    val unitAmount = if (pricePerUnit > 0) amount / pricePerUnit else 0L
    val exceedsMax = amount > maxAmount
    val isConfirmEnabled = !isProcessing && investmentName.isNotBlank() && amount > 0L && pricePerUnit > 0L && !exceedsMax

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
                    if (amount <= 0L) {
                        localError = "Please enter a valid amount"
                        return@TextButton
                    }
                    if (pricePerUnit <= 0L) {
                        localError = "Please enter a valid price per unit"
                        return@TextButton
                    }
                    if (exceedsMax) {
                        localError = "Amount exceeds available balance"
                        return@TextButton
                    }

                    coroutineScope.launch {
                        isProcessing = true
                        val success = try {
                            onBuySuspend(investmentName, amount, pricePerUnit, unitAmount)
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
            Column {
                // Investment Name
                OutlinedTextField(
                    value = investmentName,
                    onValueChange = {
                        investmentName = it
                        localError = null
                    },
                    label = { Text("Investment Name") },
                    placeholder = { Text("e.g., Gold, Stocks, etc.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Amount input field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        localError = null
                    },
                    label = { Text("Amount (Rp)") },
                    placeholder = { Text("Total investment amount") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = exceedsMax
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Available: Rp${NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).format(maxAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (exceedsMax) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Price per unit
                OutlinedTextField(
                    value = pricePerUnitText,
                    onValueChange = {
                        pricePerUnitText = it
                        localError = null
                    },
                    label = { Text("Price per Unit (Rp)") },
                    placeholder = { Text("Price of one unit") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Show calculated units
                if (amount > 0 && pricePerUnit > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Units: ${unitAmount}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
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
