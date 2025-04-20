package com.example

import com.example.Vitals
import com.example.Diets
import com.example.Medications
import com.example.Notes
import com.example.models.Users
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init(){
       Database.connect("jdbc:sqlite:./data.db", driver = "org.sqlite.JDBC")

        transaction {
            SchemaUtils.create(Users, Notes, Vitals, Diets, Medications)
        }
    }
}