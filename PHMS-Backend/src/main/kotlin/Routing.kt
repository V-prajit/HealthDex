// Routing.kt
package com.example

import com.example.dao.User
import com.example.dao.UserDAO
import com.example.dao.Doctor
import com.example.dao.DoctorDAO
import com.example.dao.Appointment
import com.example.dao.AppointmentDAO
import com.example.VitalsDAO
import com.example.VitalDTO
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

        route("/doctors") {
            get {
                val userId = call.request.queryParameters["userId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing userId")

                val doctors = DoctorDAO.getDoctorsByUserId(userId)
                call.respond(HttpStatusCode.OK, doctors)
            }

            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid doctor ID")

                val doctor = DoctorDAO.getDoctorById(id)
                    ?: return@get call.respond(HttpStatusCode.NotFound, "Doctor not found")

                call.respond(HttpStatusCode.OK, doctor)
            }

            post {
                val doctor = call.receive<Doctor>()
                val addedDoctor = DoctorDAO.addDoctor(doctor)
                call.respond(HttpStatusCode.Created, addedDoctor)
            }

            put {
                val doctor = call.receive<Doctor>()
                val updated = DoctorDAO.updateDoctor(doctor)

                if (updated) {
                    call.respond(HttpStatusCode.OK, doctor)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Doctor not found or missing ID")
                }
            }

            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid doctor ID")

                val deleted = DoctorDAO.deleteDoctor(id)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, "Doctor deleted")
                } else {
                    call.respond(HttpStatusCode.NotFound, "Doctor not found")
                }
            }
        }

        route("/appointments") {
            get {
                val userId = call.request.queryParameters["userId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing userId")

                val appointments = AppointmentDAO.getAppointmentsByUserId(userId)
                call.respond(HttpStatusCode.OK, appointments)
            }

            get("/upcoming") {
                val userId = call.request.queryParameters["userId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing userId")

                val appointments = AppointmentDAO.getUpcomingAppointments(userId)
                call.respond(HttpStatusCode.OK, appointments)
            }

            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid appointment ID")

                val appointment = AppointmentDAO.getAppointmentById(id)
                    ?: return@get call.respond(HttpStatusCode.NotFound, "Appointment not found")

                call.respond(HttpStatusCode.OK, appointment)
            }

            post {
                val appointment = call.receive<Appointment>()
                val addedAppointment = AppointmentDAO.addAppointment(appointment)
                call.respond(HttpStatusCode.Created, addedAppointment)
            }

            put {
                val appointment = call.receive<Appointment>()
                val updated = AppointmentDAO.updateAppointment(appointment)

                if (updated) {
                    call.respond(HttpStatusCode.OK, appointment)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Appointment not found or missing ID")
                }
            }

            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid appointment ID")

                val deleted = AppointmentDAO.deleteAppointment(id)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, "Appointment deleted")
                } else {
                    call.respond(HttpStatusCode.NotFound, "Appointment not found")
                }
            }
        }
    }
}
