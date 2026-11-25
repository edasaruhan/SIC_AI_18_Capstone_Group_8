package com.group8.wellbeing.worker

data class PlannedQueryWindow(
    val startMs: Long,
    val endMs: Long,
    val isCatchUp: Boolean
)

object BackgroundQueryWindowPlanner {
    private const val SAFE_OVERLAP_MS: Long = 300_000L
    private const val DEFAULT_INITIAL_LOOKBACK_MS: Long = 7_200_000L
    private const val MAX_CATCHUP_LOOKBACK_MS: Long = 86_400_000L
    private const val CATCHUP_THRESHOLD_MS: Long = 2_700_000L

    fun plan(
        nowMs: Long,
        lastCheckpointMs: Long?,
        resumedAtMs: Long?,
        backfillPausedPeriod: Boolean
    ): PlannedQueryWindow {
        val earliestAllowedMs = maxOf(0L, nowMs - MAX_CATCHUP_LOOKBACK_MS)
        val rawStartMs = when {
            resumedAtMs != null &&
                !backfillPausedPeriod &&
                (lastCheckpointMs == null || resumedAtMs > lastCheckpointMs) -> {
                resumedAtMs
            }
            lastCheckpointMs != null -> {
                maxOf(0L, lastCheckpointMs - SAFE_OVERLAP_MS)
            }
            else -> {
                maxOf(0L, nowMs - DEFAULT_INITIAL_LOOKBACK_MS)
            }
        }
        val clampedStartMs = minOf(nowMs, maxOf(earliestAllowedMs, rawStartMs))
        val isCatchUp = (nowMs - clampedStartMs) >= CATCHUP_THRESHOLD_MS
        return PlannedQueryWindow(
            startMs = clampedStartMs,
            endMs = nowMs,
            isCatchUp = isCatchUp
        )
    }
}
