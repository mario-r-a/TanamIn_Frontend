package com.mario.tanamin.data.dto

data class DataPocketHistoryResponse(
    val action: String,
    val date: String,
    val id: Int,
    val name: String,
    val nominal: Int,
    val pocketId: Int,
    val pricePerUnit: Int,
    val toPocketId: Int,
    val unitAmount: Int
)