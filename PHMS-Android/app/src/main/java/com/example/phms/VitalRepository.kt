package com.example.phms

import android.util.Log
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

  suspend fun getLatestVital(userId: String, type: String): VitalSign? =
  withContext(Dispatchers.IO) {
    try {
      val dto = RetrofitClient.apiService
        .getLatestVital(userId, type)
        .execute()
        .body()
      dto?.let { VitalSign(it.id, it.userId, it.type, it.value, it.unit, it.timestamp) }
    } catch (t: Throwable) {
      Log.e("VitalRepo", "getLatest failed", t)
      null
    }
  }

}
