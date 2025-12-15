package com.mario.tanamin.data.repository

import android.util.Log
import com.mario.tanamin.data.dto.DataPocket
import com.mario.tanamin.data.dto.LoginRequest
import com.mario.tanamin.data.dto.LoginResponse
import com.mario.tanamin.data.dto.PocketResponse
import com.mario.tanamin.data.service.TanamInService
import com.mario.tanamin.ui.model.PocketModel
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

    suspend fun getPocketsByUser(userId: Int): Result<List<PocketModel>> {
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
            val pocketsDto: List<DataPocket> = body.`data`
            Log.d("TanamInRepository", "getPocketsByUser received ${pocketsDto.size} pockets for userId=$userId")
            pocketsDto.forEach { p ->
                Log.d("TanamInRepository", "pocket dto: id=${p.id} name=${p.name} isActive=${p.isActive} walletType=${p.walletType} total=${p.total}")
            }

            // Map DTO -> UI model
            val pocketModels = pocketsDto.map { dto ->
                PocketModel(
                    id = dto.id,
                    name = dto.name,
                    total = dto.total.toLong(),
                    isActive = dto.isActive,
                    walletType = dto.walletType
                )
            }

            pocketModels.forEach { p ->
                Log.d("TanamInRepository", "pocket model: id=${p.id} name=${p.name} isActive=${p.isActive} walletType=${p.walletType} total=${p.total}")
            }

            Result.success(pocketModels)
        } catch (e: Exception) {
            Log.e("TanamInRepository", "getPocketsByUser exception", e)
            return Result.failure(e)
        }
    }

    suspend fun getProfile(): Result<com.mario.tanamin.data.dto.DataProfile> {
        return try {
            val response: Response<com.mario.tanamin.data.dto.ProfileResponse> = tanamInService.getProfile()
            if (!response.isSuccessful) {
                return Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
            val body = response.body()
            if (body == null) {
                return Result.failure(Exception("Empty response body"))
            }
            Result.success(body.`data`)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}