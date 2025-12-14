package com.mario.tanamin.ui.model

data class PocketModel(
    val id: Int,
    val name: String,
    val total: Long,
    val isActive: Boolean,
    val walletType: String
)