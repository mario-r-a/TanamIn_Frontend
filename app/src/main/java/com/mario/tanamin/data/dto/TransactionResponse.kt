package com.mario.tanamin.data.dto

data class TransactionResponse(
    val id: Int,
    val date: String,
    val name: String,
    val pricePerUnit: Int,
    val action: String,
    val nominal: Int,
    val unitAmount: Int,
    val pocketId: Int,
    val toPocketId: Int?
)
