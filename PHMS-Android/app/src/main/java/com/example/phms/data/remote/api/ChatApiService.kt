package com.example.phms.data.remote.api

import android.util.Log
import com.example.phms.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ChatApiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Use the API key from BuildConfig
    private val apiKey = BuildConfig.OPENAI_API_KEY

    private val model = "gpt-4o-mini"

    private val apiUrl = "https://api.openai.com/v1/chat/completions"

    suspend fun sendMessage(
        messages: List<ChatMessage>,
        onResponse: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val result = withContext(Dispatchers.IO) {
                val messagesJson = JSONArray()

                // Add a system message to provide context about health-related questions
                messagesJson.put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are a helpful assistant answering health-related questions. " +
                            "Provide informative responses, but always clarify that your advice is not a substitute " +
                            "for consulting a healthcare professional.")
                })

                // Add the conversation history
                messages.forEach { message ->
                    messagesJson.put(JSONObject().apply {
                        put("role", message.role)
                        put("content", message.content)
                    })
                }

                val requestJson = JSONObject().apply {
                    put("model", model)
                    put("messages", messagesJson)
                    put("max_tokens", 500)
                    put("temperature", 0.7)
                }

                val requestBody = requestJson.toString()
                    .toRequestBody("application/json".toMediaTypeOrNull())

                val request = Request.Builder()
                    .url(apiUrl)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

                try {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string() ?: ""

                    if (response.isSuccessful) {
                        val jsonObject = JSONObject(responseBody)
                        val choices = jsonObject.getJSONArray("choices")
                        if (choices.length() > 0) {
                            val firstChoice = choices.getJSONObject(0)
                            val message = firstChoice.getJSONObject("message")
                            message.getString("content")
                        } else {
                            "I'm sorry, I couldn't generate a response."
                        }
                    } else {
                        Log.e("ChatApiService", "API error: $responseBody")
                        "Error: ${response.code}. Please try again."
                    }
                } catch (e: Exception) {
                    Log.e("ChatApiService", "Request failed: ${e.message}")
                    "Network error: ${e.message}. Please check your connection."
                }
            }

            onResponse(result)
        } catch (e: Exception) {
            Log.e("ChatApiService", "Exception: ${e.message}")
            onError("An error occurred: ${e.message}")
        }
    }
}

// Data class to represent chat messages
data class ChatMessage(
    val role: String, // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)