package com.mario.tanamin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mario.tanamin.data.container.TanamInContainer
import com.mario.tanamin.data.dto.DataProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val profile: DataProfile) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

class ProfileViewModel : ViewModel() {
    private val repository = TanamInContainer().tanamInRepository

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        fetchProfile()
    }

    fun fetchProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            
            // repository.getProfile() will use the AuthInterceptor automatically if the token is in InMemorySessionHolder.
            // Ensure Login has happened before accessing this screen.
            
            val result = repository.getProfile()
            result.onSuccess { profile ->
                _uiState.value = ProfileUiState.Success(profile)
            }.onFailure { exception ->
                _uiState.value = ProfileUiState.Error(exception.message ?: "Unknown error")
            }
        }
    }
}
