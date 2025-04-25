package com.example.phms.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import com.example.phms.BuildConfig
import com.example.phms.SearchResponse
import com.example.phms.FoodItem

interface NutritionApiService {

    @GET("v1/foods/search")
    suspend fun searchFoods(
        @Query("query")   query: String,
        @Query("pageSize") pageSize: Int = 10,
        @Query("api_key") apiKey: String = BuildConfig.FDC_API_KEY
    ): SearchResponse

    @GET("v1/food/{fdcId}")
    suspend fun getFood(
        @Path("fdcId") id: Long,
        @Query("api_key") apiKey: String = BuildConfig.FDC_API_KEY
    ): FoodItem
}
