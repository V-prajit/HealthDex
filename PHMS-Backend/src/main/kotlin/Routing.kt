// Routing.kt
package com.example

import com.example.dao.User
import com.example.dao.UserDAO
import com.example.dao.Doctor
import com.example.dao.DoctorDAO
import com.example.dao.Appointment
import com.example.dao.AppointmentDAO
import com.example.dao.EmergencyContact
import com.example.dao.EmergencyContactDAO
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

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
    val biometricEnabled: Boolean,
    val securityQuestionId: Int? = null,
    val securityAnswer: String? = null
)

@Serializable
data class VitalAlertRequest(
    val userId: String,
    val vitalName: String,
    val value: Float,
    val threshold: Float,
    val isHigh: Boolean
)

@Serializable
data class VerificationResponse(
    val verified: Boolean
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
                        user.biometricEnabled,
                        user.securityQuestionId,
                        user.securityAnswer
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

            get("/email/{email}") {
                val email = call.parameters["email"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing email")

                // Log the request
                log.info("Looking up user with email: $email")

                try {
                    val users = UserDAO.getUsersByEmail(email)
                    if (users.isNotEmpty()) {
                        log.info("User found with email: $email")
                        call.respond(HttpStatusCode.OK, users.first())
                    } else {
                        log.warn("No user found with email: $email")
                        call.respond(HttpStatusCode.NotFound, "User not found")
                    }
                } catch (e: Exception) {
                    log.error("Error looking up user by email", e)
                    call.respond(HttpStatusCode.InternalServerError, "Server error: ${e.message}")
                }
            }

            post("/verify-security-question") {
                val userId = call.request.queryParameters["userId"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing userId")
                val questionId = call.request.queryParameters["questionId"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid questionId")
                val answer = call.request.queryParameters["answer"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing answer")

                val user = UserDAO.getUserByFirebaseUid(userId)
                if (user != null && user.securityQuestionId == questionId && user.securityAnswer?.equals(answer, ignoreCase = true) == true) {
                    call.respond(HttpStatusCode.OK, VerificationResponse(true))
                } else {
                    call.respond(HttpStatusCode.OK, VerificationResponse(false))
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

        route("/diets") {
            get {
                val userId = call.request.queryParameters["userId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing userId")

                val date = call.request.queryParameters["date"]

                val diets = if (date != null) {
                    try {
                        val parsedDate = LocalDate.parse(date)
                        DietDAO.getAllDietsByUser(userId).filter {
                            try {
                                val entryDate = LocalDateTime.parse(it.timestamp).toLocalDate()
                                entryDate == parsedDate
                            } catch (e: Exception) {
                                false
                            }
                        }
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid date format")
                        return@get
                    }
                } else {
                    // Otherwise return all diets for the user
                    DietDAO.getAllDietsByUser(userId)
                }

                call.respond(HttpStatusCode.OK, diets)
            }

            post {
                val diet = call.receive<DietDTO>()
                call.respond(HttpStatusCode.Created, DietDAO.addDiet(diet))
            }

            put {
                val diet = call.receive<DietDTO>()
                if (DietDAO.updateDiet(diet)) {
                    call.respond(HttpStatusCode.OK, diet)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Diet not found or missing ID")
                }
            }

            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid diet ID")

                if (DietDAO.deleteDiet(id)) {
                    call.respond(HttpStatusCode.OK, "Diet deleted")
                } else {
                    call.respond(HttpStatusCode.NotFound, "Diet not found")
                }
            }

            get("/goals") {
                val userId = call.request.queryParameters["userId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing userId")

                val goals = mapOf(
                    "calorieGoal" to 2000,
                    "proteinGoal" to 75,
                    "fatGoal" to 65,
                    "carbGoal" to 300
                )

                call.respond(HttpStatusCode.OK, goals)
            }

            post("/goals") {
                val userId = call.request.queryParameters["userId"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing userId")

                val goals = call.receive<Map<String, Int>>()
                call.respond(HttpStatusCode.OK, goals)
            }

            get("/goals") {
                val userId = call.request.queryParameters["userId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing userId")

                val goals = DietGoalDAO.getGoalsByUserId(userId) ?: DietGoalDTO(
                    userId = userId,
                    calorieGoal = 2000,
                    proteinGoal = 75,
                    fatGoal = 65,
                    carbGoal = 300
                )

                call.respond(HttpStatusCode.OK, goals)
            }

            post("/goals") {
                val goals = call.receive<DietGoalDTO>()
                val updatedGoals = DietGoalDAO.setGoals(goals)
                call.respond(HttpStatusCode.OK, updatedGoals)
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

        route("/emergency-contacts") {
            get {
                val userId = call.request.queryParameters["userId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing userId")

                val contacts = EmergencyContactDAO.getContactsByUserId(userId)
                call.respond(HttpStatusCode.OK, contacts)
            }

            post {
                val contact = call.receive<EmergencyContact>()
                val addedContact = EmergencyContactDAO.addContact(contact)
                call.respond(HttpStatusCode.Created, addedContact)
            }

            put {
                val contact = call.receive<EmergencyContact>()
                val updated = EmergencyContactDAO.updateContact(contact)

                if (updated) {
                    call.respond(HttpStatusCode.OK, contact)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Contact not found or missing ID")
                }
            }

            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid ID")

                val deleted = EmergencyContactDAO.deleteContact(id)
                if (deleted) {
                    call.respond(HttpStatusCode.OK, "Contact deleted")
                } else {
                    call.respond(HttpStatusCode.NotFound, "Contact not found")
                }
            }
        }

        post("/send-vital-alert") {
            val alertRequest = call.receive<VitalAlertRequest>()

            val contacts = EmergencyContactDAO.getContactsByUserId(alertRequest.userId)
                .filter { it.notifyOnEmergency }

            val doctors = DoctorDAO.getDoctorsByUserId(alertRequest.userId)
                .filter { it.notifyOnEmergency }

            val user = UserDAO.getUserByFirebaseUid(alertRequest.userId)
            val patientName = "${user?.firstName} ${user?.lastName}"

            var emailsSent = 0

            // Send emails to emergency contacts
            contacts.forEach { contact ->
                if (EmailService.sendVitalAlertEmail(
                        recipientEmail = contact.email,
                        recipientName = contact.name,
                        patientName = patientName,
                        vitalName = alertRequest.vitalName,
                        value = alertRequest.value,
                        threshold = alertRequest.threshold,
                        isHigh = alertRequest.isHigh
                    )) {
                    emailsSent++
                }
            }

            // Send emails to doctors
            doctors.forEach { doctor ->
                if (EmailService.sendVitalAlertEmail(
                        recipientEmail = doctor.email,
                        recipientName = "Dr. ${doctor.name}",
                        patientName = patientName,
                        vitalName = alertRequest.vitalName,
                        value = alertRequest.value,
                        threshold = alertRequest.threshold,
                        isHigh = alertRequest.isHigh
                    )) {
                    emailsSent++
                }
            }

            call.respond(HttpStatusCode.OK, mapOf("emailsSent" to emailsSent))
        }
    }
}
