package com.mario.tanamin.data.dto

data class DataQuestion(
    val answer: String,
    val id: Int,
    val levelId: Int,
    val option1: String,
    val option2: String,
    val option3: String,
    val option4: String,
    val question: String
)