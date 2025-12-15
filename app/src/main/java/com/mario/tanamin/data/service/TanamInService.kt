package com.mario.tanamin.data.service

import com.mario.tanamin.data.dto.DataPocketUpdate
import com.mario.tanamin.data.dto.LoginRequest
import com.mario.tanamin.data.dto.LoginResponse
import com.mario.tanamin.data.dto.PocketResponse
import com.mario.tanamin.data.dto.ProfileResponse
import com.mario.tanamin.data.dto.UpdatePocketResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
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

    @PATCH("api/pockets/{pocketId}")
    suspend fun updatePocket(
        @Path("pocketId") pocketId: Int,
        @Body update: DataPocketUpdate
    ): Response<UpdatePocketResponse>
    
    @GET("api/profile")
    suspend fun getProfile(): Response<ProfileResponse>

    @POST("api/transactions")
    suspend fun createTransaction(
        @Body request: com.mario.tanamin.data.dto.CreateTransactionRequest
    ): Response<com.mario.tanamin.data.dto.TransactionSingleResponse>

    @GET("api/transactions/pocket/{pocketId}")
    suspend fun getTransactionsByPocket(
        @Path("pocketId") pocketId: Int
    ): Response<com.mario.tanamin.data.dto.TransactionListResponse>

    @POST("api/pockets")
    suspend fun createPocket(
        @Body request: com.mario.tanamin.data.dto.CreatePocketRequest
    ): Response<com.mario.tanamin.data.dto.CreatePocketResponse>
}