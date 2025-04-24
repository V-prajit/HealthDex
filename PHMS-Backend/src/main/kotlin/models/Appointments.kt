package com.example.models

import org.jetbrains.exposed.dao.id.IntIdTable

object Appointments: IntIdTable() {
    val userId = varchar("user_id", 255).references(Users.firebaseUid)
    val doctorId = reference("doctor_id", Doctors)
    val date = varchar("date", 20)
    val time = varchar("time", 20)
    val duration = integer("duration")
    val reason = varchar("reason", 255)
    val notes = text("notes").nullable()
    val status = varchar("status", 20).default("scheduled") // scheduled, completed, cancelled
    val reminders = bool("reminders").default(true)
}