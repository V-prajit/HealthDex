package com.example.phms

import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SimpleCallback<T>(
    private val onResult: (T?) -> Unit,
    private val tag: String
) : Callback<T> {
    override fun onResponse(call: Call<T>, resp: Response<T>) {
        onResult(resp.body())
    }
    override fun onFailure(call: Call<T>, t: Throwable) {
        Log.e(tag, "API error", t)
        onResult(null)
    }
}
    