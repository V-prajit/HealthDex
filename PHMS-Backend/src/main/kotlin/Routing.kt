// Routing.kt
package com.example

import com.example.VitalsDAO
import com.example.VitalDTO
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import java.rmi.server.UID
import com.example.NotesDAO
import com.example.NoteDTO
import com.example.DietDAO
import com.example.DietDTO
import com.example.MedicationDAO
import com.example.MedicationDTO
import com.example.dao.UserDAO
import com.example.dao.User


@Serializable
data class AuthRequest(val token: String)

@Serializable
data class UserDTO(
    val firebaseUid: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val age: Int?,
    val height: Double?,
    val weight: Double?,
    val biometricEnabled: Boolean
)

fun Application.configureRouting() {
    val userThemeMap = mutableMapOf<String, Boolean>()

    routing {
        post("/auth") {
            val request = call.receive<AuthRequest>()
            val firebaseUser = verifyFirebaseToken(request.token)

            if (firebaseUser != null) {
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

        route("/users") {
            post("/register") {
                val user = call.receive<UserDTO>()
                UserDAO.addUser(
                    User(
                        user.firebaseUid,
                        user.firstName,
                        user.lastName,
                        user.email,
                        user.age,
                        user.height,
                        user.weight,
                        user.biometricEnabled
                    )
                )
                call.respond(HttpStatusCode.Created, "User added successfully")
            }

            get("/{firebaseUid}") {
                val firebaseUid = call.parameters["firebaseUid"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing Firebase UID")
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

        route("/vitals") {
            get {
                val userId = call.request.queryParameters["userId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing userId")
                call.respond(HttpStatusCode.OK, VitalsDAO.getAllByUser(userId))
            }

            get("/latest") {
                val userId = call.request.queryParameters["userId"]
                val type = call.request.queryParameters["type"]
                if (userId == null || type == null)
                    return@get call.respond(HttpStatusCode.BadRequest, "Missing userId or type")
                VitalsDAO.getLatest(userId, type)
                    ?.let { call.respond(HttpStatusCode.OK, it) }
                    ?: call.respond(HttpStatusCode.NotFound, "No record found")
            }

            post {
                val vital = call.receive<VitalDTO>()
                call.respond(HttpStatusCode.Created, VitalsDAO.add(vital))
            }

            put {
                val vital = call.receive<VitalDTO>()
                if (VitalsDAO.update(vital))
                    call.respond(HttpStatusCode.OK, vital)
                else
                    call.respond(HttpStatusCode.NotFound, "Vital not found or missing id")
            }

            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid id")
                if (VitalsDAO.delete(id))
                    call.respond(HttpStatusCode.OK, "Deleted")
                else
                    call.respond(HttpStatusCode.NotFound, "Vital not found")
            }
        }

        route("/theme") {
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

                // ===== Diet Endpoints =====
        route("/diet") {
            get {
                val userId = call.request.queryParameters["userId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing userId")
                call.respond(HttpStatusCode.OK, DietDAO.getAllDietsByUser(userId))
            }
            get("/latest") {
                val userId = call.request.queryParameters["userId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing userId")
                DietDAO.getLatestDietByUser(userId)
                ?.let { call.respond(HttpStatusCode.OK, it) }
                ?: call.respond(HttpStatusCode.NotFound, "No diet entry found")
            }
            post {
                val dto = call.receive<DietDTO>()
                val created = DietDAO.addDiet(dto)
                call.respond(HttpStatusCode.Created, created)
            }
            put {
                val dto = call.receive<DietDTO>()
                if (DietDAO.updateDiet(dto)) call.respond(HttpStatusCode.OK, dto)
                else                         call.respond(HttpStatusCode.NotFound, "Diet not found or missing id")
            }
            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid id")
                if (DietDAO.deleteDiet(id)) call.respond(HttpStatusCode.OK, "Deleted")
                else                        call.respond(HttpStatusCode.NotFound, "Diet not found")
            }
        }

        // ===== Medication Endpoints =====
        route("/medications") {
            get {
                val userId = call.request.queryParameters["userId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing userId")
                call.respond(HttpStatusCode.OK, MedicationDAO.getAllMedicationsByUser(userId))
            }
            get("/latest") {
                val userId = call.request.queryParameters["userId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing userId")
                MedicationDAO.getLatestMedicationByUser(userId)
                ?.let { call.respond(HttpStatusCode.OK, it) }
                ?: call.respond(HttpStatusCode.NotFound, "No medication record found")
            }
            post {
                val dto = call.receive<MedicationDTO>()
                val created = MedicationDAO.addMedication(dto)
                call.respond(HttpStatusCode.Created, created)
            }
            put {
                val dto = call.receive<MedicationDTO>()
                if (MedicationDAO.updateMedication(dto)) call.respond(HttpStatusCode.OK, dto)
                else                                    call.respond(HttpStatusCode.NotFound, "Medication not found or missing id")
            }
            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid id")
                if (MedicationDAO.deleteMedication(id)) call.respond(HttpStatusCode.OK, "Deleted")
                else                                    call.respond(HttpStatusCode.NotFound, "Medication not found")
            }
        }

    }
}
