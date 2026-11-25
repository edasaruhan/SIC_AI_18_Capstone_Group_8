package com.group8.wellbeing.usage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageEventNormalizerTest {
    @Test
    fun mapsAllFiveCandidateAndroidEventTypesAndIgnoresOthers() {
        assertEquals(NormalizedEventType.SCREEN_INTERACTIVE, UsageEventNormalizer.mapEventType(15))
        assertEquals(NormalizedEventType.KEYGUARD_HIDDEN, UsageEventNormalizer.mapEventType(18))
        assertEquals(NormalizedEventType.KEYGUARD_SHOWN, UsageEventNormalizer.mapEventType(17))
        assertEquals(NormalizedEventType.SCREEN_NON_INTERACTIVE, UsageEventNormalizer.mapEventType(16))
        assertEquals(NormalizedEventType.APP_FOREGROUND, UsageEventNormalizer.mapEventType(1))
        assertNull(UsageEventNormalizer.mapEventType(2))
        assertNull(UsageEventNormalizer.mapEventType(23))
    }

    @Test
    fun enforcesHalfOpenIntervalAndChronologicalOrder() {
        val raw = listOf(
            RawSystemEvent(999L, 18, null, null),
            RawSystemEvent(1500L, 1, "com.example.reader", "MainActivity"),
            RawSystemEvent(1000L, 18, null, null),
            RawSystemEvent(2000L, 16, null, null)
        )
        val result = UsageEventNormalizer.normalize(raw, startMs = 1000L, endMs = 2000L)
        assertEquals(2, result.size)
        assertEquals(1000L, result[0].timestampMs)
        assertEquals(NormalizedEventType.KEYGUARD_HIDDEN, result[0].eventType)
        assertEquals(1500L, result[1].timestampMs)
        assertEquals(NormalizedEventType.APP_FOREGROUND, result[1].eventType)
    }

    @Test
    fun classifiesKeyboardLauncherAndSystemPackagesAsNonApp() {
        assertTrue(UsageEventNormalizer.isNonAppPackage("com.google.android.inputmethod.latin"))
        assertTrue(UsageEventNormalizer.isNonAppPackage("com.google.android.apps.nexuslauncher"))
        assertTrue(UsageEventNormalizer.isNonAppPackage("com.android.systemui"))
        assertTrue(UsageEventNormalizer.isNonAppPackage(null))
        assertFalse(UsageEventNormalizer.isNonAppPackage("com.example.social"))
    }

    @Test
    fun tokenizesPackagesDeterministicallyWithoutExposingRawName() {
        val tokenA1 = UsageEventNormalizer.tokenizePackage("com.example.appA")
        val tokenA2 = UsageEventNormalizer.tokenizePackage("COM.EXAMPLE.APPA")
        val tokenB = UsageEventNormalizer.tokenizePackage("com.example.appB")

        assertEquals(tokenA1, tokenA2)
        assertNotEquals(tokenA1, tokenB)
        assertTrue(tokenA1!!.startsWith("pkg_"))
        assertFalse(tokenA1.contains("example"))
    }
}
