package com.example.phms.repository

import android.util.Log
import com.example.phms.Doctor
import com.example.phms.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DoctorRepository {
    private val apiService = RetrofitClient.apiService

    suspend fun getDoctors(userId: String): List<Doctor> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getDoctors(userId).execute()
            response.body() ?: emptyList()
        } catch (e: Exception) {
            Log.e("DoctorRepository", "Error fetching doctors: ${e.message}")
            emptyList()
        }
    }

    suspend fun getDoctor(id: Int): Doctor? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getDoctor(id).execute()
            response.body()
        } catch (e: Exception) {
            Log.e("DoctorRepository", "Error fetching doctor with id $id: ${e.message}")
            null
        }
    }

    suspend fun addDoctor(doctor: Doctor): Doctor? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.addDoctor(doctor).execute()
            response.body()
        } catch (e: Exception) {
            Log.e("DoctorRepository", "Error adding doctor: ${e.message}")
            null
        }
    }

    suspend fun updateDoctor(doctor: Doctor): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateDoctor(doctor).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("DoctorRepository", "Error updating doctor: ${e.message}")
            false
        }
    }

    suspend fun deleteDoctor(id: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteDoctor(id).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("DoctorRepository", "Error deleting doctor: ${e.message}")
            false
        }
    }
}