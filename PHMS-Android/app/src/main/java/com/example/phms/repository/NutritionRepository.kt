package com.example.phms.repository

import android.content.Context
import com.example.phms.data.local.FoodEntity
import com.example.phms.data.local.FoodDatabase
import com.example.phms.model.nutrition.NutritionInfo
import com.example.phms.model.nutrition.toNutritionInfo
import com.example.phms.network.NutritionRetrofit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object NutritionRepository {

    lateinit var appContext: Context
        private set

    fun init(context: Context) { appContext = context.applicationContext }

    private val dao by lazy { FoodDatabase.get(appContext).foodDao() }
    private val service get() = NutritionRetrofit.service

    suspend fun getNutrition(name: String): NutritionInfo? = withContext(Dispatchers.IO) {
        val key = name.lowercase(Locale.US)

        dao.getByName(key)?.toDomain()?.let { return@withContext it }

        val hit = runCatching { service.searchFoods(key).foods.firstOrNull() }.getOrNull()
            ?: return@withContext null

        val info = runCatching { service.getFood(hit.fdcId).toNutritionInfo() }.getOrNull()
            ?: return@withContext null

        dao.insert(FoodEntity(key, info.calories, info.protein, info.fat, info.carbs))
        info
    }
}
