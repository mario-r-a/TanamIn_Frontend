package com.mario.tanamin.data.dto

data class AddTransactionRequest(
    val action: String,
    val name: String,
    val nominal: Int,
    val pocketId: Int,
    val pricePerUnit: Int,
    val toPocketId: Int?,
    val unitAmount: Int
)