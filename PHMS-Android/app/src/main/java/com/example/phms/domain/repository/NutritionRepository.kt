package com.example.phms.domain.repository

import android.content.Context
import com.example.phms.model.nutrition.*
import com.example.phms.data.remote.client.NutritionRetrofit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object NutritionRepository {

    lateinit var appContext: Context
        private set

    fun init(context: Context) { appContext = context.applicationContext }

    private val service get() = NutritionRetrofit.service

    suspend fun searchFoodsByName(name: String): List<FoodHit> = withContext(Dispatchers.IO) {
        val key = name.lowercase(Locale.US)
        val hits = runCatching {
            service.searchFoods(key).foods
        }.getOrNull() ?: emptyList()

        return@withContext hits
    }

    suspend fun getFoodDetails(fdcId: Long): NutritionInfo? = withContext(Dispatchers.IO) {
        val info = runCatching {
            service.getFood(fdcId).toNutritionInfo()
        }.getOrNull()

        return@withContext info
    }
}
