package com.example.phms

import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object DietRepository {
    private val api = RetrofitClient.apiService

    fun fetchAll(userId: String, onResult: (List<DietDTO>?) -> Unit) {
        api.getAllDiets(userId).enqueue(object : Callback<List<DietDTO>> {
            override fun onResponse(call: Call<List<DietDTO>>, resp: Response<List<DietDTO>>) {
                onResult(resp.body())
            }
            override fun onFailure(call: Call<List<DietDTO>>, t: Throwable) {
                Log.e("DietRepo", "Error fetching diets", t)
                onResult(null)
            }
        })
    }

    fun add(entry: DietDTO, onResult: (DietDTO?) -> Unit) {
        api.addDiet(entry).enqueue(SimpleCallback(onResult, "addDiet"))
    }
    fun update(entry: DietDTO, onResult: (DietDTO?) -> Unit) {
        api.updateDiet(entry).enqueue(SimpleCallback(onResult, "updateDiet"))
    }
    fun delete(id: Int, onResult: (Boolean) -> Unit) {
        api.deleteDiet(id).enqueue(object : Callback<Void> {
            override fun onResponse(c: Call<Void>, r: Response<Void>) = onResult(r.isSuccessful)
            override fun onFailure(c: Call<Void>, t: Throwable) {
                Log.e("DietRepo", "Error deleting diet", t)
                onResult(false)
            }
        })
    }
}