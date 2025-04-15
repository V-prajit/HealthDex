package com.example.service

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.example.dao.UserDAO

object NotificationService {
    private val fcm = FirebaseMessaging.getInstance()

    fun sendHighPriority(uid: String, signType: String, value: Float) {
        val token = UserDAO.getFcmToken(uid) ?: return
        val msg = Message.builder()
            .setToken(token)
            .putData("type", "ALERT")
            .putData("sign", signType)
            .putData("value", value.toString())
            .build()
        fcm.sendAsync(msg)  // fire‑and‑forget
        // TODO call SmsService.send() or EmailService.send() if you wire them
    }
}
