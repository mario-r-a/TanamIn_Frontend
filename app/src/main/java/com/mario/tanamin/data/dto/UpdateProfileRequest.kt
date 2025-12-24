package com.mario.tanamin.data.dto

data class UpdateProfileRequest(
    val name: String,
    val email: String,
    val password: String? = null
)
