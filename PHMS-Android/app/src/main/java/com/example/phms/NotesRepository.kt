package com.example.phms
import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
object NotesRepository {
    private const val PREFS_NAME = "notes_prefs"
    private const val NOTES_KEY = "notes_key"

    fun getNotes(context: Context): MutableList<String> {
        val prefs: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val notesJson = Gson().toJson(notes)
        editor.putString(NOTES_KEY, notesJson)
        editor.apply()
    }
}
