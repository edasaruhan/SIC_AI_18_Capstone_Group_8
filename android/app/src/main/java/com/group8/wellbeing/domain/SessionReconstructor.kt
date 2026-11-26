package com.group8.wellbeing.domain

import com.group8.wellbeing.data.AppConfig
import com.group8.wellbeing.usage.NormalizedEventType

data class DomainUsageEvent(
    val timestampMs: Long,
    val eventType: NormalizedEventType,
    val packageToken: String? = null,
    val isNonAppPackage: Boolean = false
)

enum class SessionMachineMode {
    CLOSED,
    OPEN,
    UNKNOWN_START
}

data class UsageSession(
    val startMs: Long,
    val endMs: Long,
    val cappedEndMs: Long,
    val isOpenAtCutoff: Boolean,
    val closedByContinuityBreak: Boolean
) {
    val rawDurationSeconds: Double
        get() = (endMs - startMs) / 1000.0

    val cappedDurationSeconds: Double
        get() = (cappedEndMs - startMs) / 1000.0
}

data class SessionReconstructionResult(
    val sessions: List<UsageSession>,
    val duplicateUnlockCount: Int,
    val glanceTimestampsMs: List<Long>,
    val missingStartOffTimestampsMs: List<Long>,
    val continuityBreakTimestampsMs: List<Long>,
    val clockJumpTimestampsMs: List<Long>,
    val finalMode: SessionMachineMode
)

object SessionReconstructor {
    fun reconstruct(
        events: List<DomainUsageEvent>,
        cutoffMs: Long? = null,
        maxSessionDurationMs: Long = AppConfig.MAX_SESSION_DURATION_MS,
        continuityBreaksMs: List<Long> = emptyList(),
        initialMode: SessionMachineMode = SessionMachineMode.CLOSED,
        initialOpenStartMs: Long? = null
    ): SessionReconstructionResult {
        val sortedBreaks = continuityBreaksMs.sorted()
        var breakIndex = 0

        val clockJumps = mutableListOf<Long>()
        var previousTimestampMs: Long? = null
        events.forEach { event ->
            val prev = previousTimestampMs
            if (prev != null && event.timestampMs < prev) {
                clockJumps.add(event.timestampMs)
            }
            previousTimestampMs = event.timestampMs
        }

        val orderedEvents = events
            .asSequence()
            .filter { event -> cutoffMs == null || event.timestampMs < cutoffMs }
            .sortedBy { it.timestampMs }
            .toList()

        val sessions = mutableListOf<UsageSession>()
        val glanceTimestamps = mutableListOf<Long>()
        val missingStartOffs = mutableListOf<Long>()
        val triggeredBreaks = mutableListOf<Long>()
        var duplicateUnlocks = 0
        var sawInteractiveWhileClosed = false

        var mode = initialMode
        var currentStartMs: Long? = if (initialMode == SessionMachineMode.OPEN) {
            initialOpenStartMs
        } else {
            null
        }

        fun applyBreaksUpTo(limitMs: Long) {
            while (breakIndex < sortedBreaks.size && sortedBreaks[breakIndex] <= limitMs) {
                val breakMs = sortedBreaks[breakIndex]
                breakIndex += 1
                sawInteractiveWhileClosed = false
                if (mode == SessionMachineMode.OPEN && currentStartMs != null && breakMs >= currentStartMs!!) {
                    val start = currentStartMs!!
                    val cappedEnd = minOf(breakMs, start + maxSessionDurationMs)
                    sessions.add(
                        UsageSession(
                            startMs = start,
                            endMs = breakMs,
                            cappedEndMs = cappedEnd,
                            isOpenAtCutoff = false,
                            closedByContinuityBreak = true
                        )
                    )
                    triggeredBreaks.add(breakMs)
                    mode = SessionMachineMode.CLOSED
                    currentStartMs = null
                } else if (mode == SessionMachineMode.UNKNOWN_START) {
                    triggeredBreaks.add(breakMs)
                    mode = SessionMachineMode.CLOSED
                    currentStartMs = null
                }
            }
        }

        for (event in orderedEvents) {
            applyBreaksUpTo(event.timestampMs)
            when (event.eventType) {
                NormalizedEventType.SCREEN_INTERACTIVE -> {
                    if (mode == SessionMachineMode.CLOSED) {
                        sawInteractiveWhileClosed = true
                    }
                }
                NormalizedEventType.KEYGUARD_HIDDEN -> {
                    sawInteractiveWhileClosed = false
                    when (mode) {
                        SessionMachineMode.CLOSED -> {
                            mode = SessionMachineMode.OPEN
                            currentStartMs = event.timestampMs
                        }
                        SessionMachineMode.OPEN -> {
                            duplicateUnlocks += 1
                        }
                        SessionMachineMode.UNKNOWN_START -> {
                            mode = SessionMachineMode.OPEN
                            currentStartMs = event.timestampMs
                        }
                    }
                }
                NormalizedEventType.SCREEN_NON_INTERACTIVE -> {
                    when (mode) {
                        SessionMachineMode.OPEN -> {
                            val start = currentStartMs
                            if (start != null && event.timestampMs >= start) {
                                val cappedEnd = minOf(event.timestampMs, start + maxSessionDurationMs)
                                sessions.add(
                                    UsageSession(
                                        startMs = start,
                                        endMs = event.timestampMs,
                                        cappedEndMs = cappedEnd,
                                        isOpenAtCutoff = false,
                                        closedByContinuityBreak = false
                                    )
                                )
                            }
                            mode = SessionMachineMode.CLOSED
                            currentStartMs = null
                            sawInteractiveWhileClosed = false
                        }
                        SessionMachineMode.UNKNOWN_START -> {
                            missingStartOffs.add(event.timestampMs)
                            mode = SessionMachineMode.CLOSED
                            currentStartMs = null
                            sawInteractiveWhileClosed = false
                        }
                        SessionMachineMode.CLOSED -> {
                            if (sawInteractiveWhileClosed) {
                                glanceTimestamps.add(event.timestampMs)
                                sawInteractiveWhileClosed = false
                            } else {
                                missingStartOffs.add(event.timestampMs)
                            }
                        }
                    }
                }
                else -> Unit
            }
        }

        if (cutoffMs != null) {
            applyBreaksUpTo(cutoffMs)
            if (mode == SessionMachineMode.OPEN && currentStartMs != null) {
                val start = currentStartMs!!
                val end = maxOf(start, cutoffMs)
                val cappedEnd = minOf(end, start + maxSessionDurationMs)
                sessions.add(
                    UsageSession(
                        startMs = start,
                        endMs = end,
                        cappedEndMs = cappedEnd,
                        isOpenAtCutoff = true,
                        closedByContinuityBreak = false
                    )
                )
            }
        }

        return SessionReconstructionResult(
            sessions = sessions,
            duplicateUnlockCount = duplicateUnlocks,
            glanceTimestampsMs = glanceTimestamps,
            missingStartOffTimestampsMs = missingStartOffs,
            continuityBreakTimestampsMs = triggeredBreaks,
            clockJumpTimestampsMs = clockJumps,
            finalMode = mode
        )
    }
}
