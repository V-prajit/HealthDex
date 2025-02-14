package com.example.models

import org.jetbrains.exposed.dao.id.IntIdTable

object Users: IntIdTable() {
    val firebaseUid = varchar("firebase_uid", 255).uniqueIndex()
    val firstName = varchar("First name", 100)
    val lastName = varchar("Last Name", 100)
    val email = varchar("email", 255).uniqueIndex()
    val age = integer("age").nullable()
    val height = double("height").nullable()
    val weight = double("weight").nullable()
}
