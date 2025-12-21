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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TanamInRepository = TanamInContainer.tanamInRepository
    private val sessionManager = SessionManager(application)

    private val _activeTheme = MutableStateFlow<ThemeResponse?>(null)
    val activeTheme: StateFlow<ThemeResponse?> = _activeTheme.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.userIdFlow.collectLatest { userId ->
                if (userId != null) {
                    refreshTheme()
                } else {
                    _activeTheme.value = null
                }
            }
        }
    }

    fun refreshTheme() {
        viewModelScope.launch {
            repository.getActiveTheme().collect { result ->
                result.onSuccess { response ->
                    _activeTheme.value = response.data
                }.onFailure {
                    // Fail silently or log, default theme will be used
                }
            }
        }
    }
}
