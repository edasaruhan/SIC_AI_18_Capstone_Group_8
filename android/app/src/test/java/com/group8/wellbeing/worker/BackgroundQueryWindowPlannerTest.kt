package com.group8.wellbeing.worker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundQueryWindowPlannerTest {
    @Test
    fun t15_twoHourDelayedWorkerMarksCatchUpAndStartsWithSafeOverlap() {
        val lastCheckpoint = 1_795_417_200_000L
        val nowMs = lastCheckpoint + 7_200_000L
        val planned = BackgroundQueryWindowPlanner.plan(
            nowMs = nowMs,
            lastCheckpointMs = lastCheckpoint,
            resumedAtMs = null,
            backfillPausedPeriod = false
        )
        assertEquals(lastCheckpoint - 300_000L, planned.startMs)
        assertEquals(nowMs, planned.endMs)
        assertTrue(planned.isCatchUp)
    }

    @Test
    fun t22_resumeDoesNotBackfillPausedPeriodByDefault() {
        val lastCheckpoint = 1_795_417_200_000L
        val resumedAt = lastCheckpoint + 3_600_000L
        val nowMs = resumedAt + 900_000L
        val planned = BackgroundQueryWindowPlanner.plan(
            nowMs = nowMs,
            lastCheckpointMs = lastCheckpoint,
            resumedAtMs = resumedAt,
            backfillPausedPeriod = false
        )
        assertEquals(resumedAt, planned.startMs)
        assertEquals(nowMs, planned.endMs)
        assertFalse(planned.isCatchUp)
    }
}
