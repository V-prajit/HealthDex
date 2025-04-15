package com.example.dao

import com.example.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.datetime.Clock

class VitalSignDAO {
    fun add(dto: VitalSignDTO) = transaction {
        VitalSigns.insert {
            it[recordId] = dto.recordId
            it[firebaseUid] = dto.firebaseUid
            it[signType] = dto.signType
            it[value] = dto.value
            it[unit] = dto.unit
            it[epochMillis] = dto.epochMillis
        }
    }

    fun byUser(uid: String) = transaction {
        VitalSigns.select { VitalSigns.firebaseUid eq uid }
            .map {
                VitalSignDTO(
                    it[VitalSigns.recordId],
                    it[VitalSigns.firebaseUid],
                    it[VitalSigns.signType],
                    it[VitalSigns.value],
                    it[VitalSigns.unit],
                    it[VitalSigns.epochMillis]
                )
            }
    }

    fun latest(uid: String, type: String) = transaction {
        VitalSigns.select {
            (VitalSigns.firebaseUid eq uid) and (VitalSigns.signType eq type)
        }.orderBy(VitalSigns.epochMillis, SortOrder.DESC)
            .limit(1)
            .map {
                VitalSignDTO(
                    it[VitalSigns.recordId], it[VitalSigns.firebaseUid],
                    it[VitalSigns.signType], it[VitalSigns.value],
                    it[VitalSigns.unit], it[VitalSigns.epochMillis]
                )
            }.singleOrNull()
    }
    fun byTypeOver(limit: Float, type: String): List<VitalSignDTO> = transaction {
        VitalSigns
            .select { (VitalSigns.signType eq type) and (VitalSigns.value greater limit) }
            .map {
                VitalSignDTO(
                    it[VitalSigns.recordId],
                    it[VitalSigns.firebaseUid],
                    it[VitalSigns.signType],
                    it[VitalSigns.value],
                    it[VitalSigns.unit],
                    it[VitalSigns.epochMillis]
                )
            }
    }
}
