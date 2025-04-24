package com.example.models

import org.jetbrains.exposed.dao.id.IntIdTable

object Doctors: IntIdTable() {
    val userId = varchar("user_id", 255).references(Users.firebaseUid)
    val name = varchar("name", 100)
    val specialization = varchar("specialization", 100)
    val phone = varchar("phone", 30)
    val email = varchar("email", 100)
    val address = text("address")
    val notes = text("notes").nullable()
    val notifyOnEmergency = bool("notify_on_emergency").default(false)
}