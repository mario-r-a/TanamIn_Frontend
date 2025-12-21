package com.mario.tanamin.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mario.tanamin.data.dto.ThemeResponse
import com.mario.tanamin.ui.theme.safeParseColor
import com.mario.tanamin.ui.viewmodel.AppViewModel
import com.mario.tanamin.ui.viewmodel.ThemeShopUiState
import com.mario.tanamin.ui.viewmodel.ThemeShopViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeShopScreen(
    navController: NavController,
    viewModel: ThemeShopViewModel = viewModel(),
    appViewModel: AppViewModel // To pass to VM for global updates
) {
    val uiState by viewModel.uiState.collectAsState()
    val purchaseMessage by viewModel.purchaseState.collectAsState()
    val errorMessage by viewModel.errorState.collectAsState()
    val activeTheme by appViewModel.activeTheme.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(purchaseMessage) {
        purchaseMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Theme Shop", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)) {

            when (val state = uiState) {
                is ThemeShopUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ThemeShopUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is ThemeShopUiState.Success -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(state.themes) { theme ->
                            ThemeItem(
                                theme = theme,
                                isActive = activeTheme?.id == theme.id,
                                onBuy = { viewModel.purchaseTheme(theme.id, appViewModel) },
                                onEquip = { viewModel.equipTheme(theme.id, appViewModel) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeItem(
    theme: ThemeResponse,
    isActive: Boolean,
    onBuy: () -> Unit,
    onEquip: () -> Unit
) {
    val primaryColor = safeParseColor(theme.primary, Color.Gray)
    val backgroundColor = safeParseColor(theme.background, Color.White)
    val textColor = safeParseColor(theme.text, Color.Black)

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Preview Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(backgroundColor)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Mock UI for preview
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(primaryColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .height(12.dp)
                            .width(80.dp)
                            .background(textColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    )
                }
                
                if (isActive) {
                     Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .background(Color.Green, CircleShape)
                            .padding(4.dp)
                    ) {
                         Icon(Icons.Default.Check, contentDescription = "Active", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Action Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (theme.unlocked) {
                    if (isActive) {
                        Button(
                            onClick = {},
                            enabled = false,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            modifier = Modifier.fillMaxWidth().height(36.dp)
                        ) {
                            Text("Equipped", fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = onEquip,
                            modifier = Modifier.fillMaxWidth().height(36.dp)
                        ) {
                            Text("Equip", fontSize = 12.sp)
                        }
                    }
                } else {
                    Button(
                        onClick = onBuy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)), // Gold/Orange for buy
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${theme.price}", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
