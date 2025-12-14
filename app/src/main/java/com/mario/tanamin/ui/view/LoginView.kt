package com.mario.tanamin.ui.view

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mario.tanamin.ui.route.AppView
import com.mario.tanamin.ui.route.TanamInAppRoute
import com.mario.tanamin.ui.viewmodel.LoginUiState
import com.mario.tanamin.ui.viewmodel.LoginViewModel

@Composable
fun LoginView(
    navController: NavController,
    loginViewModel: LoginViewModel = viewModel()
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val loginState by loginViewModel.loginState.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(text = "Login", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { loginViewModel.login(username, password) },
                modifier = Modifier.fillMaxWidth(),
                enabled = loginState !is LoginUiState.Loading
            ) {
                Text("Login")
            }
            Spacer(modifier = Modifier.height(16.dp))
            when (loginState) {
                is LoginUiState.Loading -> CircularProgressIndicator()
                is LoginUiState.Error -> Text(
                    text = (loginState as LoginUiState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
                is LoginUiState.Success -> {
                    // Navigate to Home on success
                    LaunchedEffect(Unit) {
                        navController.navigate(AppView.Home.name) {
                            popUpTo(AppView.Login.name) { inclusive = true }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}