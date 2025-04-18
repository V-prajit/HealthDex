package com.example.phms

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

data class AuthRequest(val token: String)

data class UserDataRequest(
    val firebaseUid: String?,
    val firstName: String,
    val lastName: String,
    val email: String,
    val age: Int?,
    val height: Double?,
    val weight: Double?,
    val biometricEnabled: Boolean
)

interface ApiService {
    @POST("/auth")
    fun authenticateUser(@Body request: AuthRequest): Call<ResponseBody>

    @POST("/users/register")
    fun sendUserData(@Body request: UserDataRequest): Call<ResponseBody>

    @GET("/users/{firebaseUid}")
    fun getUser(@Path("firebaseUid") firebaseUid: String): Call<UserDTO>

    @GET("/vitals")
    fun getVitals(@Query("userId") userId: String): Call<List<VitalSign>>

    @GET("/vitals/latest")
    fun getLatestVital(
    @Query("userId") userId: String,
    @Query("type")   type: String
    ): Call<VitalSign>

    @POST("/vitals")
    fun addVital(@Body vital: VitalSign): Call<VitalSign>

    @PUT("/vitals")
    fun updateVital(@Body vital: VitalSign): Call<ResponseBody>

    @DELETE("/vitals/{id}")
    fun deleteVital(@Path("id") id: Int): Call<ResponseBody>
}


object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8085/"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
