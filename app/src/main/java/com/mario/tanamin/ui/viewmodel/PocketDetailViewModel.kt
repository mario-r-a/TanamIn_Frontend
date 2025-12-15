package com.mario.tanamin.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mario.tanamin.data.container.TanamInContainer
import com.mario.tanamin.data.repository.TanamInRepository
import com.mario.tanamin.ui.model.PocketModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for PocketDetail screen.
 * - Manages a single pocket's details
 * - Will later handle transactions for this pocket
 */
class PocketDetailViewModel(
    private val repository: TanamInRepository = TanamInContainer().tanamInRepository
) : ViewModel() {

    private val _pocket = MutableStateFlow<PocketModel?>(null)
    val pocket: StateFlow<PocketModel?> = _pocket.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // List of available target pockets for moving money (active, not named "Active Investments")
    private val _availableTargets = MutableStateFlow<List<PocketModel>>(emptyList())
    val availableTargets: StateFlow<List<PocketModel>> = _availableTargets.asStateFlow()

    // Simple one-shot message flow to notify UI of results (success/failure).
    private val _messageFlow = MutableSharedFlow<String>()
    val messageFlow: SharedFlow<String> = _messageFlow.asSharedFlow()

    // TODO: Add transactions list state when backend is ready
    // private val _transactions = MutableStateFlow<List<TransactionModel>>(emptyList())
    // val transactions: StateFlow<List<TransactionModel>> = _transactions.asStateFlow()

    /**
     * Load a specific pocket by ID and populate available targets.
     */
    fun loadPocket(pocketId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val userId = com.mario.tanamin.data.session.InMemorySessionHolder.userId?.toIntOrNull()
                if (userId != null) {
                    repository.getPocketsByUser(userId)
                        .onSuccess { pockets ->
                            val foundPocket = pockets.firstOrNull { it.id == pocketId }
                            if (foundPocket != null) {
                                _pocket.value = foundPocket
                                Log.d("PocketDetailViewModel", "Loaded pocket: ${foundPocket.name}")
                                // compute available targets (active and not named "Active Investments" and not the current pocket)
                                val targets = pockets.filter { p ->
                                    p.isActive && !p.name.equals("Active Investments", ignoreCase = true) && p.id != pocketId
                                }
                                _availableTargets.value = targets
                            } else {
                                _error.value = "Pocket not found"
                                Log.d("PocketDetailViewModel", "Pocket with id=$pocketId not found")
                                _availableTargets.value = emptyList()
                            }
                        }
                        .onFailure { ex ->
                            _error.value = ex.message ?: "Failed to load pocket"
                            _availableTargets.value = emptyList()
                            Log.e("PocketDetailViewModel", "Failed to load pocket", ex)
                        }
                } else {
                    _error.value = "User not logged in"
                    _availableTargets.value = emptyList()
                    Log.e("PocketDetailViewModel", "No user id available")
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load pocket"
                _availableTargets.value = emptyList()
                Log.e("PocketDetailViewModel", "Exception loading pocket", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Transfer money from the current pocket to a target pocket (local optimistic update).
     * This is a UI-level operation for now because backend endpoint isn't available in repository.
     */
    fun transferMoney(toPocketId: Int, amount: Long) {
        viewModelScope.launch {
            val from = _pocket.value
            if (from == null) {
                _messageFlow.emit("Source pocket not loaded")
                return@launch
            }
            if (amount <= 0L) {
                _messageFlow.emit("Amount must be greater than zero")
                return@launch
            }
            if (amount > from.total) {
                _messageFlow.emit("Insufficient balance")
                return@launch
            }

            try {
                // Optimistically update the pocket total locally
                val newFrom = from.copy(total = from.total - amount)
                _pocket.value = newFrom

                // Optionally update availableTargets list (if target present, we won't update its total locally)
                _messageFlow.emit("Moved Rp${amount} successfully")
                Log.d("PocketDetailViewModel", "Transferred $amount from ${from.id} to $toPocketId")
            } catch (e: Exception) {
                _messageFlow.emit("Failed to move money: ${e.message}")
                Log.e("PocketDetailViewModel", "transferMoney exception", e)
            }
        }
    }

    fun loadTransactions(pocketId: Int) {
        // Will be implemented when transaction endpoints are ready
    }

    fun clear() {
        _pocket.value = null
        _error.value = null
        _isLoading.value = false
        _availableTargets.value = emptyList()
    }
}
