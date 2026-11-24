package com.group8.wellbeing.data

import com.group8.wellbeing.usage.NormalizedEventType
import com.group8.wellbeing.usage.NormalizedUsageEvent
import com.group8.wellbeing.usage.UsageEventNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class EventFingerprintTest {
    @Test
    fun t05_identicalEventsProduceSameFingerprint() {
        val first = NormalizedUsageEvent(
            timestampMs = 1_700_000_005_000L,
            eventType = NormalizedEventType.APP_FOREGROUND,
            packageName = "com.example.reader",
            packageToken = "pkg_aabbccddeeff",
            className = "com.example.reader.MainActivity",
            isNonAppPackage = false,
            sourceEventCode = UsageEventNormalizer.EVENT_ACTIVITY_RESUMED,
            sourceVersion = UsageEventNormalizer.SOURCE_VERSION
        )
        val duplicate = first.copy()
        assertEquals(EventFingerprint.compute(first), EventFingerprint.compute(duplicate))
    }

    @Test
    fun distinctEventsAtSameMillisecondAreNotFalselyDeduplicated() {
        val unlockEvent = NormalizedUsageEvent(
            timestampMs = 1_700_000_005_000L,
            eventType = NormalizedEventType.KEYGUARD_HIDDEN,
            packageName = null,
            packageToken = null,
            className = null,
            isNonAppPackage = false,
            sourceEventCode = UsageEventNormalizer.EVENT_KEYGUARD_HIDDEN,
            sourceVersion = UsageEventNormalizer.SOURCE_VERSION
        )
        val appEvent = NormalizedUsageEvent(
            timestampMs = 1_700_000_005_000L,
            eventType = NormalizedEventType.APP_FOREGROUND,
            packageName = "com.example.reader",
            packageToken = "pkg_aabbccddeeff",
            className = "com.example.reader.MainActivity",
            isNonAppPackage = false,
            sourceEventCode = UsageEventNormalizer.EVENT_ACTIVITY_RESUMED,
            sourceVersion = UsageEventNormalizer.SOURCE_VERSION
        )
        val otherAppEvent = appEvent.copy(
            packageName = "com.example.notes",
            packageToken = "pkg_112233445566",
            className = "com.example.notes.HomeActivity"
        )
        assertNotEquals(EventFingerprint.compute(unlockEvent), EventFingerprint.compute(appEvent))
        assertNotEquals(EventFingerprint.compute(appEvent), EventFingerprint.compute(otherAppEvent))
    }
}
