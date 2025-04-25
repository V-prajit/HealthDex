package com.example.phms.domain.repository

import android.util.Log
import com.example.phms.data.model.Diet
import com.example.phms.data.remote.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime

object DietRepository {
    private val apiService = RetrofitClient.apiService

    suspend fun getDiets(userId: String): List<Diet> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getDiets(userId).execute()
            response.body() ?: emptyList()
        } catch (e: Exception) {
            Log.e("DietRepository", "Error fetching diets", e)
            emptyList()
        }
    }

    suspend fun getDietsForDate(userId: String, date: LocalDate): List<Diet> = withContext(Dispatchers.IO) {
        try {
            val allDiets = getDiets(userId)
            allDiets.filter {
                try {
                    val entryDate = LocalDateTime.parse(it.timestamp).toLocalDate()
                    entryDate == date
                } catch (e: Exception) {
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("DietRepository", "Error filtering diets by date", e)
            emptyList()
        }
    }

    suspend fun addDiet(diet: Diet): Diet? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.addDiet(diet).execute()
            response.body()
        } catch (e: Exception) {
            Log.e("DietRepository", "Error adding diet", e)
            null
        }
    }

    suspend fun updateDiet(diet: Diet): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateDiet(diet).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("DietRepository", "Error updating diet", e)
            false
        }
    }

    suspend fun deleteDiet(id: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteDiet(id).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("DietRepository", "Error deleting diet", e)
            false
        }
    }
}