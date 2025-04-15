package com.example.service

import com.example.dao.*
import io.ktor.server.application.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

class MonitoringService(
    private val vitalDao: VitalSignDAO,
    private val alertDao: AlertDAO
) {
    private val limits = mapOf(
        "BloodPressure" to 180f,
        "Glucose"       to 250f,
        "HeartRate"     to 130f
    )

    fun start(app: Application) {
        app.environment.log.info("Monitoring loop started")
        app.launch {
            while (true) {
                limits.forEach { (type, limit) ->
                    vitalDao.byTypeOver(limit, type).forEach { dto ->
                        if (!alertDao.alreadySent(dto.firebaseUid, type)) {
                            alertDao.save(dto.firebaseUid, type, dto.value)
                            NotificationService.sendHighPriority(dto.firebaseUid, type, dto.value)
                        }
                    }
                }
                delay(15.minutes)
            }
        }
    }
}
