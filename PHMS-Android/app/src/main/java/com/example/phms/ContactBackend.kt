package com.example.phms

import android.util.Log
import okhttp3.ResponseBody
import retrofit2.Call

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
