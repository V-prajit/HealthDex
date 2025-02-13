package com.example.phms

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

data class AuthRequest(val token: String)

data class UserDataRequest(
    val firebaseUid: String?,
    val name: String,
    val email: String,
    val age: Int?,
    val height: Double?,
    val weight: Double?
)

interface ApiService {
    @POST("/auth")
    fun authenticateUser(@Body request: AuthRequest): Call<ResponseBody>

    @POST("/users/register")
    fun sendUserData(@Body request: UserDataRequest): Call<ResponseBody>
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
