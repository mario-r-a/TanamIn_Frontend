package com.mario.tanamin.data.repository

import android.util.Log
import com.mario.tanamin.data.dto.CourseCompletionRequest
import com.mario.tanamin.data.dto.DataLevel
import com.mario.tanamin.data.dto.DataPocket
import com.mario.tanamin.data.dto.DataPocketUpdate
import com.mario.tanamin.data.dto.DataQuestion
import com.mario.tanamin.data.dto.LevelResponse
import com.mario.tanamin.data.dto.LoginRequest
import com.mario.tanamin.data.dto.LoginResponse
import com.mario.tanamin.data.dto.PocketResponse
import com.mario.tanamin.data.dto.ThemeListResponse
import com.mario.tanamin.data.dto.SingleThemeResponse
import com.mario.tanamin.data.dto.PurchaseThemeRequest
import com.mario.tanamin.data.dto.QuestionResponse
import com.mario.tanamin.data.dto.SetActiveThemeRequest
import com.mario.tanamin.data.dto.UpdateLevelRequest
import com.mario.tanamin.data.dto.UpdateLevelResponse
import com.mario.tanamin.data.dto.UpdatePocketResponse
import com.mario.tanamin.data.service.TanamInService
import com.mario.tanamin.ui.model.PocketModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import retrofit2.Response
import com.mario.tanamin.data.dto.TransactionsResponse
import com.mario.tanamin.data.dto.DataTransactionResponse
import com.mario.tanamin.data.dto.AddTransactionRequest
import com.mario.tanamin.data.dto.DataPocketHistoryResponse

class TanamInRepository(private val tanamInService: TanamInService) {

    // Flow to notify subscribers that a pocket has been updated; emits the updated PocketModel
    private val _pocketsUpdated = MutableSharedFlow<PocketModel>(extraBufferCapacity = 4)
    val pocketsUpdated: SharedFlow<PocketModel> = _pocketsUpdated

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

    // New: updatePocket uses PATCH api/pockets/{pocketId} to update a pocket and returns the updated PocketModel
    suspend fun updatePocket(update: DataPocketUpdate): Result<PocketModel> {
        return try {
            val response: Response<UpdatePocketResponse> = tanamInService.updatePocket(update.id, update)
            if (!response.isSuccessful) {
                Log.d("TanamInRepository", "updatePocket HTTP ${response.code()}: ${response.message()}")
                return Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
            val body = response.body()
            if (body == null) {
                Log.d("TanamInRepository", "updatePocket empty body")
                return Result.failure(Exception("Empty response body"))
            }

            val dto = body.`data`
            Log.d("TanamInRepository", "updatePocket returned dto: id=${dto.id} name=${dto.name} total=${dto.total} isActive=${dto.isActive} walletType=${dto.walletType}")

            val pocketModel = PocketModel(
                id = dto.id,
                name = dto.name,
                total = dto.total.toLong(),
                isActive = dto.isActive,
                walletType = dto.walletType
            )

            // notify that a pocket changed
            _pocketsUpdated.tryEmit(pocketModel)

            Result.success(pocketModel)
        } catch (e: Exception) {
            Log.e("TanamInRepository", "updatePocket exception", e)
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

    suspend fun getLevelsByUserDto(): Result<List<DataLevel>> {
        return try {
            val response: Response<LevelResponse> = tanamInService.getLevelsByUser()
            if (!response.isSuccessful) {
                Log.d("TanamInRepository", "getLevelsByUserDto HTTP ${response.code()}: ${response.message()}")
                return Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
            val body = response.body()
            if (body == null) {
                Log.d("TanamInRepository", "getLevelsByUserDto empty body")
                return Result.failure(Exception("Empty response body"))
            }
            val levels = body.`data`
            Log.d("TanamInRepository", "getLevelsByUserDto received ${levels.size} levels")
            Result.success(levels)
        } catch (e: Exception) {
            Log.e("TanamInRepository", "getLevelsByUserDto exception", e)
            Result.failure(e)
        }
    }

    suspend fun getQuestionsByLevel(levelId: Int): Result<List<DataQuestion>> {
        return try {
            val response: Response<QuestionResponse> = tanamInService.getQuestionsByLevel(levelId)
            if (!response.isSuccessful) {
                Log.d("TanamInRepository", "getQuestionsByLevel HTTP ${response.code()}: ${response.message()}")
                return Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
            val body = response.body()
            if (body == null) {
                Log.d("TanamInRepository", "getQuestionsByLevel empty body")
                return Result.failure(Exception("Empty response body"))
            }
            val questions = body.`data`
            Log.d("TanamInRepository", "getQuestionsByLevel received ${questions.size} questions for levelId=${levelId}")
            Result.success(questions)
        } catch (e: Exception) {
            Log.e("TanamInRepository", "getQuestionsByLevel exception", e)
            Result.failure(e)
        }
    }

    suspend fun handleCourseCompletion(coinDelta: Int, claimStreak: Boolean): Result<com.mario.tanamin.data.dto.DataProfile> {
        return try {
            val request = CourseCompletionRequest(coinDelta, claimStreak)
            Log.d("TanamInRepository", "handleCourseCompletion: coinDelta=$coinDelta, claimStreak=$claimStreak")
            val response = tanamInService.handleCourseCompletion(request)

            if (!response.isSuccessful) {
                Log.e("TanamInRepository", "handleCourseCompletion HTTP ${response.code()}: ${response.message()}")
                return Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
            val body = response.body()
            if (body == null) {
                Log.e("TanamInRepository", "handleCourseCompletion: empty body")
                return Result.failure(Exception("Empty response body"))
            }
            Log.d("TanamInRepository", "handleCourseCompletion success: coin=${body.data.coin}, streak=${body.data.streak}")
            Result.success(body.data)
        } catch (e: Exception) {
            Log.e("TanamInRepository", "handleCourseCompletion exception", e)
            Result.failure(e)
        }
    }

    suspend fun updateLevel(levelId: Int, isCompleted: Boolean): Result<DataLevel> {
        return try {
            val request = UpdateLevelRequest(isCompleted)
            Log.d("TanamInRepository", "updateLevel: levelId=$levelId, isCompleted=$isCompleted")
            val response: Response<UpdateLevelResponse> = tanamInService.updateLevel(levelId, request)

            if (!response.isSuccessful) {
                Log.e("TanamInRepository", "updateLevel HTTP ${response.code()}: ${response.message()}")
                return Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }

            val body = response.body()
            if (body == null) {
                Log.e("TanamInRepository", "updateLevel: empty body")
                return Result.failure(Exception("Empty response body"))
            }

            // UpdateLevelResponse.data is a single DataLevel object
            val updatedLevel = body.`data`
            Log.d("TanamInRepository", "updateLevel success: level ${updatedLevel.id}, isCompleted=${updatedLevel.isCompleted}")
            Result.success(updatedLevel)
        } catch (e: Exception) {
            Log.e("TanamInRepository", "updateLevel exception", e)
            Result.failure(e)
        }
    }

    suspend fun getTransactionsByPocket(pocketId: Int): Result<List<DataTransactionResponse>> {
        return try {
            val response = tanamInService.getTransactionsByPocket(pocketId)
            if (!response.isSuccessful) {
                Log.d("TanamInRepository", "getTransactionsByPocket HTTP "+response.code()+": "+response.message())
                return Result.failure(Exception("HTTP "+response.code()+": "+response.message()))
            }
            val body = response.body()
            if (body == null) {
                Log.d("TanamInRepository", "getTransactionsByPocket empty body")
                return Result.failure(Exception("Empty response body"))
            }
            Result.success(body.data)
        } catch (e: Exception) {
            Log.e("TanamInRepository", "getTransactionsByPocket exception", e)
            return Result.failure(e)
        }
    }

    // Theme Shop Methods
    suspend fun getThemes(): kotlinx.coroutines.flow.Flow<Result<ThemeListResponse>> = kotlinx.coroutines.flow.flow {
        try {
            val response = tanamInService.getThemes()
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun purchaseTheme(themeId: Int): kotlinx.coroutines.flow.Flow<Result<SingleThemeResponse>> = kotlinx.coroutines.flow.flow {
        try {
            val response = tanamInService.purchaseTheme(PurchaseThemeRequest(themeId))
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun activateTheme(themeId: Int): kotlinx.coroutines.flow.Flow<Result<SingleThemeResponse>> = kotlinx.coroutines.flow.flow {
        try {
            val response = tanamInService.activateTheme(SetActiveThemeRequest(themeId))
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun getActiveTheme(): kotlinx.coroutines.flow.Flow<Result<SingleThemeResponse>> = kotlinx.coroutines.flow.flow {
        try {
            val response = tanamInService.getActiveTheme()
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!))
            } else {
                emit(Result.failure(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun updateProfile(name: String, email: String, password: String?): Result<com.mario.tanamin.data.dto.DataProfile> {
        return try {
            val request = com.mario.tanamin.data.dto.UpdateProfileRequest(name, email, password)
            val response = tanamInService.updateProfile(request)
            if (!response.isSuccessful) {
                return Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
            val body = response.body()
            if (body == null) {
                return Result.failure(Exception("Empty response body"))
            }
            Result.success(body.`data`)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addTransaction(request: AddTransactionRequest): Result<DataTransactionResponse> {
        return try {
            val response = tanamInService.addTransaction(request)
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

    suspend fun getPocketHistory(pocketId: Int): Result<List<DataPocketHistoryResponse>> {
        return try {
            val response = tanamInService.getPocketHistory(pocketId)
            if (!response.isSuccessful) {
                return Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
            val body = response.body()
            if (body == null) {
                return Result.failure(Exception("Empty response body"))
            }
            Result.success(body.data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}