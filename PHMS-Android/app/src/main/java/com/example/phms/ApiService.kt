package com.example.phms

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

data class AuthRequest(val token: String)

data class UserDataRequest(
    val firebaseUid: String?,
    val firstName: String,
    val lastName: String,
    val email: String,
    val age: Int?,
    val height: Double?,
    val weight: Double?,
    val biometricEnabled: Boolean,
    val securityQuestionId: Int,
    val securityAnswer: String
)

data class VerificationResponse(
    val verified: Boolean
)

interface ApiService {
    @POST("/auth")
    fun authenticateUser(@Body request: AuthRequest): Call<ResponseBody>

    @POST("/users/register")
    fun sendUserData(@Body request: UserDataRequest): Call<ResponseBody>

    @GET("/users/{firebaseUid}")
    fun getUser(@Path("firebaseUid") firebaseUid: String): Call<UserDTO>

    // Vitals
    @GET("/vitals")
    fun getVitals(@Query("userId") userId: String): Call<List<VitalSign>>

    @GET("/vitals/latest")
    fun getLatestVital(
        @Query("userId") userId: String,
        @Query("type") type: String
    ): Call<VitalSign>

    @POST("/vitals")
    fun addVital(@Body vital: VitalSign): Call<VitalSign>

    @PUT("/vitals")
    fun updateVital(@Body vital: VitalSign): Call<ResponseBody>

    @DELETE("/vitals/{id}")
    fun deleteVital(@Path("id") id: Int): Call<ResponseBody>

    @GET("/doctors")
    fun getDoctors(@Query("userId") userId: String): Call<List<Doctor>>

    @GET("/doctors/{id}")
    fun getDoctor(@Path("id") id: Int): Call<Doctor>

    @POST("/doctors")
    fun addDoctor(@Body doctor: Doctor): Call<Doctor>

    @PUT("/doctors")
    fun updateDoctor(@Body doctor: Doctor): Call<Doctor>

    @DELETE("/doctors/{id}")
    fun deleteDoctor(@Path("id") id: Int): Call<ResponseBody>

    @GET("/appointments")
    fun getAppointments(@Query("userId") userId: String): Call<List<Appointment>>

    @GET("/appointments/upcoming")
    fun getUpcomingAppointments(@Query("userId") userId: String): Call<List<Appointment>>

    @GET("/appointments/{id}")
    fun getAppointment(@Path("id") id: Int): Call<Appointment>

    @POST("/appointments")
    fun addAppointment(@Body appointment: Appointment): Call<Appointment>

    @PUT("/appointments")
    fun updateAppointment(@Body appointment: Appointment): Call<Appointment>

    @DELETE("/appointments/{id}")
    fun deleteAppointment(@Path("id") id: Int): Call<ResponseBody>

    @GET("/emergency-contacts")
    fun getEmergencyContacts(@Query("userId") userId: String): Call<List<EmergencyContact>>

    @POST("/emergency-contacts")
    fun addEmergencyContact(@Body contact: EmergencyContact): Call<EmergencyContact>

    @PUT("/emergency-contacts")
    fun updateEmergencyContact(@Body contact: EmergencyContact): Call<EmergencyContact>

    @DELETE("/emergency-contacts/{id}")
    fun deleteEmergencyContact(@Path("id") id: Int): Call<ResponseBody>

    @POST("/send-vital-alert")
    suspend fun sendVitalAlert(@Body alertRequest: VitalAlertRequest): retrofit2.Response<Map<String, Int>>

    @GET("/users/email/{email}")
    fun findUserByEmail(@Path("email") email: String): Call<UserDTO>

    @POST("/users/verify-security-question")
    fun verifySecurityAnswer(
        @Query("userId") userId: String,
        @Query("questionId") questionId: Int,
        @Query("answer") answer: String
    ): Call<VerificationResponse>

    @GET("/medications")
    fun getMedications(@Query("userId") userId: String): Call<List<Medication>>

    @POST("/medications")
    fun addMedication(@Body med: Medication): Call<Medication>

    @PUT("/medications")
    fun updateMedication(@Body med: Medication): Call<Void>

    @DELETE("/medications/{id}")
    fun deleteMedication(@Path("id") id: Int): Call<Void>

    @GET("/diets")
    fun getDiets(@Query("userId") userId: String): Call<List<Diet>>

    @POST("/diets")
    fun addDiet(@Body diet: Diet): Call<Diet>

    @PUT("/diets")
    fun updateDiet(@Body diet: Diet): Call<Diet>

    @DELETE("/diets/{id}")
    fun deleteDiet(@Path("id") id: Int): Call<ResponseBody>

    @GET("/diets/goals")
    fun getDietGoals(@Query("userId") userId: String): Call<DietGoalDTO>

    @POST("/diets/goals")
    fun setDietGoals(@Body goals: DietGoalDTO): Call<DietGoalDTO>
}

object RetrofitClient {
    private const val BASE_URL = "http://100.83.200.110:8085"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
