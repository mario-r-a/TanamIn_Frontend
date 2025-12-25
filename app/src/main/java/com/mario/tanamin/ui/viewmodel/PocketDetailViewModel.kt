package com.mario.tanamin.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mario.tanamin.data.container.TanamInContainer
import com.mario.tanamin.data.dto.AddTransactionRequest
import com.mario.tanamin.data.dto.DataPocketUpdate
import com.mario.tanamin.data.dto.DataTransactionResponse
import com.mario.tanamin.data.repository.TanamInRepository
import com.mario.tanamin.ui.model.PocketModel
import com.mario.tanamin.ui.model.PocketTransactionModel
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

    // Transactions state for this pocket
    private val _transactions = MutableStateFlow<List<DataTransactionResponse>>(emptyList())
    val transactions: StateFlow<List<DataTransactionResponse>> = _transactions.asStateFlow()

    // UI Transactions state for this pocket, enriched with transfer details
    private val _uiTransactions = MutableStateFlow<List<PocketTransactionModel>>(emptyList())
    val uiTransactions: StateFlow<List<PocketTransactionModel>> = _uiTransactions.asStateFlow()

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

            // Create transaction for the transfer
            val transferTransaction = AddTransactionRequest(
                action = "Transfer",
                name = "Pocket Transfer",
                nominal = amount.toInt(),
                pocketId = from.id,
                pricePerUnit = 1,
                toPocketId = toPocketId,
                unitAmount = amount.toInt()
            )
            val transferResult = repository.addTransaction(transferTransaction)
            transferResult.onFailure { ex ->
                _messageFlow.emit("Money moved but failed to record transaction: ${ex.message}")
                return false
            }

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

            // Reload transactions for this pocket to update the list immediately
            loadTransactions(currentPocketId)

            return true
        } catch (e: Exception) {
            _messageFlow.emit("Transfer failed: ${e.message}")
            return false
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Withdraw money from the current pocket.
     * Validates balance, updates the pocket via API, creates a transaction, and reloads data.
     */
    suspend fun withdrawMoneySuspend(amount: Long): Boolean {
        val from = _pocket.value
        if (from == null) {
            _messageFlow.emit("Pocket not loaded")
            return false
        }
        if (amount <= 0L) {
            _messageFlow.emit("Amount must be greater than zero")
            return false
        }
        if (from.total < amount) {
            _messageFlow.emit("Insufficient balance")
            return false
        }

        _isLoading.value = true
        try {
            // Optimistic update local pocket
            val originalFrom = from
            _pocket.value = from.copy(total = from.total - amount)

            // Prepare patch for the pocket
            val updatedPocketDto = DataPocketUpdate(
                id = from.id,
                isActive = from.isActive,
                name = from.name,
                total = (from.total - amount).toInt(),
                userId = com.mario.tanamin.data.session.InMemorySessionHolder.userId?.toIntOrNull() ?: 0,
                walletType = from.walletType
            )

            // Send PATCH for pocket
            val resFrom = repository.updatePocket(updatedPocketDto)
            if (resFrom.isFailure) {
                _pocket.value = originalFrom
                _messageFlow.emit("Failed to withdraw: ${resFrom.exceptionOrNull()?.message}")
                return false
            }

            // Create transaction for the withdrawal
            val withdrawTransaction = AddTransactionRequest(
                action = "Withdraw",
                name = "Withdraw",
                nominal = amount.toInt(),
                pocketId = from.id,
                pricePerUnit = 1,
                toPocketId = null,
                unitAmount = amount.toInt()
            )
            val withdrawResult = repository.addTransaction(withdrawTransaction)
            withdrawResult.onFailure { ex ->
                _messageFlow.emit("Money withdrawn but failed to record transaction: ${ex.message}")
                return false
            }

            // Reload the current pocket from server
            val currentPocketId = from.id
            val userId = com.mario.tanamin.data.session.InMemorySessionHolder.userId?.toIntOrNull()
            if (userId != null) {
                repository.getPocketsByUser(userId)
                    .onSuccess { pockets ->
                        val refreshedPocket = pockets.firstOrNull { it.id == currentPocketId }
                        if (refreshedPocket != null) {
                            _pocket.value = refreshedPocket
                        }
                    }
            }

            _messageFlow.emit("Withdrawn Rp$amount successfully")

            // Reload transactions for this pocket to update the list immediately
            loadTransactions(currentPocketId)

            return true
        } catch (e: Exception) {
            _messageFlow.emit("Withdraw failed: ${e.message}")
            return false
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Buy investment by creating a transaction with action "Buy".
     * Validates that user has enough balance in the pocket.
     */
    suspend fun buyInvestmentSuspend(name: String, nominal: Long, pricePerUnit: Long, unitAmount: Long): Boolean {
        val pocket = _pocket.value
        if (pocket == null) {
            _messageFlow.emit("Pocket not loaded")
            return false
        }
        if (nominal <= 0L) {
            _messageFlow.emit("Amount must be greater than zero")
            return false
        }
        if (pocket.total < nominal) {
            _messageFlow.emit("Insufficient balance in pocket")
            return false
        }

        _isLoading.value = true
        try {
            // Find active investment pocket
            val userId = com.mario.tanamin.data.session.InMemorySessionHolder.userId?.toIntOrNull()
            val pocketsResult = if (userId != null) repository.getPocketsByUser(userId) else Result.success(emptyList())
            if (pocketsResult.isFailure) {
                _messageFlow.emit("Failed to find active investment pocket")
                return false
            }
            val pockets = pocketsResult.getOrNull() ?: emptyList()
            val activeInvestmentPocket = pockets.firstOrNull { it.walletType.contains("invest", true) && it.isActive && it.id != pocket.id }
            if (activeInvestmentPocket == null) {
                _messageFlow.emit("Active investment pocket not found")
                return false
            }

            // Deduct from inactive investment pocket
            val updatedInactiveDto = DataPocketUpdate(
                id = pocket.id,
                isActive = pocket.isActive,
                name = pocket.name,
                total = (pocket.total - nominal).toInt(),
                userId = userId ?: 0,
                walletType = pocket.walletType
            )
            val resInactive = repository.updatePocket(updatedInactiveDto)
            if (resInactive.isFailure) {
                _messageFlow.emit("Failed to deduct from inactive investment pocket: ${resInactive.exceptionOrNull()?.message}")
                return false
            }
            _pocket.value = resInactive.getOrNull()!!

            // Add to active investment pocket
            val updatedActiveDto = DataPocketUpdate(
                id = activeInvestmentPocket.id,
                isActive = activeInvestmentPocket.isActive,
                name = activeInvestmentPocket.name,
                total = (activeInvestmentPocket.total + nominal).toInt(),
                userId = userId ?: 0,
                walletType = activeInvestmentPocket.walletType
            )
            val resActive = repository.updatePocket(updatedActiveDto)
            if (resActive.isFailure) {
                _messageFlow.emit("Failed to add to active investment pocket: ${resActive.exceptionOrNull()?.message}")
                return false
            }

            // Record Buy transaction (simulate transfer)
            val buyTransaction = AddTransactionRequest(
                action = "Buy",
                name = name,
                nominal = nominal.toInt(),
                pocketId = pocket.id,
                pricePerUnit = pricePerUnit.toInt(),
                toPocketId = activeInvestmentPocket.id,
                unitAmount = unitAmount.toInt()
            )
            val result = repository.addTransaction(buyTransaction)
            result.onFailure { ex ->
                _messageFlow.emit("Failed to buy investment: ${ex.message}")
                return false
            }

            _messageFlow.emit("Investment '$name' purchased successfully!")

            // Reload transactions for this pocket to update the list immediately
            loadTransactions(pocket.id)
            // Also reload active investment pocket transactions
            loadTransactions(activeInvestmentPocket.id)

            return true
        } catch (e: Exception) {
            _messageFlow.emit("Buy investment failed: ${e.message}")
            return false
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Loads transactions for the given pocketId and updates state.
     * Enriches transaction data for UI display, especially for transfer transactions.
     */
    fun loadTransactions(pocketId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val historyResult = repository.getPocketHistory(pocketId)
                val userId = com.mario.tanamin.data.session.InMemorySessionHolder.userId?.toIntOrNull()
                val pocketsResult = if (userId != null) repository.getPocketsByUser(userId) else Result.success(emptyList())
                if (historyResult.isFailure || pocketsResult.isFailure) {
                    _error.value = historyResult.exceptionOrNull()?.message ?: pocketsResult.exceptionOrNull()?.message ?: "Failed to load transactions"
                    _transactions.value = emptyList()
                    _uiTransactions.value = emptyList()
                    _isLoading.value = false
                    return@launch
                }
                val txs = historyResult.getOrNull() ?: emptyList()
                Log.d("PocketDetailViewModel", "Fetched ${txs.size} transactions from pocket history for pocketId=$pocketId")
                val pockets = pocketsResult.getOrNull() ?: emptyList()
                val pocketMap = pockets.associateBy { it.id }

                val uiTxs = txs.map { tx ->
                    when {
                        // When this pocket is the sender, show 'Transfer To'
                        tx.action == "Transfer" && tx.pocketId == pocketId -> {
                            PocketTransactionModel(
                                id = tx.id,
                                action = tx.action,
                                type = "Transfer To",
                                amount = tx.nominal,
                                date = tx.date,
                                otherPocketName = pocketMap[tx.toPocketId]?.name
                            )
                        }
                        // When this pocket is the receiver, show 'Transfer From'
                        tx.action == "Transfer" && tx.toPocketId == pocketId -> {
                            PocketTransactionModel(
                                id = tx.id,
                                action = tx.action,
                                type = "Transfer From",
                                amount = tx.nominal,
                                date = tx.date,
                                otherPocketName = pocketMap[tx.pocketId]?.name
                            )
                        }
                        // Show stock name for Buy/Sell
                        (tx.action == "Buy" || tx.action == "Sell") -> {
                            PocketTransactionModel(
                                id = tx.id,
                                action = tx.action,
                                type = tx.action,
                                amount = tx.nominal,
                                date = tx.date,
                                otherPocketName = tx.name // Show stock name
                            )
                        }
                        else -> {
                            PocketTransactionModel(
                                id = tx.id,
                                action = tx.action,
                                type = tx.action,
                                amount = tx.nominal,
                                date = tx.date,
                                otherPocketName = null
                            )
                        }
                    }
                }
                // Store raw transactions for Active Investments calculation
                _transactions.value = txs.map { tx ->
                    DataTransactionResponse(
                        action = tx.action,
                        date = tx.date,
                        id = tx.id,
                        name = tx.name,
                        nominal = tx.nominal,
                        pocketId = tx.pocketId,
                        pricePerUnit = tx.pricePerUnit,
                        toPocketId = tx.toPocketId,
                        unitAmount = tx.unitAmount
                    )
                }
                _uiTransactions.value = uiTxs
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load transactions"
                _transactions.value = emptyList()
                _uiTransactions.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clear() {
        _pocket.value = null
        _error.value = null
        _isLoading.value = false
        _availableTargets.value = emptyList()
    }
}