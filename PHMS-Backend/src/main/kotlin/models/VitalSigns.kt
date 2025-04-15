package com.example.models

import org.jetbrains.exposed.sql.Table
import java.util.UUID

data class VitalSignDTO(
    val recordId: String = UUID.randomUUID().toString(),
    val firebaseUid: String,
    val signType: String,
    val value: Float,
    val unit: String,
    val epochMillis: Long
)

object VitalSigns : Table() {
    val recordId    = varchar("record_id", 36)
    val firebaseUid = varchar("firebase_uid", 255).index()
    val signType    = varchar("sign_type", 32)
    val value       = float("value")
    val unit        = varchar("unit", 16)
    val epochMillis = long("epoch_millis")
    override val primaryKey = PrimaryKey(recordId)
}

data class AlertDTO(
    val alertId: String = UUID.randomUUID().toString(),
    val firebaseUid: String,
    val signType: String,
    val value: Float,
    val sentAt: Long
)

object Alerts : Table() {
    val alertId    = varchar("alert_id", 36)
    val firebaseUid = varchar("firebase_uid", 255).index()
    val signType   = varchar("sign_type", 32)
    val value      = float("value")
    val sentAt     = long("sent_at")
    override val primaryKey = PrimaryKey(alertId)
}
