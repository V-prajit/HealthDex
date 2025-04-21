package com.example.models


import org.jetbrains.exposed.dao.id.IntIdTable

object EmergencyContacts: IntIdTable() {
    val userId = varchar("user_id", 255).references(Users.firebaseUid)
    val name = varchar("name", 100)
    val email = varchar("email", 100)
    val phone = varchar("phone", 30)
    val relationship = varchar("relationship", 50)
    val notifyOnEmergency = bool("notify_on_emergency").default(true)
}