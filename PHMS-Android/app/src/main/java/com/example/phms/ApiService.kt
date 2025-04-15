package com.example.phms


import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.GET
import retrofit2.converter.moshi.MoshiConverterFactory

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
}


object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8085/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    /** one Retrofit instance, two converters */
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create(moshi))   // for BackendApi
        .addConverterFactory(GsonConverterFactory.create())         // for ApiService
        .build()

    /** original service used by other screens (unchanged) */
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    /** NEW – service needed by VitalRepository / BackendStuff.kt */
    val api: BackendApi by lazy {
        retrofit.create(BackendApi::class.java)
    }
}