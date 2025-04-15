package com.example.phms

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.http.*
import java.util.UUID

/* ───────── shared DTO ───────── */

data class VitalSignDTO(
    val recordId: String = UUID.randomUUID().toString(),
    val firebaseUid: String = "",
    val signType: String,
    val value: Float,
    val unit: String,
    val epochMillis: Long
)

/* ───────── Retrofit endpoints ───────── */

interface BackendApi {
    @POST("users/{uid}/vitals")
    suspend fun addVital(@Path("uid") uid: String, @Body dto: VitalSignDTO)

    @GET("users/{uid}/vitals")
    suspend fun vitals(@Path("uid") uid: String): List<VitalSignDTO>

    @POST("users/{uid}/token")
    suspend fun registerToken(
        @Path("uid") uid: String,
        @Body body: Map<String, String>
    )
}

/* ───────── repository ───────── */

object VitalRepository {
    private val api = RetrofitClient.api   // ← now resolves

    suspend fun add(uid: String, dto: VitalSignDTO) =
        withContext(Dispatchers.IO) { api.addVital(uid, dto) }

    suspend fun list(uid: String) =
        withContext(Dispatchers.IO) { api.vitals(uid) }

    suspend fun sendFcmToken(uid: String, token: String) =
        withContext(Dispatchers.IO) { api.registerToken(uid, mapOf("token" to token)) }
}