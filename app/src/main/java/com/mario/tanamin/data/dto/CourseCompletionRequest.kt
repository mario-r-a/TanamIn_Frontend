package com.mario.tanamin.data.dto

data class CourseCompletionRequest(
    val coinDelta: Int,
    val claimStreak: Boolean,
    val timezone: String = "Asia/Jakarta"
)