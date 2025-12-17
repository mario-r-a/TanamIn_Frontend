package com.mario.tanamin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mario.tanamin.data.container.TanamInContainer
import com.mario.tanamin.data.dto.DataLevel
import com.mario.tanamin.data.repository.TanamInRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CourseViewModel(
    private val repository: TanamInRepository = TanamInContainer().tanamInRepository
) : ViewModel() {

    private val _levels = MutableStateFlow<List<DataLevel>>(emptyList())
    val levels: StateFlow<List<DataLevel>> = _levels.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _coins = MutableStateFlow(0)
    val coins: StateFlow<Int> = _coins.asStateFlow()

    private val _streak = MutableStateFlow(0)
    val streak: StateFlow<Int> = _streak.asStateFlow()

    init {
        loadLevels()
        loadProfile()
    }

    fun loadLevels() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val result = repository.getLevelsByUserDto()
                result.fold(
                    onSuccess = { data ->
                        // Sorting (urutin level berdasarkan ID)
                        _levels.value = data.sortedBy { it.id }
                    },
                    onFailure = { t -> _errorMessage.value = t.message ?: "Unknown error" }
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun loadProfile() {
        viewModelScope.launch {
            try {
                val result = repository.getProfile()
                result.fold(
                    onSuccess = { profile ->
                        _coins.value = profile.coin
                        _streak.value = profile.streak
                    },
                    onFailure = { t ->
                        _errorMessage.value = t.message ?: "Failed to load profile"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load profile"
            }
        }
    }
}
