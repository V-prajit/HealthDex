// Routing.kt
package com.example

import com.example.dao.User
import com.example.dao.UserDAO
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import java.rmi.server.UID

@Serializable
data class AuthRequest(val token: String)

@Serializable
data class UserDTO(val firebaseUid: String, val firstName: String, val lastName: String, val email: String, val age: Int?, val height: Double?, val weight: Double?,val biometricEnabled: Boolean )

fun Application.configureRouting() {
    val userThemeMap = mutableMapOf<String, Boolean>()
    routing {
        post("/auth"){
            val request = call.receive<AuthRequest>()
            val firebaseUser = verifyFirebaseToken(request.token)

            if (firebaseUser != null){
                call.respond(HttpStatusCode.OK, "User authenticated: ${firebaseUser.uid}")
            } else {
                call.respond(HttpStatusCode.Unauthorized, "Invalid token")
            }
        }

        get("/") {
            call.respondText("Welcome to PHMS Backend")
        }

        get("/test") {
            call.respondText("Test API is working!")
        }

        route("/users"){
            post("/register"){
                val user = call.receive<UserDTO>()
                UserDAO.addUser(User( user.firebaseUid, user.firstName, user.lastName, user.email, user.age, user.height, user.weight,user.biometricEnabled ))
                call.respond(HttpStatusCode.Created, "User added successfully")
            }

            get("/{firebaseUid}"){
                val firebaseUid = call.parameters["firebaseUid"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing Firebase UID")
                val user = UserDAO.getUserByFirebaseUid(firebaseUid)
                if (user != null) {
                    call.respond(HttpStatusCode.OK, user)
                } else {
                    call.respond(HttpStatusCode.NotFound, "User not found")
                }
            }
        }
        
        route("/notes") {
            get {
                val userId = call.request.queryParameters["userId"]
                val notes = if (userId != null) {
                    NotesDAO.getNotesForUser(userId)
                } else {
                    NotesDAO.getAllNotes()
                }
                call.respond(HttpStatusCode.OK, notes)
            }
            post {
                val note = call.receive<NoteDTO>()
                val addedNote = NotesDAO.addNote(note)
                call.respond(HttpStatusCode.Created, addedNote)
            }
            put {
            val note = call.receive<NoteDTO>()
            val updated = NotesDAO.updateNoteById(note)
            if (updated) {
                call.respond(HttpStatusCode.OK, note)
            } else {
                call.respond(HttpStatusCode.NotFound, "Note not found or missing ID")
            }
}

delete("{id}") {
    val id = call.parameters["id"]?.toIntOrNull()
    if (id == null) {
        call.respond(HttpStatusCode.BadRequest, "Missing or invalid note ID")
        return@delete
    }
    val deleted = NotesDAO.deleteNoteById(id)
    if (deleted) {
        call.respond(HttpStatusCode.OK, "Note deleted")
    } else {
        call.respond(HttpStatusCode.NotFound, "Note not found")
    }
}

        }
        route("/theme") {
            // Update user's dark mode setting.
            post {
                val payload = call.receive<Map<String, Any>>()
                val firebaseUid = payload["firebaseUid"] as? String
                val darkMode = payload["darkMode"] as? Boolean ?: false
                if (firebaseUid == null) {
                    call.respond(HttpStatusCode.BadRequest, "Missing firebaseUid")
                } else {
                    userThemeMap[firebaseUid] = darkMode
                    call.respond(HttpStatusCode.OK, mapOf("darkMode" to darkMode))
                }
            }
            get("{firebaseUid}") {
                val firebaseUid = call.parameters["firebaseUid"]
                if (firebaseUid == null) {
                    call.respond(HttpStatusCode.BadRequest, "Missing firebaseUid")
                } else {
                    val darkMode = userThemeMap[firebaseUid] ?: false
                    call.respond(HttpStatusCode.OK, mapOf("darkMode" to darkMode))
                }
            }
        }
    }
}
