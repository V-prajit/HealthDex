package com.example.phms.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

object NotesRepository {
    private const val PREFS_NAME = "notes_prefs"
    private const val NOTES_KEY = "notes_key"

    fun getNotes(context: Context): MutableList<String> {
        val prefs: SharedPreferences =
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val notesJson = prefs.getString(NOTES_KEY, null)
        return if (notesJson != null) {
            val type = object : TypeToken<MutableList<String>>() {}.type
            Gson().fromJson(notesJson, type)
        } else {
            mutableListOf()
        }
    }

    fun saveNotes(context: Context, notes: List<String>) {
        val prefs: SharedPreferences =
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val notesJson = Gson().toJson(notes)
        editor.putString(NOTES_KEY, notesJson)
        editor.apply()
    }
}

data class NoteDTO(
    val id: Int? = null,
    val userId: String,
    val title: String,
    val body: String
)

interface NotesApi {
    @GET("notes")
    suspend fun getNotes(@Query("userId") userId: String? = null): List<NoteDTO>

    @POST("notes")
    suspend fun addNote(@Body note: NoteDTO): NoteDTO

    @PUT("notes")
    suspend fun updateNote(@Body note: NoteDTO): NoteDTO

    @DELETE("notes/{id}")
    suspend fun deleteNote(@Path("id") id: Int): retrofit2.Response<Unit>
}

object NotesRepositoryBackend {
    private const val BASE_URL = "http://10.0.2.2:8085"
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val notesApi = retrofit.create(NotesApi::class.java)

    suspend fun getNotes(userId: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val notesDTO = notesApi.getNotes(userId)
            notesDTO.map { "${it.id}|${it.title}\n${it.body}" }
        } catch (e: Exception) {
            //println("Error fetching notes: ${e.message}")
            emptyList()
        }
    }

    suspend fun saveNote(userId: String, note: String) = withContext(Dispatchers.IO) {
        try {
            val lines = note.split("\n", limit = 2)
            val firstLine = lines.getOrElse(0) { "" }
            val body = lines.getOrElse(1) { "" }
            if (firstLine.contains("|")) {
                val parts = firstLine.split("|")
                val id = parts[0].toIntOrNull()
                val title = parts.getOrElse(1) { "" }
                if (id != null) {
                    val noteDTO = NoteDTO(id = id, userId = userId, title = title, body = body)
                    notesApi.updateNote(noteDTO)
                    return@withContext
                }
            }
            val title = firstLine
            val noteDTO = NoteDTO(userId = userId, title = title, body = body)
            notesApi.addNote(noteDTO)
        } catch (e: Exception) {
        }
    }

    suspend fun deleteNote(id: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = notesApi.deleteNote(id)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
