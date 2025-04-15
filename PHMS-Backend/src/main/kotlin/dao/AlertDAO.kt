package com.example.dao

import com.example.models.Alerts
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.datetime.Clock
import java.util.UUID

class AlertDAO {

    fun alreadySent(uid: String, type: String): Boolean = transaction {
        Alerts.select { (Alerts.firebaseUid eq uid) and (Alerts.signType eq type) }.any()
    }

    fun save(uid: String, type: String, value: Float) = transaction {
        Alerts.insert {
            it[alertId]     = UUID.randomUUID().toString()
            it[firebaseUid] = uid
            it[signType]    = type
            it[Alerts.value] = value
            it[sentAt]      = Clock.System.now().toEpochMilliseconds()
        }
    }
}
