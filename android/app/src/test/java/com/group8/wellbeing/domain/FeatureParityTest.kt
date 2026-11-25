package com.group8.wellbeing.domain

import com.group8.wellbeing.data.EventFingerprint
import com.group8.wellbeing.usage.NormalizedEventType
import com.group8.wellbeing.usage.NormalizedUsageEvent
import com.group8.wellbeing.usage.UsageEventNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class FeatureParityTest {
    private val istanbul = ZoneId.of("Europe/Istanbul")
    private val tolerance = 0.001

    private fun ms(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int = 0): Long {
        return ZonedDateTime.of(year, month, day, hour, minute, second, 0, istanbul)
            .toInstant()
            .toEpochMilli()
    }

    @Test
    fun f01_t07_normalUnlockToOffSession() {
        val wStart = ms(2026, 11, 23, 10, 0)
        val wEnd = ms(2026, 11, 23, 10, 30)
        val events = listOf(
            DomainUsageEvent(ms(2026, 11, 23, 10, 5), NormalizedEventType.KEYGUARD_HIDDEN),
            DomainUsageEvent(ms(2026, 11, 23, 10, 15), NormalizedEventType.SCREEN_NON_INTERACTIVE)
        )
        val windows = FeatureWindowCalculator.computeWindows(events, wStart, wEnd, wEnd, istanbul)
        assertEquals(1, windows.size)
        val f = windows[0].features
        assertEquals(600.0, f.screenSeconds, tolerance)
        assertEquals(1, f.sessionsStarted)
        assertEquals(600.0, f.longestRunSeconds, tolerance)
        assertEquals(0, f.appSwitches)
        assertEquals(0.0, f.previousSessionSeconds, tolerance)
        assertEquals(10, f.hour)
        assertEquals(0, f.dayOfWeek)
        assertEquals(WindowQualityStatus.OBSERVATION_SUFFICIENT, windows[0].qualityStatus)
    }

    @Test
    fun f02_t09_sessionSpanningTwoWindows() {
        val start = ms(2026, 11, 23, 10, 0)
        val end = ms(2026, 11, 23, 11, 0)
        val events = listOf(
            DomainUsageEvent(ms(2026, 11, 23, 10, 20), NormalizedEventType.KEYGUARD_HIDDEN),
            DomainUsageEvent(ms(2026, 11, 23, 10, 40), NormalizedEventType.SCREEN_NON_INTERACTIVE)
        )
        val windows = FeatureWindowCalculator.computeWindows(events, start, end, end, istanbul)
        assertEquals(2, windows.size)

        assertEquals(600.0, windows[0].features.screenSeconds, tolerance)
        assertEquals(1, windows[0].features.sessionsStarted)
        assertEquals(600.0, windows[0].features.longestRunSeconds, tolerance)

        assertEquals(600.0, windows[1].features.screenSeconds, tolerance)
        assertEquals(0, windows[1].features.sessionsStarted)
        assertEquals(600.0, windows[1].features.longestRunSeconds, tolerance)
    }

    @Test
    fun f03_t08_duplicateUnlockInsideOpenSessionIgnored() {
        val wStart = ms(2026, 11, 23, 10, 0)
        val wEnd = ms(2026, 11, 23, 10, 30)
        val events = listOf(
            DomainUsageEvent(ms(2026, 11, 23, 10, 5), NormalizedEventType.KEYGUARD_HIDDEN),
            DomainUsageEvent(ms(2026, 11, 23, 10, 6), NormalizedEventType.KEYGUARD_HIDDEN),
            DomainUsageEvent(ms(2026, 11, 23, 10, 15), NormalizedEventType.SCREEN_NON_INTERACTIVE)
        )
        val reconstructed = SessionReconstructor.reconstruct(events, cutoffMs = wEnd)
        assertEquals(1, reconstructed.duplicateUnlockCount)
        assertEquals(1, reconstructed.sessions.size)

        val windows = FeatureWindowCalculator.computeWindows(events, wStart, wEnd, wEnd, istanbul)
        assertEquals(1, windows[0].features.sessionsStarted)
        assertEquals(600.0, windows[0].features.screenSeconds, tolerance)
    }

    @Test
    fun f04_exactBoundaryUnlockBelongsToNextWindow() {
        val start = ms(2026, 11, 23, 10, 0)
        val end = ms(2026, 11, 23, 11, 0)
        val events = listOf(
            DomainUsageEvent(ms(2026, 11, 23, 10, 30), NormalizedEventType.KEYGUARD_HIDDEN),
            DomainUsageEvent(ms(2026, 11, 23, 10, 35), NormalizedEventType.SCREEN_NON_INTERACTIVE)
        )
        val windows = FeatureWindowCalculator.computeWindows(events, start, end, end, istanbul)
        assertEquals(2, windows.size)
        assertEquals(0, windows[0].features.sessionsStarted)
        assertEquals(0.0, windows[0].features.screenSeconds, tolerance)
        assertEquals(1, windows[1].features.sessionsStarted)
        assertEquals(300.0, windows[1].features.screenSeconds, tolerance)
    }

    @Test
    fun f05_previousSessionCompletedBeforeWindowStart() {
        val start = ms(2026, 11, 23, 10, 0)
        val end = ms(2026, 11, 23, 10, 30)
        val events = listOf(
            DomainUsageEvent(ms(2026, 11, 23, 9, 50), NormalizedEventType.KEYGUARD_HIDDEN),
            DomainUsageEvent(ms(2026, 11, 23, 9, 52), NormalizedEventType.SCREEN_NON_INTERACTIVE)
        )
        val windows = FeatureWindowCalculator.computeWindows(events, start, end, end, istanbul)
        assertEquals(120.0, windows[0].features.previousSessionSeconds, tolerance)
    }

    @Test
    fun f06_sessionEndingAfterWindowStartIsNotPrevSessionForCurrentWindow() {
        val start = ms(2026, 11, 23, 10, 0)
        val end = ms(2026, 11, 23, 11, 0)
        val events = listOf(
            DomainUsageEvent(ms(2026, 11, 23, 9, 50), NormalizedEventType.KEYGUARD_HIDDEN),
            DomainUsageEvent(ms(2026, 11, 23, 10, 5), NormalizedEventType.SCREEN_NON_INTERACTIVE)
        )
        val windows = FeatureWindowCalculator.computeWindows(events, start, end, end, istanbul)
        assertEquals(0.0, windows[0].features.previousSessionSeconds, tolerance)
        assertEquals(900.0, windows[1].features.previousSessionSeconds, tolerance)
    }

    @Test
    fun f07_t11_appSwitchesFiltersKeyboardLauncherAndSameAppRepeats() {
        val wStart = ms(2026, 11, 23, 10, 0)
        val wEnd = ms(2026, 11, 23, 10, 30)
        val events = listOf(
            DomainUsageEvent(ms(2026, 11, 23, 10, 1), NormalizedEventType.APP_FOREGROUND, "pkg_A", false),
            DomainUsageEvent(ms(2026, 11, 23, 10, 2), NormalizedEventType.APP_FOREGROUND, "pkg_A", false),
            DomainUsageEvent(ms(2026, 11, 23, 10, 3), NormalizedEventType.APP_FOREGROUND, "pkg_kbd", true),
            DomainUsageEvent(ms(2026, 11, 23, 10, 4), NormalizedEventType.APP_FOREGROUND, "pkg_B", false),
            DomainUsageEvent(ms(2026, 11, 23, 10, 5), NormalizedEventType.APP_FOREGROUND, "pkg_lnch", true),
            DomainUsageEvent(ms(2026, 11, 23, 10, 6), NormalizedEventType.APP_FOREGROUND, "pkg_B", false),
            DomainUsageEvent(ms(2026, 11, 23, 10, 7), NormalizedEventType.APP_FOREGROUND, "pkg_A", false)
        )
        val windowsUnknownPrior = FeatureWindowCalculator.computeWindows(events, wStart, wEnd, wEnd, istanbul)
        assertEquals(2, windowsUnknownPrior[0].features.appSwitches)

        val windowsWithPriorB = FeatureWindowCalculator.computeWindows(
            events = events,
            rangeStartMs = wStart,
            rangeEndMs = wEnd,
            knownCutoffMs = wEnd,
            zoneId = istanbul,
            initialPreviousPackageToken = "pkg_B"
        )
        assertEquals(3, windowsWithPriorB[0].features.appSwitches)
    }

    @Test
    fun f08_t13_openSessionCausalityNoFutureLeakage() {
        val wStart = ms(2026, 11, 23, 10, 0)
        val wEnd = ms(2026, 11, 23, 10, 30)
        val fullTimeline = listOf(
            DomainUsageEvent(ms(2026, 11, 23, 10, 20), NormalizedEventType.KEYGUARD_HIDDEN),
            DomainUsageEvent(ms(2026, 11, 23, 10, 45), NormalizedEventType.SCREEN_NON_INTERACTIVE)
        )
        val truncatedTimeline = fullTimeline.filter { it.timestampMs < wEnd }

        val fromFull = FeatureWindowCalculator.computeWindows(
            events = fullTimeline,
            rangeStartMs = wStart,
            rangeEndMs = wEnd,
            knownCutoffMs = ms(2026, 11, 23, 11, 0),
            zoneId = istanbul
        )
        val fromTruncated = FeatureWindowCalculator.computeWindows(
            events = truncatedTimeline,
            rangeStartMs = wStart,
            rangeEndMs = wEnd,
            knownCutoffMs = wEnd,
            zoneId = istanbul
        )

        assertEquals(fromFull[0].features, fromTruncated[0].features)
        assertEquals(600.0, fromTruncated[0].features.screenSeconds, tolerance)
    }

    @Test
    fun f09_sessionOverThreeHoursIsCappedAtFirstThreeHours() {
        val start = ms(2026, 11, 23, 10, 0)
        val end = ms(2026, 11, 23, 15, 0)
        val events = listOf(
            DomainUsageEvent(ms(2026, 11, 23, 10, 0), NormalizedEventType.KEYGUARD_HIDDEN),
            DomainUsageEvent(ms(2026, 11, 23, 14, 0), NormalizedEventType.SCREEN_NON_INTERACTIVE)
        )
        val windows = FeatureWindowCalculator.computeWindows(events, start, end, end, istanbul)
        assertEquals(10, windows.size)
        for (i in 0..5) {
            assertEquals(1800.0, windows[i].features.screenSeconds, tolerance)
        }
        assertEquals(0.0, windows[6].features.screenSeconds, tolerance)
        assertEquals(0.0, windows[7].features.screenSeconds, tolerance)
        assertEquals(10800.0, windows[8].features.previousSessionSeconds, tolerance)
    }

    @Test
    fun f10_t10_t12_apiErrorMissingStartAndIncompleteWindowQualityFlags() {
        val start = ms(2026, 11, 23, 10, 0)
        val end = ms(2026, 11, 23, 11, 0)
        val events = listOf(
            DomainUsageEvent(ms(2026, 11, 23, 10, 10), NormalizedEventType.SCREEN_NON_INTERACTIVE)
        )
        val issues = listOf(
            QualityIssueInterval(
                startMs = ms(2026, 11, 23, 10, 30),
                endMs = ms(2026, 11, 23, 11, 0),
                reason = WindowQualityReason.QUERY_ERROR
            )
        )
        val windows = FeatureWindowCalculator.computeWindows(
            events = events,
            rangeStartMs = start,
            rangeEndMs = end,
            knownCutoffMs = end,
            zoneId = istanbul,
            qualityIssues = issues
        )
        assertEquals(2, windows.size)
        assertEquals(WindowQualityStatus.UNCERTAIN, windows[0].qualityStatus)
        assertTrue(WindowQualityReason.MISSING_SESSION_START in windows[0].qualityReasons)
        assertEquals(WindowQualityStatus.INVALID, windows[1].qualityStatus)
        assertTrue(WindowQualityReason.QUERY_ERROR in windows[1].qualityReasons)

        val partialWindows = FeatureWindowCalculator.computeWindows(
            events = events,
            rangeStartMs = start,
            rangeEndMs = ms(2026, 11, 23, 10, 30),
            knownCutoffMs = ms(2026, 11, 23, 10, 15),
            zoneId = istanbul
        )
        assertEquals(0, partialWindows.size)
    }

    @Test
    fun f11_midnightAndWeekStartRolloverInLocalTimezone() {
        val start = ms(2026, 11, 22, 23, 30)
        val end = ms(2026, 11, 23, 0, 30)
        val windows = FeatureWindowCalculator.computeWindows(emptyList(), start, end, end, istanbul)
        assertEquals(2, windows.size)

        assertEquals(23, windows[0].features.hour)
        assertEquals(6, windows[0].features.dayOfWeek)
        assertEquals(10800, windows[0].offsetSeconds)
        assertEquals("Europe/Istanbul", windows[0].timezoneId)

        assertEquals(0, windows[1].features.hour)
        assertEquals(0, windows[1].features.dayOfWeek)
    }

    @Test
    fun f12_duplicateImportProducesIdenticalCountsAndFeatures() {
        val wStart = ms(2026, 11, 23, 10, 0)
        val wEnd = ms(2026, 11, 23, 10, 30)
        val batch = listOf(
            NormalizedUsageEvent(
                timestampMs = ms(2026, 11, 23, 10, 5),
                eventType = NormalizedEventType.KEYGUARD_HIDDEN,
                packageName = null,
                packageToken = null,
                className = null,
                isNonAppPackage = false,
                sourceEventCode = UsageEventNormalizer.EVENT_KEYGUARD_HIDDEN,
                sourceVersion = UsageEventNormalizer.SOURCE_VERSION
            ),
            NormalizedUsageEvent(
                timestampMs = ms(2026, 11, 23, 10, 15),
                eventType = NormalizedEventType.SCREEN_NON_INTERACTIVE,
                packageName = null,
                packageToken = null,
                className = null,
                isNonAppPackage = false,
                sourceEventCode = UsageEventNormalizer.EVENT_SCREEN_NON_INTERACTIVE,
                sourceVersion = UsageEventNormalizer.SOURCE_VERSION
            )
        )
        val deduplicated = (batch + batch)
            .associateBy { EventFingerprint.compute(it) }
            .values
            .map { DomainUsageEvent(it.timestampMs, it.eventType, it.packageToken, it.isNonAppPackage) }

        assertEquals(2, deduplicated.size)
        val windows = FeatureWindowCalculator.computeWindows(deduplicated, wStart, wEnd, wEnd, istanbul)
        assertEquals(1, windows.size)
        assertEquals(600.0, windows[0].features.screenSeconds, tolerance)
        assertEquals(1, windows[0].features.sessionsStarted)
    }
}
