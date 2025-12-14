package com.mario.tanamin.data.service

import com.mario.tanamin.data.dto.LoginRequest
import com.mario.tanamin.data.dto.LoginResponse
import com.mario.tanamin.data.dto.PocketResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface TanamInService {
    @POST("/api/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @GET("api/pockets/user/{userId}")
    suspend fun getPocketsByUser(
        @Path("userId") userId: Int
    ): Response<PocketResponse>
}