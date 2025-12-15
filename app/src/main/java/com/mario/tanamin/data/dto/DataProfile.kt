package com.mario.tanamin.data.dto

data class DataProfile(
    val id: Int,
    val name: String,
    val username: String,
    val email: String,
    val coin: Int,
    val streak: Int,
    val highestStreak: Int,
    val lastStreakDate: String?,
    val budgetingPercentage: Int,
    val activeThemeId: Int
)
