package com.mario.tanamin.data.container

import com.google.gson.GsonBuilder
import com.mario.tanamin.data.repository.TanamInRepository
import com.mario.tanamin.data.service.TanamInService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class TanamInContainer {
    companion object {
        val BASE_URL = "http://10.0.2.2:3000/"
    }

    private val retrofit = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
        .baseUrl(BASE_URL)
        .build()

    private val tanamInService: TanamInService by lazy {
        retrofit.create(TanamInService::class.java)
    }

    val tanamInRepository: TanamInRepository by lazy {
        TanamInRepository(tanamInService)
    }
}