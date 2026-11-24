package com.group8.wellbeing.data

import androidx.room.withTransaction
import com.group8.wellbeing.data.local.AppSettingsEntity
import com.group8.wellbeing.data.local.CollectionBatchEntity
import com.group8.wellbeing.data.local.RawUsageEventEntity
import com.group8.wellbeing.data.local.WellbeingDatabase
import com.group8.wellbeing.usage.UsageQueryBatchResult
import com.group8.wellbeing.usage.UsageQueryErrorCode

data class StorageStats(
    val storedRawEventCount: Int = 0,
    val storedBatchCount: Int = 0,
    val lastCheckpointMs: Long? = null
)

data class BatchPersistenceSummary(
    val insertedEventCount: Int,
    val duplicateSkippedCount: Int,
    val stats: StorageStats
)

interface LocalDataRepository {
    suspend fun readStorageStats(participantId: String): StorageStats
    suspend fun persistCollectionBatch(
        participantId: String,
        consentVersion: String?,
        consentTimeMs: Long?,
        collectionEnabled: Boolean,
        batch: UsageQueryBatchResult
    ): BatchPersistenceSummary
    suspend fun clearAllLocalData()
}

class RoomWellbeingRepository(
    private val database: WellbeingDatabase
) : LocalDataRepository {
    private val dao = database.wellbeingDao()

    override suspend fun readStorageStats(participantId: String): StorageStats {
        val rawCount = dao.countRawEvents(participantId)
        val batchCount = dao.countCollectionBatches(participantId)
        val settings = dao.getSettings()
        return StorageStats(
            storedRawEventCount = rawCount,
            storedBatchCount = batchCount,
            lastCheckpointMs = settings?.lastCheckpointMs
        )
    }

    override suspend fun persistCollectionBatch(
        participantId: String,
        consentVersion: String?,
        consentTimeMs: Long?,
        collectionEnabled: Boolean,
        batch: UsageQueryBatchResult
    ): BatchPersistenceSummary {
        return database.withTransaction {
            val entities = batch.events.map { event ->
                RawUsageEventEntity(
                    participantId = participantId,
                    timestampMs = event.timestampMs,
                    eventType = event.eventType.name,
                    packageToken = event.packageToken,
                    isNonAppPackage = event.isNonAppPackage,
                    sourceEventCode = event.sourceEventCode,
                    sourceVersion = event.sourceVersion,
                    eventFingerprint = EventFingerprint.compute(event)
                )
            }
            val rowIds = if (entities.isNotEmpty()) {
                dao.insertRawEvents(entities)
            } else {
                emptyList()
            }
            val insertedCount = rowIds.count { id -> id != -1L }
            val duplicateSkippedCount = entities.size - insertedCount

            dao.insertCollectionBatch(
                CollectionBatchEntity(
                    participantId = participantId,
                    queriedStartMs = batch.queriedStartMs,
                    queriedEndMs = batch.queriedEndMs,
                    collectedAtMs = batch.collectedAtMs,
                    permissionState = batch.permissionState.name,
                    rawScannedCount = batch.rawScannedCount,
                    resultCount = batch.events.size,
                    insertedCount = insertedCount,
                    errorCode = batch.errorCode.name,
                    errorDetail = batch.errorDetail
                )
            )

            val existingSettings = dao.getSettings()
            val nextCheckpoint = if (batch.errorCode == UsageQueryErrorCode.NONE) {
                maxOf(existingSettings?.lastCheckpointMs ?: 0L, batch.queriedEndMs)
            } else {
                existingSettings?.lastCheckpointMs
            }

            dao.upsertSettings(
                AppSettingsEntity(
                    id = 1,
                    participantId = participantId,
                    collectionEnabled = collectionEnabled,
                    remindersEnabled = existingSettings?.remindersEnabled ?: false,
                    quietStartHour = existingSettings?.quietStartHour ?: AppConfig.QUIET_START_HOUR,
                    quietEndHour = existingSettings?.quietEndHour ?: AppConfig.QUIET_END_HOUR,
                    dailyCap = existingSettings?.dailyCap ?: AppConfig.DAILY_REMINDER_CAP,
                    consentVersion = consentVersion,
                    consentTimeMs = consentTimeMs,
                    lastCheckpointMs = nextCheckpoint,
                    schemaVersion = AppConfig.APP_SCHEMA_VERSION
                )
            )

            val rawRetentionCutoffMs =
                batch.collectedAtMs - AppConfig.RAW_EVENT_RETENTION_DAYS * 86_400_000L
            dao.deleteRawEventsBefore(rawRetentionCutoffMs)

            val totalRaw = dao.countRawEvents(participantId)
            val totalBatches = dao.countCollectionBatches(participantId)
            BatchPersistenceSummary(
                insertedEventCount = insertedCount,
                duplicateSkippedCount = duplicateSkippedCount,
                stats = StorageStats(
                    storedRawEventCount = totalRaw,
                    storedBatchCount = totalBatches,
                    lastCheckpointMs = nextCheckpoint
                )
            )
        }
    }

    override suspend fun clearAllLocalData() {
        database.withTransaction {
            dao.clearRawEvents()
            dao.clearCollectionBatches()
            dao.clearFeatureWindows()
            dao.clearPredictions()
            dao.clearReminderEvents()
            dao.clearFeedbacks()
            dao.clearSettings()
        }
    }
}
