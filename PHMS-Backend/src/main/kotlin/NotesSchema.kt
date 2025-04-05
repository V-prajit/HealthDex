package com.example

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

// define the notes table to store notes for each user.
object Notes : Table() {
    val id = integer("id").autoIncrement()
    val userId = varchar("user_id", 255) // Firebase UID of the note owner
    val title = varchar("title", 255)
    val body = text("body")
    override val primaryKey = PrimaryKey(id)
}

//data transfer object for note
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
        Notes.selectAll().where { Notes.userId eq userId }.map{
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

    // Update an existing note
    fun updateNoteById(note: NoteDTO): Boolean = transaction {
    if (note.id == null) return@transaction false
    Notes.update({ Notes.id eq note.id }) {
        it[body] = note.body
        it[title] = note.title // in case title is edited too
    } > 0
}


    // delete a note
    fun deleteNoteById(id: Int): Boolean = transaction {
    Notes.deleteWhere { Notes.id eq id } > 0
    }

}
