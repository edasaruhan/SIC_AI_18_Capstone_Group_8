package com.group8.wellbeing.data

import androidx.room.withTransaction
import com.group8.wellbeing.data.local.AppSettingsEntity
import com.group8.wellbeing.data.local.CollectionBatchEntity
import com.group8.wellbeing.data.local.FeatureWindowEntity
import com.group8.wellbeing.data.local.FeedbackEntity
import com.group8.wellbeing.data.local.PredictionEntity
import com.group8.wellbeing.data.local.RawUsageEventEntity
import com.group8.wellbeing.data.local.WellbeingDatabase
import com.group8.wellbeing.domain.DomainUsageEvent
import com.group8.wellbeing.domain.FeatureWindowCalculator
import com.group8.wellbeing.domain.FeedbackAnswer
import com.group8.wellbeing.domain.FeedbackRecord
import com.group8.wellbeing.domain.FeedbackResponseStatus
import com.group8.wellbeing.domain.QualityIssueInterval
import com.group8.wellbeing.domain.WindowQualityReason
import com.group8.wellbeing.inference.DisabledPredictor
import com.group8.wellbeing.inference.PredictionOutcome
import com.group8.wellbeing.inference.PredictionRunner
import com.group8.wellbeing.inference.Predictor
import com.group8.wellbeing.reminders.ReminderDelivery
import com.group8.wellbeing.usage.NormalizedEventType
import com.group8.wellbeing.usage.UsageQueryBatchResult
import com.group8.wellbeing.usage.UsageQueryErrorCode
import java.time.ZoneId

data class StorageStats(
    val storedRawEventCount: Int = 0,
    val storedBatchCount: Int = 0,
    val storedWindowCount: Int = 0,
    val storedFeedbackCount: Int = 0,
    val lastCheckpointMs: Long? = null
)

data class BatchPersistenceSummary(
    val insertedEventCount: Int,
    val duplicateSkippedCount: Int,
    val computedWindowCount: Int,
    val stats: StorageStats
)

data class PredictionStatus(
    val modelVersion: String? = null,
    val latest: PredictionEntity? = null
)

interface LocalDataRepository {
    suspend fun readStorageStats(participantId: String): StorageStats
    suspend fun readFeatureWindows(participantId: String): List<FeatureWindowEntity>
    suspend fun readFeedbacks(participantId: String): List<FeedbackEntity>
    suspend fun readPredictionStatus(participantId: String): PredictionStatus = PredictionStatus()
    suspend fun persistCollectionBatch(
        participantId: String,
        consentVersion: String?,
        consentTimeMs: Long?,
        collectionEnabled: Boolean,
        batch: UsageQueryBatchResult,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): BatchPersistenceSummary
    suspend fun saveFeedback(
        participantId: String,
        windowId: Long?,
        predictionId: Long?,
        windowStartMs: Long,
        windowEndMs: Long,
        answer: FeedbackAnswer?,
        responseStatus: FeedbackResponseStatus,
        nowMs: Long
    ): FeedbackEntity
    suspend fun buildExportPayload(
        participantId: String,
        appVersion: String,
        nowMs: Long
    ): ExportPayload
    suspend fun clearAllLocalData()
}

class RoomWellbeingRepository(
    private val database: WellbeingDatabase,
    private val predictorProvider: () -> Predictor = { DisabledPredictor },
    private val nowMsProvider: () -> Long = System::currentTimeMillis,
    private val reminderDelivery: ReminderDelivery? = null
) : LocalDataRepository {
    private val dao = database.wellbeingDao()

    override suspend fun readStorageStats(participantId: String): StorageStats {
        val rawCount = dao.countRawEvents(participantId)
        val batchCount = dao.countCollectionBatches(participantId)
        val windowCount = dao.countFeatureWindows(participantId)
        val feedbackCount = dao.countFeedbacks(participantId)
        val settings = dao.getSettings()
        return StorageStats(
            storedRawEventCount = rawCount,
            storedBatchCount = batchCount,
            storedWindowCount = windowCount,
            storedFeedbackCount = feedbackCount,
            lastCheckpointMs = settings?.lastCheckpointMs
        )
    }

    override suspend fun readFeatureWindows(participantId: String): List<FeatureWindowEntity> {
        return dao.getAllFeatureWindows(participantId)
    }

    override suspend fun readFeedbacks(participantId: String): List<FeedbackEntity> {
        return dao.getAllFeedbacks(participantId)
    }

    override suspend fun readPredictionStatus(participantId: String): PredictionStatus {
        val predictor = predictorProvider()
        if (!predictor.isReady || !predictor.metadata.isRealModel) return PredictionStatus()
        val version = predictor.metadata.modelVersion
        return PredictionStatus(version, dao.getLatestPrediction(participantId, version))
    }

    override suspend fun persistCollectionBatch(
        participantId: String,
        consentVersion: String?,
        consentTimeMs: Long?,
        collectionEnabled: Boolean,
        batch: UsageQueryBatchResult,
        zoneId: ZoneId
    ): BatchPersistenceSummary {
        val predictor = if (collectionEnabled && batch.errorCode == UsageQueryErrorCode.NONE) {
            predictorProvider()
        } else {
            DisabledPredictor
        }
        val result = database.withTransaction {
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

            val lookbackContextStartMs = maxOf(0L, batch.queriedStartMs - AppConfig.MAX_SESSION_DURATION_MS)
            val storedInRange = dao.getRawEventsInRange(
                participantId = participantId,
                startMs = lookbackContextStartMs,
                endMs = batch.queriedEndMs
            )
            val domainEvents = storedInRange.mapNotNull { row ->
                val type = runCatching { NormalizedEventType.valueOf(row.eventType) }.getOrNull()
                    ?: return@mapNotNull null
                DomainUsageEvent(
                    timestampMs = row.timestampMs,
                    eventType = type,
                    packageToken = row.packageToken,
                    isNonAppPackage = row.isNonAppPackage
                )
            }
            val qualityIssues = when (batch.errorCode) {
                UsageQueryErrorCode.NONE -> emptyList()
                UsageQueryErrorCode.PERMISSION_REQUIRED,
                UsageQueryErrorCode.SECURITY_EXCEPTION -> listOf(
                    QualityIssueInterval(
                        startMs = batch.queriedStartMs,
                        endMs = batch.queriedEndMs,
                        reason = WindowQualityReason.PERMISSION_DISABLED
                    )
                )
                UsageQueryErrorCode.USER_LOCKED_AFTER_REBOOT -> listOf(
                    QualityIssueInterval(
                        startMs = batch.queriedStartMs,
                        endMs = batch.queriedEndMs,
                        reason = WindowQualityReason.REBOOT_GAP
                    )
                )
                else -> listOf(
                    QualityIssueInterval(
                        startMs = batch.queriedStartMs,
                        endMs = batch.queriedEndMs,
                        reason = WindowQualityReason.QUERY_ERROR
                    )
                )
            }

            val computedWindows = FeatureWindowCalculator.computeWindows(
                events = domainEvents,
                rangeStartMs = batch.queriedStartMs,
                rangeEndMs = batch.queriedEndMs,
                knownCutoffMs = batch.queriedEndMs,
                zoneId = zoneId,
                qualityIssues = qualityIssues
            )
            computedWindows.forEach { computed ->
                val existing = dao.findFeatureWindow(
                    participantId = participantId,
                    startMs = computed.startMs,
                    schemaVersion = computed.schemaVersion
                )
                val nextRevision = if (existing != null) existing.revision + 1 else 1
                val windowId = dao.upsertFeatureWindow(
                    FeatureWindowEntity(
                        id = existing?.id ?: 0L,
                        participantId = participantId,
                        startMs = computed.startMs,
                        endMs = computed.endMs,
                        timezoneId = computed.timezoneId,
                        offsetSeconds = computed.offsetSeconds,
                        screenSeconds = computed.features.screenSeconds,
                        sessionsStarted = computed.features.sessionsStarted,
                        longestRunSeconds = computed.features.longestRunSeconds,
                        appSwitches = computed.features.appSwitches,
                        previousSessionSeconds = computed.features.previousSessionSeconds,
                        hour = computed.features.hour,
                        dayOfWeek = computed.features.dayOfWeek,
                        schemaVersion = computed.schemaVersion,
                        revision = nextRevision,
                        qualityStatus = computed.qualityStatus.name,
                        qualityReasons = computed.qualityReasons.joinToString("|") { it.name },
                        computedAtMs = batch.collectedAtMs
                    )
                )
                if (predictor.isReady && predictor.metadata.isRealModel &&
                    !dao.hasPrediction(windowId, predictor.metadata.modelVersion)
                ) {
                    val targetStartMs = computed.endMs
                    val targetEndMs = targetStartMs + AppConfig.WINDOW_DURATION_MS
                    val producedAtMs = nowMsProvider()
                    val outcome = PredictionRunner.evaluate(
                        predictor = predictor,
                        vector = computed.features,
                        qualityStatus = computed.qualityStatus,
                        targetStartMs = targetStartMs,
                        producedAtMs = producedAtMs,
                        isReplay = producedAtMs >= targetEndMs
                    )
                    if (outcome is PredictionOutcome.Success) {
                        dao.insertPrediction(
                            PredictionEntity(
                                windowId = windowId,
                                targetStartMs = targetStartMs,
                                targetEndMs = targetEndMs,
                                producedAtMs = outcome.producedAtMs,
                                modelVersion = outcome.modelVersion,
                                score = outcome.score,
                                threshold = outcome.threshold,
                                decision = outcome.decision,
                                qualityStatus = computed.qualityStatus.name,
                                mode = outcome.mode.name
                            )
                        )
                    }
                }
            }

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
                    remindersEnabled = reminderDelivery?.remindersEnabled(participantId)
                        ?: existingSettings?.remindersEnabled ?: false,
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
            val derivedRetentionCutoffMs =
                batch.collectedAtMs - AppConfig.DERIVED_RETENTION_DAYS * 86_400_000L
            dao.deleteRawEventsBefore(rawRetentionCutoffMs)
            dao.deleteRemindersForExpiredWindows(derivedRetentionCutoffMs)
            dao.deletePredictionsForExpiredWindows(derivedRetentionCutoffMs)
            dao.deleteFeatureWindowsBefore(derivedRetentionCutoffMs)
            dao.deleteFeedbacksBefore(derivedRetentionCutoffMs)

            BatchPersistenceSummary(
                insertedEventCount = insertedCount,
                duplicateSkippedCount = duplicateSkippedCount,
                computedWindowCount = computedWindows.size,
                stats = StorageStats(
                    storedRawEventCount = dao.countRawEvents(participantId),
                    storedBatchCount = dao.countCollectionBatches(participantId),
                    storedWindowCount = dao.countFeatureWindows(participantId),
                    storedFeedbackCount = dao.countFeedbacks(participantId),
                    lastCheckpointMs = nextCheckpoint
                )
            )
        }
        if (reminderDelivery != null && collectionEnabled && batch.errorCode == UsageQueryErrorCode.NONE) {
            readPredictionStatus(participantId).latest?.let { prediction ->
                reminderDelivery.deliver(participantId, prediction, zoneId)
            }
        }
        return result
    }

    override suspend fun saveFeedback(
        participantId: String,
        windowId: Long?,
        predictionId: Long?,
        windowStartMs: Long,
        windowEndMs: Long,
        answer: FeedbackAnswer?,
        responseStatus: FeedbackResponseStatus,
        nowMs: Long
    ): FeedbackEntity {
        val normalizedAnswer = if (responseStatus == FeedbackResponseStatus.ANSWERED) {
            answer?.name
        } else {
            null
        }
        return database.withTransaction {
            val existing = dao.findFeedback(participantId, windowStartMs, windowEndMs)
            val answeredAt = when {
                responseStatus != FeedbackResponseStatus.ANSWERED -> existing?.answeredAtMs
                existing?.answeredAtMs != null -> existing.answeredAtMs
                else -> nowMs
            }
            val updatedAt = if (existing != null) nowMs else null
            val entity = FeedbackEntity(
                id = existing?.id ?: 0L,
                participantId = participantId,
                windowId = windowId ?: existing?.windowId,
                predictionId = predictionId ?: existing?.predictionId,
                windowStartMs = windowStartMs,
                windowEndMs = windowEndMs,
                answer = normalizedAnswer,
                responseStatus = responseStatus.name,
                askedAtMs = existing?.askedAtMs ?: nowMs,
                answeredAtMs = answeredAt,
                updatedAtMs = updatedAt,
                questionVersion = FeedbackRecord.QUESTION_VERSION
            )
            val savedId = dao.upsertFeedback(entity)
            entity.copy(id = if (existing != null) existing.id else savedId)
        }
    }

    override suspend fun buildExportPayload(
        participantId: String,
        appVersion: String,
        nowMs: Long
    ): ExportPayload {
        val windows = dao.getAllFeatureWindows(participantId)
        val feedbacks = dao.getAllFeedbacks(participantId)
        return ExportFormatter.buildPayload(
            participantId = participantId,
            appVersion = appVersion,
            createdAtMs = nowMs,
            windows = windows,
            feedbacks = feedbacks
        )
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
