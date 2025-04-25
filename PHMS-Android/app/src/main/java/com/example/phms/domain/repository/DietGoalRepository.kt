package com.example.phms.domain.repository

import android.util.Log
import com.example.phms.data.model.DietGoalDTO
import com.example.phms.data.remote.api.RetrofitClient
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