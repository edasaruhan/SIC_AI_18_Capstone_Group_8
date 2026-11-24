package com.group8.wellbeing.domain

import com.group8.wellbeing.data.AppConfig
import com.group8.wellbeing.inference.FeatureVector
import com.group8.wellbeing.usage.NormalizedEventType
import java.time.Instant
import java.time.ZoneId

data class QualityIssueInterval(
    val startMs: Long,
    val endMs: Long,
    val reason: WindowQualityReason
)

data class ComputedFeatureWindow(
    val startMs: Long,
    val endMs: Long,
    val timezoneId: String,
    val offsetSeconds: Int,
    val features: FeatureVector,
    val qualityStatus: WindowQualityStatus,
    val qualityReasons: Set<WindowQualityReason>,
    val schemaVersion: String
)

object FeatureWindowCalculator {
    private val invalidReasons = setOf(
        WindowQualityReason.PERMISSION_DISABLED,
        WindowQualityReason.QUERY_ERROR,
        WindowQualityReason.OUT_OF_RETENTION,
        WindowQualityReason.INCOMPLETE_WINDOW
    )

    private val uncertainReasons = setOf(
        WindowQualityReason.REBOOT_GAP,
        WindowQualityReason.CONSENT_OR_PAUSE_BOUNDARY,
        WindowQualityReason.CLOCK_JUMP,
        WindowQualityReason.MISSING_SESSION_START
    )

    fun alignFloor(timestampMs: Long, windowDurationMs: Long = AppConfig.WINDOW_DURATION_MS): Long {
        return Math.floorDiv(timestampMs, windowDurationMs) * windowDurationMs
    }

    fun computeWindows(
        events: List<DomainUsageEvent>,
        rangeStartMs: Long,
        rangeEndMs: Long,
        knownCutoffMs: Long,
        zoneId: ZoneId,
        qualityIssues: List<QualityIssueInterval> = emptyList(),
        continuityBreaksMs: List<Long> = emptyList(),
        initialSessionMode: SessionMachineMode = SessionMachineMode.CLOSED,
        initialOpenStartMs: Long? = null,
        initialPreviousPackageToken: String? = null
    ): List<ComputedFeatureWindow> {
        val windowDurationMs = AppConfig.WINDOW_DURATION_MS
        val firstStartMs = alignFloor(rangeStartMs, windowDurationMs)
        val effectiveEndMs = minOf(rangeEndMs, knownCutoffMs)
        if (effectiveEndMs < firstStartMs + windowDurationMs) {
            return emptyList()
        }

        val reconstruction = SessionReconstructor.reconstruct(
            events = events,
            cutoffMs = knownCutoffMs,
            maxSessionDurationMs = AppConfig.MAX_SESSION_DURATION_MS,
            continuityBreaksMs = continuityBreaksMs,
            initialMode = initialSessionMode,
            initialOpenStartMs = initialOpenStartMs
        )

        val validAppEvents = events
            .asSequence()
            .filter { event ->
                event.timestampMs < knownCutoffMs &&
                    event.eventType == NormalizedEventType.APP_FOREGROUND &&
                    !event.isNonAppPackage &&
                    !event.packageToken.isNullOrBlank()
            }
            .sortedBy { it.timestampMs }
            .toList()

        val completedSessionsByEnd = reconstruction.sessions
            .filter { session -> !session.isOpenAtCutoff && session.endMs >= session.startMs }
            .sortedBy { it.endMs }

        val windows = mutableListOf<ComputedFeatureWindow>()
        var windowStartMs = firstStartMs

        var appIndex = 0
        var lastValidPackage: String? = initialPreviousPackageToken
        while (appIndex < validAppEvents.size && validAppEvents[appIndex].timestampMs < windowStartMs) {
            lastValidPackage = validAppEvents[appIndex].packageToken
            appIndex += 1
        }

        while (windowStartMs + windowDurationMs <= effectiveEndMs) {
            val windowEndMs = windowStartMs + windowDurationMs

            var screenSeconds = 0.0
            var longestRunSeconds = 0.0
            var sessionsStarted = 0

            for (session in reconstruction.sessions) {
                if (session.startMs >= windowStartMs && session.startMs < windowEndMs) {
                    sessionsStarted += 1
                }
                val overlapStart = maxOf(windowStartMs, session.startMs)
                val overlapEnd = minOf(windowEndMs, session.cappedEndMs)
                if (overlapEnd > overlapStart) {
                    val overlapSec = (overlapEnd - overlapStart) / 1000.0
                    screenSeconds += overlapSec
                    if (overlapSec > longestRunSeconds) {
                        longestRunSeconds = overlapSec
                    }
                }
            }
            screenSeconds = minOf(screenSeconds, windowDurationMs / 1000.0)
            longestRunSeconds = minOf(longestRunSeconds, screenSeconds)

            var appSwitches = 0
            while (appIndex < validAppEvents.size && validAppEvents[appIndex].timestampMs < windowEndMs) {
                val token = validAppEvents[appIndex].packageToken
                if (lastValidPackage != null && token != lastValidPackage) {
                    appSwitches += 1
                }
                lastValidPackage = token
                appIndex += 1
            }

            var prevSessionSeconds = 0.0
            for (session in completedSessionsByEnd) {
                if (session.endMs <= windowStartMs) {
                    prevSessionSeconds = session.cappedDurationSeconds
                } else {
                    break
                }
            }

            val zonedStart = Instant.ofEpochMilli(windowStartMs).atZone(zoneId)
            val hour = zonedStart.hour
            val dow = zonedStart.dayOfWeek.value - 1

            val reasons = linkedSetOf<WindowQualityReason>()
            for (issue in qualityIssues) {
                if (issue.startMs < windowEndMs && issue.endMs > windowStartMs) {
                    reasons.add(issue.reason)
                }
            }
            if (reconstruction.missingStartOffTimestampsMs.any { ts -> ts in windowStartMs until windowEndMs }) {
                reasons.add(WindowQualityReason.MISSING_SESSION_START)
            }
            if (reconstruction.clockJumpTimestampsMs.any { ts -> ts in windowStartMs until windowEndMs }) {
                reasons.add(WindowQualityReason.CLOCK_JUMP)
            }

            val qualityStatus = when {
                reasons.any { it in invalidReasons } -> WindowQualityStatus.INVALID
                reasons.any { it in uncertainReasons } -> WindowQualityStatus.UNCERTAIN
                else -> WindowQualityStatus.OBSERVATION_SUFFICIENT
            }

            windows.add(
                ComputedFeatureWindow(
                    startMs = windowStartMs,
                    endMs = windowEndMs,
                    timezoneId = zoneId.id,
                    offsetSeconds = zonedStart.offset.totalSeconds,
                    features = FeatureVector(
                        screenSeconds = screenSeconds,
                        sessionsStarted = sessionsStarted,
                        longestRunSeconds = longestRunSeconds,
                        appSwitches = appSwitches,
                        previousSessionSeconds = prevSessionSeconds,
                        hour = hour,
                        dayOfWeek = dow,
                        schemaVersion = AppConfig.FEATURE_SCHEMA_VERSION
                    ),
                    qualityStatus = qualityStatus,
                    qualityReasons = reasons,
                    schemaVersion = AppConfig.FEATURE_SCHEMA_VERSION
                )
            )
            windowStartMs = windowEndMs
        }

        return windows
    }
}
