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
import kotlinx.coroutines.launch

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

    val snackbarHostState = remember { SnackbarHostState() }
    // coroutineScope not needed here (snackbar uses LaunchedEffect)

    // Collect one-shot messages from the ViewModel
    LaunchedEffect(Unit) {
        viewModel.messageFlow.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    var showMoveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(pocketId) {
        viewModel.loadPocket(pocketId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Large decorative circle at bottom
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(y = 350.dp)
                .align(Alignment.BottomCenter)
                .background(Color(0xFFE8D5F2), shape = CircleShape)
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
                        CircularProgressIndicator(color = Color(0xFFFFB86C))
                    }
                } else if (error != null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = error ?: "Unknown error",
                                color = Color.Red,
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
                        onMoveClicked = { showMoveDialog = true },
                        onSellClicked = onSell,
                        onBuyClicked = onBuy,
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
            }
        }
    }
}

@Composable
private fun PocketDetailContent(
    pocket: PocketModel,
    onMoveClicked: () -> Unit,
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
                .background(Color(0xFFFFB86C))
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
                    color = Color(0xFF222B45),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Rp$formattedTotal",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF222B45),
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
                                Text("Move Money", color = Color(0xFF222B45), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFF222B45), modifier = Modifier.size(16.dp))
                            }
                        }

                        // Buy Investment button (was Sell Investment)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFFFFF7ED))
                                .clickable { onBuyClicked() }
                                .padding(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("Buy Investment", color = Color(0xFF222B45), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFF222B45), modifier = Modifier.size(16.dp))
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
                                color = Color(0xFF222B45),
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color(0xFF222B45),
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
                color = Color(0xFF222B45),
                modifier = Modifier.padding(top = 42.dp)
            )
        }

        // Scrollable list of history items only (header is outside so it won't scroll)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding) // ensure scaffold insets are respected for the scrollable area
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // TODO: Replace with actual transactions from ViewModel
            items(4) { _ ->
                TransactionHistoryItem(
                    description = "Beli Saham BBCA - 3 lot x Rp 30.000",
                    date = "10 Des 2025, 13:04",
                    amount = "Rp. 30.000",
                    label = "Buy"
                )
                Spacer(modifier = Modifier.height(12.dp))
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
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (maxAmount != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Available: Rp${NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).format(maxAmount)}", style = MaterialTheme.typography.bodySmall)
                }
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
                        modifier = Modifier.fillMaxWidth()
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
                if (localError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = localError ?: "", color = Color.Red, style = MaterialTheme.typography.bodySmall)
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
                color = Color(0xFF222B45)
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
                color = Color(0xFF222B45)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFFDDB3))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF222B45)
                )
            }
        }
    }
}
