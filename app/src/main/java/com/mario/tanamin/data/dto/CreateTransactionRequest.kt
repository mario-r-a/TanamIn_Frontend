package com.mario.tanamin.data.dto

data class CreateTransactionRequest(
    val date: String,
    val name: String,
    val pricePerUnit: Int,
    val action: String,
    val nominal: Int,
    val unitAmount: Int,
    val pocketId: Int,
    val toPocketId: Int? = null
)
