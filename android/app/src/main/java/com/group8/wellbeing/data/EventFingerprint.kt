package com.group8.wellbeing.data

import com.group8.wellbeing.usage.NormalizedUsageEvent
import java.security.MessageDigest

object EventFingerprint {
    fun compute(event: NormalizedUsageEvent): String {
        val rawKey = buildString {
            append(event.timestampMs)
            append('|')
            append(event.eventType.name)
            append('|')
            append(event.packageToken ?: "")
            append('|')
            append(event.className ?: "")
            append('|')
            append(event.sourceEventCode)
            append('|')
            append(event.sourceVersion)
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(rawKey.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }
}
