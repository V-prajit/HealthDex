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
      dto?.let {
        VitalSign(
            id              = it.id,
            userId          = it.userId,
            type            = it.type,
            value           = it.value,
            unit            = it.unit,
            timestamp       = it.timestamp,
            manualSystolic  = it.manualSystolic,
            manualDiastolic = it.manualDiastolic
        )
    }
    } catch (t: Throwable) {
      Log.e("VitalRepo", "getLatest failed", t)
      null
    }
  }

}
