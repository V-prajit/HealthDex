package com.example

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.serialization.Serializable

// define the notes table to store notes for each user.
object Notes : Table() {
    val id = integer("id").autoIncrement()
    val userId = varchar("user_id", 255) // Firebase UID of the note owner
    val title = varchar("title", 255)
    val body = text("body")
    override val primaryKey = PrimaryKey(id)
}

//Data transfer object for Note
@Serializable
data class NoteDTO(
    val id: Int? = null,
    val userId: String,
    val title: String,
    val body: String
)

// for interacting with the notes table
object NotesDAO {
    fun getNotesForUser(userId: String): List<NoteDTO> = transaction {
        Notes.select { Notes.userId eq userId }.map {
            NoteDTO(
                id = it[Notes.id],
                userId = it[Notes.userId],
                title = it[Notes.title],
                body = it[Notes.body]
            )
        }
    }

    fun getAllNotes(): List<NoteDTO> = transaction {
        Notes.selectAll().map {
            NoteDTO(
                id = it[Notes.id],
                userId = it[Notes.userId],
                title = it[Notes.title],
                body = it[Notes.body]
            )
        }
    }

    fun addNote(note: NoteDTO): NoteDTO = transaction {
        val insertedId = Notes.insert {
            it[userId] = note.userId
            it[title] = note.title
            it[body] = note.body
        } get Notes.id
        note.copy(id = insertedId)
    }
}
