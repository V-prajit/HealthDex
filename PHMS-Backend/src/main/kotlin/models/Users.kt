package com.example.models

import org.jetbrains.exposed.dao.id.IntIdTable

object Users: IntIdTable() {
    val firebaseUid = varchar("firebase_uid", 255).uniqueIndex()
    val firstName = varchar("first_name", 100)
    val lastName = varchar("last_name", 100)
    val email = varchar("email", 255).uniqueIndex()
    val age = integer("age").nullable()
    val height = double("height").nullable()
    val weight = double("weight").nullable()
    val biometricEnabled = bool("biometric_enabled").default(false)
    val securityQuestionId = integer("security_question_id").nullable()
    val securityAnswer = varchar("security_answer", 255).nullable()
}
