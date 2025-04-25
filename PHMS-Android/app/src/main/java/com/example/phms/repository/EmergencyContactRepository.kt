package com.example.phms.repository

import android.util.Log
import com.example.phms.EmergencyContact
import com.example.phms.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object EmergencyContactRepository {
    private val apiService = RetrofitClient.apiService

    suspend fun getContacts(userId: String): List<EmergencyContact> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getEmergencyContacts(userId).execute()
            response.body() ?: emptyList()
        } catch (e: Exception) {
            Log.e("EmergencyContactRepository", "Error fetching contacts: ${e.message}")
            emptyList()
        }
    }

    suspend fun addContact(contact: EmergencyContact): EmergencyContact? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.addEmergencyContact(contact).execute()
            response.body()
        } catch (e: Exception) {
            Log.e("EmergencyContactRepository", "Error adding contact: ${e.message}")
            null
        }
    }

    suspend fun updateContact(contact: EmergencyContact): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateEmergencyContact(contact).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("EmergencyContactRepository", "Error updating contact: ${e.message}")
            false
        }
    }

    suspend fun deleteContact(id: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteEmergencyContact(id).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("EmergencyContactRepository", "Error deleting contact: ${e.message}")
            false
        }
    }
}