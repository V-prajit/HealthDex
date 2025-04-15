package com.example.dao

import com.example.models.Users
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class User(
    val firebaseUid: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val age: Int?,
    val height: Double?,
    val weight: Double?,
    val biometricEnabled: Boolean
)

object UserDAO {
    fun addUser(user: User) {
        try {
            transaction {
                Users.insert {
                    it[firebaseUid] = user.firebaseUid
                    it[firstName] = user.firstName
                    it[lastName] = user.lastName
                    it[email] = user.email
                    it[age] = user.age
                    it[height] = user.height
                    it[weight] = user.weight
                    it[biometricEnabled] = user.biometricEnabled
                }
            }
            println("Successfully added user: ${user.firstName} ${user.lastName}")
        } catch(e: Exception) {
            println("Error adding user to database: ${e.message}")
            e.printStackTrace()
        }
    }


    fun getUserByFirebaseUid(firebaseUid: String): User? {
        return transaction {
            Users.select { Users.firebaseUid eq firebaseUid }
                .map {
                    User(
                        it[Users.firebaseUid],
                        it[Users.firstName],
                        it[Users.lastName],
                        it[Users.email],
                        it[Users.age],
                        it[Users.height],
                        it[Users.weight],
                        it[Users.biometricEnabled]
                    )
                }.singleOrNull()
        }
    }

    fun setFcmToken(uid: String, token: String) = transaction {
        Users.update({ Users.firebaseUid eq uid }) {
            it[fcmToken] = token
        }
    }
    fun getFcmToken(uid: String): String? = transaction {
        Users
            .slice(Users.fcmToken)
            .select { Users.firebaseUid eq uid }
            .singleOrNull()
            ?.get(Users.fcmToken)
    }
}
