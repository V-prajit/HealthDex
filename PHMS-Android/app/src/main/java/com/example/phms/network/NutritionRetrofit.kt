package com.example.phms.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object NutritionRetrofit {
    val service: NutritionApiService by lazy {
        val logger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        Retrofit.Builder()
            .baseUrl("https://api.nal.usda.gov/fdc/")
            .client(OkHttpClient.Builder().addInterceptor(logger).build())
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(NutritionApiService::class.java)
    }
}
