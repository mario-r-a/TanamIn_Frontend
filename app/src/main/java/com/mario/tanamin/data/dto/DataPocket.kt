package com.mario.tanamin.data.dto

data class DataPocket(
    val id: Int,
    val isActive: Boolean,
    val name: String,
    val total: Int,
    val userId: Int,
    val walletType: String
)