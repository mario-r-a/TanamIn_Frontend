package com.mario.tanamin.data.repository

import com.mario.tanamin.data.dto.LoginRequest
import com.mario.tanamin.data.dto.LoginResponse
import com.mario.tanamin.data.service.TanamInService
import retrofit2.Response

class TanamInRepository(private val tanamInService: TanamInService) {
    suspend fun login(username: String, password: String): Result<LoginResponse> {
        return try {
            val response: Response<LoginResponse> = tanamInService.login(
                LoginRequest(
                    username,
                    password
                )
            )
            if (!response.isSuccessful) {
                return Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
            val body = response.body()
            if (body == null) {
                return Result.failure(Exception("Empty response body"))
            }
            Result.success(body)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}