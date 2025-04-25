package com.example.phms.repository

import android.util.Log
import com.example.phms.Appointment
import com.example.phms.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppointmentRepository {
    private val apiService = RetrofitClient.apiService

    suspend fun getAppointments(userId: String): List<Appointment> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getAppointments(userId).execute()
            response.body() ?: emptyList()
        } catch (e: Exception) {
            Log.e("AppointmentRepository", "Error fetching appointments: ${e.message}")
            emptyList()
        }
    }

    suspend fun getUpcomingAppointments(userId: String): List<Appointment> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getUpcomingAppointments(userId).execute()
            response.body() ?: emptyList()
        } catch (e: Exception) {
            Log.e("AppointmentRepository", "Error fetching upcoming appointments: ${e.message}")
            emptyList()
        }
    }

    suspend fun getAppointment(id: Int): Appointment? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getAppointment(id).execute()
            response.body()
        } catch (e: Exception) {
            Log.e("AppointmentRepository", "Error fetching appointment with id $id: ${e.message}")
            null
        }
    }

    suspend fun addAppointment(appointment: Appointment): Appointment? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.addAppointment(appointment).execute()
            response.body()
        } catch (e: Exception) {
            Log.e("AppointmentRepository", "Error adding appointment: ${e.message}")
            null
        }
    }

    suspend fun updateAppointment(appointment: Appointment): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateAppointment(appointment).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("AppointmentRepository", "Error updating appointment: ${e.message}")
            false
        }
    }

    suspend fun deleteAppointment(id: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteAppointment(id).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("AppointmentRepository", "Error deleting appointment: ${e.message}")
            false
        }
    }
}