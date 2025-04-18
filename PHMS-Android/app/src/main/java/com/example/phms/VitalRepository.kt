package com.example.phms

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object VitalRepository {
  private val svc = RetrofitClient.apiService

  suspend fun getVitals(userId: String): List<VitalSign> = withContext(Dispatchers.IO) {
    svc.getVitals(userId).execute().body() ?: emptyList()
  }

  suspend fun addVital(v: VitalSign): VitalSign? = withContext(Dispatchers.IO) {
    svc.addVital(v).execute().body()
  }

  suspend fun updateVital(v: VitalSign): Boolean = withContext(Dispatchers.IO) {
    svc.updateVital(v).execute().isSuccessful
  }

  suspend fun deleteVital(id: Int): Boolean = withContext(Dispatchers.IO) {
    svc.deleteVital(id).execute().isSuccessful
  }
}
