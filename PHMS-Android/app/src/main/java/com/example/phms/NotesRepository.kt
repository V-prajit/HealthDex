package com.example.phms

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            notesDTO.map { "${it.title}\n${it.body}" }
        } catch (e: Exception) {
            //println("Error fetching notes: ${e.message}")
            emptyList()
        }
    }

    suspend fun saveNote(userId: String, note: String) = withContext(Dispatchers.IO) {
        try {
            val parts = note.split("\n", limit = 2)
            val title = parts.getOrElse(0) { "" }
            val body = parts.getOrElse(1) { "" }
            val noteDTO = NoteDTO(userId = userId, title = title, body = body)
            notesApi.addNote(noteDTO)
        } catch (e: Exception) {
        }
    }
}
