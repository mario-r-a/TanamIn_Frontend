package com.mario.tanamin.ui.model

data class PocketTransactionModel(
    val id: Int,
    val action: String,
    val type: String, // Deposit, Transfer From, Transfer To, etc.
    val amount: Int,
    val date: String,
    val otherPocketName: String? // For transfers, the other pocket's name
)
