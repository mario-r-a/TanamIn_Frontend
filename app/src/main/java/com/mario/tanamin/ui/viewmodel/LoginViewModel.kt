package com.mario.tanamin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mario.tanamin.data.container.TanamInContainer
import com.mario.tanamin.data.dto.LoginResponse
import com.mario.tanamin.data.repository.TanamInRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val response: LoginResponse) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel : ViewModel() {
    private val repository: TanamInRepository = TanamInContainer().tanamInRepository

    private val _loginState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginState: StateFlow<LoginUiState> = _loginState

    fun login(username: String, password: String) {
        _loginState.value = LoginUiState.Loading
        viewModelScope.launch {
            val result = repository.login(username, password)
            result.onSuccess { response ->
                _loginState.value = LoginUiState.Success(response)
            }.onFailure { exception ->
                _loginState.value = LoginUiState.Error(exception.message ?: "Unknown error")
            }
        }
    }
}