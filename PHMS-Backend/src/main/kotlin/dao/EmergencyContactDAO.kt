package com.example.dao

import com.example.models.EmergencyContacts
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

@Serializable
data class EmergencyContact(
    val id: Int? = null,
    val userId: String,
    val name: String,
    val email: String,
    val phone: String,
    val relationship: String,
    val notifyOnEmergency: Boolean = true
)

object EmergencyContactDAO {
    fun addContact(contact: EmergencyContact): EmergencyContact = transaction {
        val id = EmergencyContacts.insert {
            it[userId] = contact.userId
            it[name] = contact.name
            it[email] = contact.email
            it[phone] = contact.phone
            it[relationship] = contact.relationship
            it[notifyOnEmergency] = contact.notifyOnEmergency
        } get EmergencyContacts.id

        contact.copy(id = id.value)
    }

    fun getContactsByUserId(userId: String): List<EmergencyContact> = transaction {
        EmergencyContacts.selectAll()
            .where { EmergencyContacts.userId eq userId }
            .map { row ->
                EmergencyContact(
                    id = row[EmergencyContacts.id].value,
                    userId = row[EmergencyContacts.userId],
                    name = row[EmergencyContacts.name],
                    email = row[EmergencyContacts.email],
                    phone = row[EmergencyContacts.phone],
                    relationship = row[EmergencyContacts.relationship],
                    notifyOnEmergency = row[EmergencyContacts.notifyOnEmergency]
                )
            }
    }

    fun updateContact(contact: EmergencyContact): Boolean = transaction {
        if (contact.id == null) return@transaction false

        EmergencyContacts.update({ EmergencyContacts.id eq contact.id }) {
            it[name] = contact.name
            it[email] = contact.email
            it[phone] = contact.phone
            it[relationship] = contact.relationship
            it[notifyOnEmergency] = contact.notifyOnEmergency
        } > 0
    }

    fun deleteContact(id: Int): Boolean = transaction {
        EmergencyContacts.deleteWhere { EmergencyContacts.id eq id } > 0
    }
}