package com.example.phms

import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object MedicationRepository {
    private val api = RetrofitClient.apiService

    fun fetchAll(userId: String, onResult: (List<Medication>?) -> Unit) {
        api.getMedications(userId).enqueue(object : Callback<List<Medication>> {
            override fun onResponse(call: Call<List<Medication>>, response: Response<List<Medication>>) {
                onResult(response.body())
            }

            override fun onFailure(call: Call<List<Medication>>, t: Throwable) {
                Log.e("MedicationRepo", "Error fetching medications", t)
                onResult(null)
            }
        })
    }

    fun add(entry: Medication, onResult: (Medication?) -> Unit) {
        api.addMedication(entry).enqueue(object : Callback<Medication> {
            override fun onResponse(call: Call<Medication>, response: Response<Medication>) {
                onResult(response.body())
            }

            override fun onFailure(call: Call<Medication>, t: Throwable) {
                Log.e("MedicationRepo", "Error adding medication", t)
                onResult(null)
            }
        })
    }

    fun update(entry: Medication, onResult: (Boolean) -> Unit) {
        api.updateMedication(entry).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                onResult(response.isSuccessful)
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("MedicationRepo", "Error updating medication", t)
                onResult(false)
            }
        })
    }

    fun delete(id: Int, onResult: (Boolean) -> Unit) {
        api.deleteMedication(id).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                onResult(response.isSuccessful)
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("MedicationRepo", "Error deleting medication", t)
                onResult(false)
            }
        })
    }
}
