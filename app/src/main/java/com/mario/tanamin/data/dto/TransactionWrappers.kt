package com.mario.tanamin.data.dto

data class TransactionListResponse(
    val data: List<TransactionResponse>
)

data class TransactionSingleResponse(
    val data: TransactionResponse
)
