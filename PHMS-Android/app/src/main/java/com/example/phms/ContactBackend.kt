package com.example.phms

import android.util.Log
import com.google.gson.Gson
import okhttp3.ResponseBody
import retrofit2.Call
import com.google.gson.annotations.SerializedName
import com.example.phms.AuthRequest

data class UserDTO(
    @SerializedName("firebaseUid") val firebaseUid: String,
    @SerializedName("firstName") val firstName: String,
    @SerializedName("lastName") val lastName: String,
    @SerializedName("email") val email: String,
    @SerializedName("age") val age: Int?,
    @SerializedName("height") val height: Double?,
    @SerializedName("weight") val weight: Double?
    ,@SerializedName("biometricEnabled") val biometricEnabled: Boolean = false
)


fun sendAuthTokenToBackend(token: String?) {
    if (token == null) {
        Log.e("Backend", "Token is null")
        return
    }

    val request = AuthRequest(token)

    RetrofitClient.apiService.authenticateUser(request).enqueue(object : retrofit2.Callback<ResponseBody> {
        override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
            if (response.isSuccessful) {
                Log.d("Backend", "User authenticated successfully: ${response.body()?.string()}")
            } else {
                Log.e("Backend", "Authentication failed: ${response.errorBody()?.string()}")
            }
        }

        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
            Log.e("Backend", "Error sending token: ${t.message}")
        }
    })
}

fun sendUserDataToBackend(firebaseUid: String?, email: String, firstName: String, lastName:String, age: String, height: String, weight: String,biometricEnabled: Boolean, callback: (String) -> Unit) {
    if (firebaseUid == null) {
        callback("Empty token")
        return
    }

    val request = UserDataRequest(
        firebaseUid = firebaseUid,
        firstName = firstName,
        lastName = lastName,
        email = email,
        age = age.toIntOrNull(),
        height = height.toDoubleOrNull(),
        weight = weight.toDoubleOrNull(),
        biometricEnabled = biometricEnabled
    )

    RetrofitClient.apiService.sendUserData(request).enqueue(object : retrofit2.Callback<ResponseBody> {
        override fun onResponse(call: Call<ResponseBody>, response: retrofit2.Response<ResponseBody>) {
            if (response.isSuccessful) {
                callback("User details saved successfully!")
            } else {
                callback("Failed to save user details: ${response.errorBody()?.string()}")
            }
        }

        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
            callback("Error: ${t.message}")
        }
    })
}

fun fetchUserData(firebaseUid: String, callback: (UserDTO?) -> Unit) {
    if (firebaseUid.isBlank()) {
        callback(null)
        return
    }

    RetrofitClient.apiService.getUser(firebaseUid).enqueue(object : retrofit2.Callback<UserDTO> {
        override fun onResponse(call: Call<UserDTO>, response: retrofit2.Response<UserDTO>) {
            if (response.isSuccessful) {
                callback(response.body())
            } else {
                Log.e("Backend", "Failed to fetch user data: ${response.errorBody()?.string()}")
                callback(null)
            }
        }

        override fun onFailure(call: Call<UserDTO>, t: Throwable) {
            Log.e("Backend", "Error fetching user data: ${t.message}")
            callback(null)
        }
    })
}

