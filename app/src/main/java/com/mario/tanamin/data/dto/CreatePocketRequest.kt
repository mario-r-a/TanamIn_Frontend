package com.mario.tanamin.data.dto

data class CreatePocketRequest(
    val name: String,
    val walletType: String,
    val userId: Int,
    val total: Int = 0,
    val isActive: Boolean = true
)
