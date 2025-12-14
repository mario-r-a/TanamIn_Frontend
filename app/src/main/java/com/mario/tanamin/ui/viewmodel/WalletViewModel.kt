package com.mario.tanamin.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mario.tanamin.data.container.TanamInContainer
import com.mario.tanamin.data.dto.DataPocket
import com.mario.tanamin.data.repository.TanamInRepository
import com.mario.tanamin.data.session.InMemorySessionHolder
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for Wallets (pockets).
 * - Exposes pockets list, loading state and error message as StateFlows.
 * - Use loadPocketsFor(userId) if you pass the user id explicitly.
 * - Use loadPocketsFromInMemoryUser() to read the user id from InMemorySessionHolder (set on login).
 */
class WalletViewModel(
    private val repository: TanamInRepository = TanamInContainer().tanamInRepository
) : ViewModel() {

    private val _pockets = MutableStateFlow<List<DataPocket>>(emptyList())
    val pockets: StateFlow<List<DataPocket>> = _pockets.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Derived reactive total: sums active Main pockets; UI can collect this to guarantee updates.
    val mainTotalFlow: StateFlow<Long> = _pockets
        .map { list ->
            list.filter { p ->
                val wt = (p.walletType ?: "").trim()
                p.isActive && wt.equals("Main", ignoreCase = true)
            }.sumOf { it.total.toLong() }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    /** Load pockets for a given user id (preferred: explicit). */
    fun loadPocketsFor(userId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                repository.getPocketsByUser(userId)
                    .onSuccess { list ->
                        _pockets.value = list
                        // Log derived total for debug
                        val total = list.filter { p ->
                            val wt = (p.walletType ?: "").trim()
                            p.isActive && wt.equals("Main", ignoreCase = true)
                        }.sumOf { it.total.toLong() }
                        Log.d("WalletViewModel", "Loaded ${list.size} pockets; main active total=$total")
                    }
                    .onFailure { ex ->
                        _error.value = ex.message ?: "Failed to load pockets"
                        Log.d("WalletViewModel", "Failed to load pockets: ${ex.message}")
                    }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load pockets"
                Log.e("WalletViewModel", "Exception loading pockets", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Convenience: load pockets using the in-memory userId (set by SessionManager.saveSession). */
    fun loadPocketsFromInMemoryUser() {
        val userIdStr = InMemorySessionHolder.userId
        val id = userIdStr?.toIntOrNull()
        if (id == null) {
            _error.value = "No user id available in memory"
            Log.d("WalletViewModel", "No user id available in memory when trying to load pockets")
            return
        }
        loadPocketsFor(id)
    }

    fun clear() {
        _pockets.value = emptyList()
        _error.value = null
        _isLoading.value = false
    }
}
