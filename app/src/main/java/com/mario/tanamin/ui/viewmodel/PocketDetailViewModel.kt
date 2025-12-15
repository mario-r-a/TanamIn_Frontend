package com.mario.tanamin.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mario.tanamin.data.container.TanamInContainer
import com.mario.tanamin.data.dto.DataPocketUpdate
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
     * Helper: check whether current pocket has at least `amount` available to transfer.
     */
    fun canMove(amount: Long): Boolean {
        val from = _pocket.value
        return when {
            from == null -> false
            amount <= 0L -> false
            else -> from.total >= amount
        }
    }

    /**
     * Helper: check whether a target pocket id is present among available targets.
     */
    fun canMoveTo(toPocketId: Int): Boolean {
        return _availableTargets.value.any { it.id == toPocketId }
    }

    /**
     * Suspended implementation that performs validation, optimistic update and repository PATCH calls.
     * Returns true when the whole transfer completed successfully.
     */
    suspend fun transferMoneySuspend(toPocketId: Int, amount: Long): Boolean {
        val from = _pocket.value
        if (from == null) {
            _messageFlow.emit("Source pocket not loaded")
            return false
        }
        if (amount <= 0L) {
            _messageFlow.emit("Amount must be greater than zero")
            return false
        }
        if (!canMove(amount)) {
            _messageFlow.emit("Insufficient balance")
            return false
        }
        if (!canMoveTo(toPocketId)) {
            _messageFlow.emit("Target pocket not available")
            return false
        }

        _isLoading.value = true
        try {
            // Optimistic update local pocket
            val originalFrom = from
            _pocket.value = from.copy(total = from.total - amount)

            // Prepare patches for both pockets
            val updatedFromDto = DataPocketUpdate(
                id = from.id,
                isActive = from.isActive,
                name = from.name,
                total = (from.total - amount).toInt(),
                userId = com.mario.tanamin.data.session.InMemorySessionHolder.userId?.toIntOrNull() ?: 0,
                walletType = from.walletType
            )

            val target = _availableTargets.value.firstOrNull { it.id == toPocketId }
            val targetCurrentTotal = target?.total ?: 0L
            val updatedToDto = DataPocketUpdate(
                id = toPocketId,
                isActive = target?.isActive ?: true,
                name = target?.name ?: "Pocket",
                total = (targetCurrentTotal + amount).toInt(),
                userId = com.mario.tanamin.data.session.InMemorySessionHolder.userId?.toIntOrNull() ?: 0,
                walletType = target?.walletType ?: "Main"
            )

            // Send PATCH for source
            val resFrom = repository.updatePocket(updatedFromDto)
            if (resFrom.isFailure) {
                _pocket.value = originalFrom
                _messageFlow.emit("Failed to move money: ${resFrom.exceptionOrNull()?.message}")
                return false
            }
            val updatedFrom = resFrom.getOrNull()!!
            _pocket.value = updatedFrom

            // Send PATCH for destination
            val resTo = repository.updatePocket(updatedToDto)
            if (resTo.isFailure) {
                _messageFlow.emit("Moved money but failed to update destination: ${resTo.exceptionOrNull()?.message}")
                return false
            }

            // update availableTargets with the returned destination pocket model so detail view reflects new total immediately
            val updatedTo = resTo.getOrNull()!!
            val newTargets = _availableTargets.value.map { if (it.id == updatedTo.id) updatedTo else it }
            _availableTargets.value = newTargets

            // Reload the current pocket from server to ensure we have the authoritative state
            // (in case server modified other fields or we want to be certain of the final total)
            val currentPocketId = from.id
            val userId = com.mario.tanamin.data.session.InMemorySessionHolder.userId?.toIntOrNull()
            if (userId != null) {
                repository.getPocketsByUser(userId)
                    .onSuccess { pockets ->
                        val refreshedPocket = pockets.firstOrNull { it.id == currentPocketId }
                        if (refreshedPocket != null) {
                            _pocket.value = refreshedPocket
                            // Also refresh available targets
                            val targets = pockets.filter { p ->
                                p.isActive && !p.name.equals("Active Investments", ignoreCase = true) && p.id != currentPocketId
                            }
                            _availableTargets.value = targets
                        }
                    }
            }

            _messageFlow.emit("Moved Rp$amount successfully")
            return true
        } catch (e: Exception) {
            _messageFlow.emit("Transfer failed: ${e.message}")
            return false
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Transfer money from the current pocket to a target pocket (with validation and optimistic update).
     * Delegates to suspend implementation.
     */
    fun moveMoney(toPocketId: Int, amount: Long) {
        viewModelScope.launch {
            transferMoneySuspend(toPocketId, amount)
        }
    }


    /**
     * Public wrapper kept for compatibility with view calls (PocketDetailView) — delegates to moveMoney.
     * This ensures callers that use `transferMoney(...)` benefit from the same validation (balance, target availability).
     */
    fun transferMoney(toPocketId: Int, amount: Long) {
        // Reuse existing validation inside moveMoney; simply delegate.
        moveMoney(toPocketId, amount)
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
