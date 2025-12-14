package com.mario.tanamin.data.repository

import android.util.Log
import com.mario.tanamin.data.dto.DataPocket
import com.mario.tanamin.data.dto.LoginRequest
import com.mario.tanamin.data.dto.LoginResponse
import com.mario.tanamin.data.dto.PocketResponse
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
            return Result.failure(e)
        }
    }

    suspend fun getPocketsByUser(userId: Int): Result<List<DataPocket>> {
        return try {
            val response: Response<PocketResponse> = tanamInService.getPocketsByUser(userId)
            if (!response.isSuccessful) {
                Log.d("TanamInRepository", "getPocketsByUser HTTP ${response.code()}: ${response.message()}")
                return Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
            val body = response.body()
            if (body == null) {
                Log.d("TanamInRepository", "getPocketsByUser empty body")
                return Result.failure(Exception("Empty response body"))
            }
            val pockets = body.`data`
            Log.d("TanamInRepository", "getPocketsByUser received ${pockets.size} pockets for userId=$userId")
            pockets.forEach { p ->
                Log.d("TanamInRepository", "pocket: id=${p.id} name=${p.name} isActive=${p.isActive} walletType=${p.walletType} total=${p.total}")
            }
            Result.success(pockets)
        } catch (e: Exception) {
            Log.e("TanamInRepository", "getPocketsByUser exception", e)
            return Result.failure(e)
        }
    }
}