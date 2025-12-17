package com.mario.tanamin.data.service

import com.mario.tanamin.data.dto.CourseCompletionRequest
import com.mario.tanamin.data.dto.DataPocketUpdate
import com.mario.tanamin.data.dto.LevelResponse
import com.mario.tanamin.data.dto.LoginRequest
import com.mario.tanamin.data.dto.LoginResponse
import com.mario.tanamin.data.dto.PocketResponse
import com.mario.tanamin.data.dto.ProfileResponse
import com.mario.tanamin.data.dto.UpdatePocketResponse
import com.mario.tanamin.data.dto.QuestionResponse
import com.mario.tanamin.data.dto.UpdateLevelRequest
import com.mario.tanamin.data.dto.UpdateLevelResponse
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

    @GET("api/levels")
    suspend fun getLevelsByUser(): Response<LevelResponse>

    @GET("api/questions/level/{levelId}")
    suspend fun getQuestionsByLevel(
        @Path("levelId") levelId: Int
    ): Response<QuestionResponse>

    @POST("/api/profile/course-complete")
    suspend fun handleCourseCompletion(
        @Body request: CourseCompletionRequest
    ): Response<ProfileResponse>

    @PATCH("/api/levels/{levelId}")
    suspend fun updateLevel(
        @Path("levelId") levelId: Int,
        @Body request: UpdateLevelRequest
    ): Response<UpdateLevelResponse>
}