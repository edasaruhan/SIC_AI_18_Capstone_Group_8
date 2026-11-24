package com.group8.wellbeing.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.UserManager

interface UsageEventReader {
    fun queryEvents(startMs: Long, endMs: Long, collectedAtMs: Long): UsageQueryBatchResult
}

class AndroidUsageEventReader(context: Context) : UsageEventReader {
    private val appContext = context.applicationContext

    override fun queryEvents(startMs: Long, endMs: Long, collectedAtMs: Long): UsageQueryBatchResult {
        val permissionState = UsageAccessChecker.check(appContext)
        if (permissionState != UsagePermissionState.GRANTED) {
            return UsageQueryBatchResult(
                queriedStartMs = startMs,
                queriedEndMs = endMs,
                collectedAtMs = collectedAtMs,
                permissionState = permissionState,
                events = emptyList(),
                rawScannedCount = 0,
                errorCode = UsageQueryErrorCode.PERMISSION_REQUIRED
            )
        }

        val userManager = appContext.getSystemService(Context.USER_SERVICE) as? UserManager
        if (userManager != null && !userManager.isUserUnlocked) {
            return UsageQueryBatchResult(
                queriedStartMs = startMs,
                queriedEndMs = endMs,
                collectedAtMs = collectedAtMs,
                permissionState = permissionState,
                events = emptyList(),
                rawScannedCount = 0,
                errorCode = UsageQueryErrorCode.USER_LOCKED_AFTER_REBOOT
            )
        }

        val usageStatsManager =
            appContext.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return UsageQueryBatchResult(
                    queriedStartMs = startMs,
                    queriedEndMs = endMs,
                    collectedAtMs = collectedAtMs,
                    permissionState = permissionState,
                    events = emptyList(),
                    rawScannedCount = 0,
                    errorCode = UsageQueryErrorCode.UNEXPECTED_ERROR,
                    errorDetail = "UsageStatsManager servisi alınamadı."
                )

        return try {
            val usageEvents = usageStatsManager.queryEvents(startMs, endMs)
            if (usageEvents == null) {
                return UsageQueryBatchResult(
                    queriedStartMs = startMs,
                    queriedEndMs = endMs,
                    collectedAtMs = collectedAtMs,
                    permissionState = permissionState,
                    events = emptyList(),
                    rawScannedCount = 0,
                    errorCode = UsageQueryErrorCode.NULL_EVENTS
                )
            }
            val rawList = mutableListOf<RawSystemEvent>()
            val reusableEvent = UsageEvents.Event()
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(reusableEvent)
                rawList.add(
                    RawSystemEvent(
                        timestampMs = reusableEvent.timeStamp,
                        androidEventType = reusableEvent.eventType,
                        packageName = reusableEvent.packageName,
                        className = reusableEvent.className
                    )
                )
            }
            val normalized = UsageEventNormalizer.normalize(
                rawEvents = rawList,
                startMs = startMs,
                endMs = endMs
            )
            UsageQueryBatchResult(
                queriedStartMs = startMs,
                queriedEndMs = endMs,
                collectedAtMs = collectedAtMs,
                permissionState = permissionState,
                events = normalized,
                rawScannedCount = rawList.size,
                errorCode = UsageQueryErrorCode.NONE
            )
        } catch (exception: SecurityException) {
            UsageQueryBatchResult(
                queriedStartMs = startMs,
                queriedEndMs = endMs,
                collectedAtMs = collectedAtMs,
                permissionState = UsagePermissionState.REQUIRED,
                events = emptyList(),
                rawScannedCount = 0,
                errorCode = UsageQueryErrorCode.SECURITY_EXCEPTION,
                errorDetail = exception.message
            )
        } catch (exception: Exception) {
            UsageQueryBatchResult(
                queriedStartMs = startMs,
                queriedEndMs = endMs,
                collectedAtMs = collectedAtMs,
                permissionState = permissionState,
                events = emptyList(),
                rawScannedCount = 0,
                errorCode = UsageQueryErrorCode.UNEXPECTED_ERROR,
                errorDetail = exception.message
            )
        }
    }
}
