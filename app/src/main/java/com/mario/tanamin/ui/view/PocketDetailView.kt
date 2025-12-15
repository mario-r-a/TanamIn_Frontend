package com.mario.tanamin.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PocketDetailView(
    navController: NavController,
    pocketId: Int,
    viewModel: PocketDetailViewModel = viewModel()
) {
    val pocket by viewModel.pocket.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val availableTargets by viewModel.availableTargets.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

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
            // Do NOT apply innerPadding to the full content container so the header
            // can extend to the top. Apply innerPadding only to the scrollable content below.
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
                    // Pass innerPadding to the content so lists/scrollables will not be obscured by
                    // navigation bars / snackbar areas.
                    PocketDetailContent(
                        pocket = pocket!!,
                        onMoveClicked = { showMoveDialog = true },
                        contentPadding = innerPadding
                    )
                }

                if (showMoveDialog) {
                    MoveMoneyDialog(
                        availableTargets = availableTargets,
                        onDismiss = { showMoveDialog = false },
                        onMove = { toPocketId, amount ->
                            viewModel.transferMoney(toPocketId, amount)
                            showMoveDialog = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PocketDetailContent(
    pocket: com.mario.tanamin.ui.model.PocketModel,
    onMoveClicked: () -> Unit,
    contentPadding: PaddingValues = PaddingValues()
) {
    val formattedTotal = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).format(pocket.total)

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
                    .statusBarsPadding(), // respect the status bar, so the orange header visually starts under it
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

                // Move Money Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .clickable { onMoveClicked() }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Move Money",
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

        // History Section
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding) // ensure scaffold insets are respected for the scrollable area
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
        ) {
            item {
                Text(
                    text = "History",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF222B45)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

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
    onDismiss: () -> Unit,
    onMove: (toPocketId: Int, amount: Long) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedTarget by remember { mutableStateOf<PocketModel?>(if (availableTargets.isNotEmpty()) availableTargets[0] else null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val amount = amountText.replace("[^0-9]".toRegex(), "").toLongOrNull() ?: 0L
                val toId = selectedTarget?.id
                if (toId != null) {
                    onMove(toId, amount)
                }
            }) {
                Text("Move")
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
