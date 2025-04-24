package com.example

import com.example.Vitals
import com.example.Medications
import com.example.Notes
import com.example.models.Users
import com.example.models.Doctors
import com.example.models.Appointments
import com.example.models.EmergencyContacts
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init(){
       Database.connect("jdbc:sqlite:./data.db", driver = "org.sqlite.JDBC")

        transaction {
            SchemaUtils.create(Users, Notes, Vitals, Doctors, Appointments, EmergencyContacts, Medications)
        }
    }
}