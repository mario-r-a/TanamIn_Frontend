package com.mario.tanamin.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mario.tanamin.data.container.TanamInContainer
import com.mario.tanamin.data.dto.ThemeResponse
import com.mario.tanamin.data.repository.TanamInRepository
import com.mario.tanamin.data.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ThemeShopUiState {
    object Loading : ThemeShopUiState()
    data class Success(val themes: List<ThemeResponse>) : ThemeShopUiState()
    data class Error(val message: String) : ThemeShopUiState()
}

class ThemeShopViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TanamInRepository = TanamInContainer.tanamInRepository
    private val sessionManager = SessionManager(application)

    private val _uiState = MutableStateFlow<ThemeShopUiState>(ThemeShopUiState.Loading)
    val uiState: StateFlow<ThemeShopUiState> = _uiState.asStateFlow()

    private val _purchaseState = MutableStateFlow<String?>(null) // Success message or null
    val purchaseState: StateFlow<String?> = _purchaseState.asStateFlow()

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    init {
        loadThemes()
    }

    fun loadThemes() {
        viewModelScope.launch {
            _uiState.value = ThemeShopUiState.Loading
            repository.getThemes().collect { result ->
                result.onSuccess { response ->
                    _uiState.value = ThemeShopUiState.Success(response.data)
                }.onFailure {
                    _uiState.value = ThemeShopUiState.Error(it.message ?: "Failed to load themes")
                }
            }
        }
    }

    fun purchaseTheme(themeId: Int, appViewModel: AppViewModel) {
        viewModelScope.launch {
            repository.purchaseTheme(themeId).collect { result ->
                result.onSuccess {
                    _purchaseState.value = "Theme purchased successfully!"
                    loadThemes() // Refresh list to show unlocked status
                    appViewModel.refreshTheme() // In case current coins affect something else
                }.onFailure {
                    _errorState.value = it.message ?: "Failed to purchase theme"
                }
            }
        }
    }

    fun equipTheme(themeId: Int, appViewModel: AppViewModel) {
        viewModelScope.launch {
            repository.activateTheme(themeId).collect { result ->
                result.onSuccess {
                    _purchaseState.value = "Theme activated!"
                    appViewModel.refreshTheme() // This updates the global theme
                    loadThemes()
                }.onFailure {
                    _errorState.value = it.message ?: "Failed to activate theme"
                }
            }
        }
    }

    fun clearMessages() {
        _purchaseState.value = null
        _errorState.value = null
    }
}
