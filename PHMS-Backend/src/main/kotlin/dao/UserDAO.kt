package com.example.dao

import com.example.models.Users
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class User(
    val firebaseUid: String,
    val name: String,
    val email: String,
    val age: Int?,
    val height: Double?,
    val weight: Double?
)

object UserDAO {
    fun addUser(user: User) {
        transaction {
            Users.insert {
                it[firebaseUid] = user.firebaseUid
                it[name] = user.name
                it[email] = user.email
                it[age] = user.age
                it[height] = user.height
                it[weight] = user.weight
            }
        }
    }

    fun getUserByFirebaseUid(firebaseUid: String): User? {
        return transaction {
            Users.selectAll().where { Users.firebaseUid eq firebaseUid }
                .map {
                    User(
                        it[Users.firebaseUid],
                        it[Users.name],
                        it[Users.email],
                        it[Users.age],
                        it[Users.height],
                        it[Users.weight]
                    )
                }.singleOrNull()
        }
    }
}
