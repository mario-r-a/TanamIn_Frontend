package com.mario.tanamin.data.dto

data class ThemeResponse(
    val id: Int,
    val price: Int,
    val primary: String,
    val subprimary: String,
    val secondary: String,
    val subsecondary: String,
    val background: String,
    val subbackground: String,
    val text: String,
    val subtext: String,
    val pie1: String,
    val pie2: String,
    val unlocked: Boolean,
    val userId: Int?
)
