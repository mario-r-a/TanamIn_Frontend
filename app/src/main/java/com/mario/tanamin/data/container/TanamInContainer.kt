package com.mario.tanamin.data.container

import com.google.gson.GsonBuilder
import com.mario.tanamin.data.repository.TanamInRepository
import com.mario.tanamin.data.service.TanamInService
import com.mario.tanamin.data.session.InMemorySessionHolder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class TanamInContainer {
    companion object {
        val BASE_URL = "http://10.0.2.2:3000/"

        // Interceptor that attaches Authorization header if token exists in InMemorySessionHolder
        private val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val token = InMemorySessionHolder.token
            val requestBuilder = original.newBuilder()
            if (!token.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
            val request = requestBuilder.build()
            chain.proceed(request)
        }

        private val okHttpClient by lazy {
            OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
        }

        private val retrofit by lazy {
            Retrofit.Builder()
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
                .baseUrl(BASE_URL)
                .build()
        }

        private val tanamInService: TanamInService by lazy {
            retrofit.create(TanamInService::class.java)
        }

        val tanamInRepository: TanamInRepository by lazy {
            TanamInRepository(tanamInService)
        }
    }

    // Keep instance accessor for existing call sites (TanamInContainer().tanamInRepository)
    val tanamInRepository: TanamInRepository
        get() = Companion.tanamInRepository
}