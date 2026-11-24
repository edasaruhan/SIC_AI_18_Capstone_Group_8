package com.group8.wellbeing.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.group8.wellbeing.data.RoomWellbeingRepository
import com.group8.wellbeing.data.SharedPrefsControlStore
import com.group8.wellbeing.data.local.WellbeingDatabase
import com.group8.wellbeing.domain.CollectionStateResolver
import com.group8.wellbeing.usage.AndroidUsageEventReader
import com.group8.wellbeing.usage.UsageAccessChecker

class UsageCollectionWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val controlStore = SharedPrefsControlStore(applicationContext)
        val snapshot = controlStore.read()
        val usagePermission = UsageAccessChecker.check(applicationContext)
        val resolved = CollectionStateResolver.resolve(
            consentAccepted = snapshot.isConsentAccepted,
            usagePermission = usagePermission,
            hadUsagePermissionPreviously = snapshot.hadUsagePermissionPreviously,
            pausedByUser = snapshot.pausedByUser,
            errorMessage = null
        )
        if (!resolved.canQueryUsageData) {
            return Result.success()
        }

        val repository = RoomWellbeingRepository(WellbeingDatabase.getInstance(applicationContext))
        val stats = repository.readStorageStats(snapshot.participantId)
        val nowMs = System.currentTimeMillis()
        val planned = BackgroundQueryWindowPlanner.plan(
            nowMs = nowMs,
            lastCheckpointMs = stats.lastCheckpointMs,
            resumedAtMs = snapshot.resumedAtMs,
            backfillPausedPeriod = snapshot.backfillPausedPeriod
        )
        if (planned.endMs <= planned.startMs) {
            return Result.success()
        }

        val reader = AndroidUsageEventReader(applicationContext)
        val batch = reader.queryEvents(
            startMs = planned.startMs,
            endMs = planned.endMs,
            collectedAtMs = nowMs
        )
        repository.persistCollectionBatch(
            participantId = snapshot.participantId,
            consentVersion = snapshot.consentVersion,
            consentTimeMs = snapshot.consentAcceptedAtMs,
            collectionEnabled = true,
            batch = batch
        )
        return Result.success()
    }
}
