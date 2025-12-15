package com.mario.tanamin.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mario.tanamin.data.container.TanamInContainer
import com.mario.tanamin.data.dto.DataPocketUpdate
import com.mario.tanamin.data.repository.TanamInRepository
import com.mario.tanamin.data.session.InMemorySessionHolder
import com.mario.tanamin.ui.model.PocketModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for Wallets (pockets).
 * - Exposes pockets list, loading state and error message as StateFlows.
 * - Use loadPocketsFor(userId) if you pass the user id explicitly.
 * - Use loadPocketsFromInMemoryUser() to read the user id from InMemorySessionHolder (set on login).
 *
 * This ViewModel exposes PocketModel lists; the view will format amounts for display.
 */
class WalletViewModel(
    private val repository: TanamInRepository = TanamInContainer().tanamInRepository
) : ViewModel() {

    private val _pockets = MutableStateFlow<List<PocketModel>>(emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Profile budgeting percentage from backend
    private val _budgetingPercentage = MutableStateFlow<Int?>(null)
    val budgetingPercentage: StateFlow<Int?> = _budgetingPercentage.asStateFlow()

    // Message flow for one-shot messages (success/error)
    private val _messageFlow = MutableSharedFlow<String>()
    val messageFlow: SharedFlow<String> = _messageFlow.asSharedFlow()

    // UI screen selection for the Wallet view
    enum class WalletScreen { Main, Investment }

    private val _selectedScreen = MutableStateFlow(WalletScreen.Main)
    val selectedScreen: StateFlow<WalletScreen> = _selectedScreen.asStateFlow()

    fun setSelectedScreen(screen: WalletScreen) {
        _selectedScreen.value = screen
    }

    // Derived reactive total: sums active Main pockets; UI can collect this to guarantee updates.
    val mainTotalFlow: StateFlow<Long> = _pockets
        .map { list ->
            list.filter { p ->
                val wt = p.walletType.trim()
                p.isActive && wt.equals("Main", ignoreCase = true)
            }.sumOf { it.total }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    // Derived StateFlow containing only active pockets with walletType == "Main"
    val activeMainPockets: StateFlow<List<PocketModel>> = _pockets
        .map { list ->
            list.filter { p ->
                val wt = p.walletType.trim()
                p.isActive && wt.equals("Main", ignoreCase = true)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Derived StateFlow containing pockets with walletType == "Investment" (case-insensitive).
    val investmentPockets: StateFlow<List<PocketModel>> = _pockets
        .map { list ->
            list.filter { p ->
                val wt = p.walletType.trim()
                wt.equals("Investment", ignoreCase = true)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Investment total as Long
    val investmentTotalFlow: StateFlow<Long> = _pockets
        .map { list ->
            list.filter { p ->
                p.walletType.trim().equals("Investment", ignoreCase = true)
            }.sumOf { it.total }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    init {
        // listen for repository updates and apply incremental changes
        viewModelScope.launch {
            repository.pocketsUpdated.collect { updatedPocket ->
                // apply the updated pocket to the current list if present
                val current = _pockets.value
                val replaced = current.map { if (it.id == updatedPocket.id) updatedPocket else it }
                _pockets.value = replaced
            }
        }

        // Load pockets when ViewModel created
        loadPocketsFromInMemoryUser()
    }

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
                            val wt = p.walletType.trim()
                            p.isActive && wt.equals("Main", ignoreCase = true)
                        }.sumOf { it.total }
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

    /** Load user profile to get budgeting percentage */
    fun loadProfile() {
        viewModelScope.launch {
            try {
                repository.getProfile()
                    .onSuccess { profile ->
                        _budgetingPercentage.value = profile.budgetingPercentage
                        Log.d("WalletViewModel", "Loaded profile budgetingPercentage=${profile.budgetingPercentage}")
                    }
                    .onFailure { ex ->
                        Log.e("WalletViewModel", "Failed to load profile: ${ex.message}")
                        _messageFlow.emit("Failed to load budgeting percentage")
                    }
            } catch (e: Exception) {
                Log.e("WalletViewModel", "Exception loading profile", e)
                _messageFlow.emit("Failed to load profile")
            }
        }
    }

    /**
     * Add balance with budgeting split:
     * - budgetPercentage% goes to first active Main pocket
     * - (100-budgetPercentage)% goes to Inactive Investments pocket
     */
    suspend fun addBalance(amount: Long, budgetPercentage: Int): Boolean {
        if (amount <= 0) {
            _messageFlow.emit("Amount must be greater than zero")
            return false
        }
        if (budgetPercentage < 0 || budgetPercentage > 100) {
            _messageFlow.emit("Budget percentage must be between 0 and 100")
            return false
        }

        _isLoading.value = true
        try {
            val userId = InMemorySessionHolder.userId?.toIntOrNull()
            if (userId == null) {
                _messageFlow.emit("User not logged in")
                return false
            }

            val currentPockets = _pockets.value

            // Find first active Main pocket
            val mainPocket = currentPockets.firstOrNull { p ->
                p.isActive && p.walletType.trim().equals("Main", ignoreCase = true)
            }

            // Find Inactive Investments pocket
            val inactiveInvestmentsPocket = currentPockets.firstOrNull { p ->
                p.walletType.trim().equals("Investment", ignoreCase = true) &&
                p.name.trim().equals("Inactive Investments", ignoreCase = true)
            }

            if (mainPocket == null) {
                _messageFlow.emit("No active Main pocket found")
                return false
            }

            if (inactiveInvestmentsPocket == null) {
                _messageFlow.emit("Inactive Investments pocket not found")
                return false
            }

            // Calculate split
            val toMain = (amount * budgetPercentage / 100)
            val toInvestment = amount - toMain

            Log.d("WalletViewModel", "Adding balance: total=$amount, toMain=$toMain ($budgetPercentage%), toInvestment=$toInvestment")

            // Update main pocket
            val updatedMainDto = DataPocketUpdate(
                id = mainPocket.id,
                isActive = mainPocket.isActive,
                name = mainPocket.name,
                total = (mainPocket.total + toMain).toInt(),
                userId = userId,
                walletType = mainPocket.walletType
            )

            val mainResult = repository.updatePocket(updatedMainDto)
            mainResult.onFailure { ex ->
                _messageFlow.emit("Failed to update Main pocket: ${ex.message}")
                return false
            }

            // Update inactive investments pocket
            val updatedInvestmentDto = DataPocketUpdate(
                id = inactiveInvestmentsPocket.id,
                isActive = inactiveInvestmentsPocket.isActive,
                name = inactiveInvestmentsPocket.name,
                total = (inactiveInvestmentsPocket.total + toInvestment).toInt(),
                userId = userId,
                walletType = inactiveInvestmentsPocket.walletType
            )

            val investmentResult = repository.updatePocket(updatedInvestmentDto)
            investmentResult.onFailure { ex ->
                _messageFlow.emit("Failed to update Inactive Investments pocket: ${ex.message}")
                return false
            }

            // Reload pockets to reflect changes
            loadPocketsFor(userId)

            _messageFlow.emit("Balance added successfully!")
            return true
        } catch (e: Exception) {
            Log.e("WalletViewModel", "Exception adding balance", e)
            _messageFlow.emit("Failed to add balance: ${e.message}")
            return false
        } finally {
            _isLoading.value = false
        }
    }
}
