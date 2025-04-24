package com.example.phms

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DietGoalRepository {
    private val apiService = RetrofitClient.apiService

    suspend fun getDietGoals(userId: String): DietGoalDTO? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getDietGoals(userId).execute()
            response.body()
        } catch (e: Exception) {
            Log.e("DietGoalRepository", "Error fetching diet goals", e)
            null
        }
    }

    suspend fun setDietGoals(goals: DietGoalDTO): DietGoalDTO? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.setDietGoals(goals).execute()
            response.body()
        } catch (e: Exception) {
            Log.e("DietGoalRepository", "Error setting diet goals", e)
            null
        }
    }
}