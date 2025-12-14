package com.mario.tanamin.data.service

import com.mario.tanamin.data.dto.LoginRequest
import com.mario.tanamin.data.dto.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface TanamInService {
    @POST("/api/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>
}