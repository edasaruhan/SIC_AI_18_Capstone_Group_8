package com.group8.wellbeing.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AppConfigTest {
    @Test
    fun initialConfigurationMatchesRoadmapDefaults() {
        assertEquals(28, AppConfig.MIN_API_LEVEL)
        assertEquals(1_800_000L, AppConfig.WINDOW_DURATION_MS)
        assertEquals(10_800_000L, AppConfig.MAX_SESSION_DURATION_MS)
        assertEquals(15L, AppConfig.WORK_INTERVAL_MINUTES)
        assertEquals(3, AppConfig.DAILY_REMINDER_CAP)
        assertEquals(60, AppConfig.REMINDER_COOLDOWN_MINUTES)
        assertEquals(23, AppConfig.QUIET_START_HOUR)
        assertEquals(8, AppConfig.QUIET_END_HOUR)
        assertEquals(7, AppConfig.RAW_EVENT_RETENTION_DAYS)
        assertEquals(30, AppConfig.DERIVED_RETENTION_DAYS)
    }
}
