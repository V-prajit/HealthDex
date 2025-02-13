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
data class UserDTO(val firebaseUid: String, val name: String, val email: String, val age: Int?, val height: Double?, val weight: Double?)

fun Application.configureRouting() {
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
                UserDAO.addUser(User( user.firebaseUid, user.name, user.email, user.age, user.height, user.weight ))
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
    }
}

