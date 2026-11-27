package com.group8.wellbeing.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WellbeingDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRawEvents(events: List<RawUsageEventEntity>): List<Long>

    @Query("SELECT COUNT(*) FROM raw_usage_events WHERE participant_id = :participantId")
    suspend fun countRawEvents(participantId: String): Int

    @Query(
        "SELECT * FROM raw_usage_events WHERE participant_id = :participantId " +
            "AND timestamp_ms >= :startMs AND timestamp_ms < :endMs ORDER BY timestamp_ms ASC, id ASC"
    )
    suspend fun getRawEventsInRange(
        participantId: String,
        startMs: Long,
        endMs: Long
    ): List<RawUsageEventEntity>

    @Query("DELETE FROM raw_usage_events WHERE timestamp_ms < :cutoffMs")
    suspend fun deleteRawEventsBefore(cutoffMs: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollectionBatch(batch: CollectionBatchEntity): Long

    @Query("SELECT COUNT(*) FROM collection_batches WHERE participant_id = :participantId")
    suspend fun countCollectionBatches(participantId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFeatureWindow(window: FeatureWindowEntity): Long

    @Query(
        "SELECT * FROM feature_windows WHERE participant_id = :participantId " +
            "AND start_ms = :startMs AND schema_version = :schemaVersion LIMIT 1"
    )
    suspend fun findFeatureWindow(
        participantId: String,
        startMs: Long,
        schemaVersion: String
    ): FeatureWindowEntity?

    @Query("SELECT * FROM feature_windows WHERE participant_id = :participantId ORDER BY start_ms DESC")
    suspend fun getAllFeatureWindows(participantId: String): List<FeatureWindowEntity>

    @Query("SELECT COUNT(*) FROM feature_windows WHERE participant_id = :participantId")
    suspend fun countFeatureWindows(participantId: String): Int

    @Query("DELETE FROM feature_windows WHERE end_ms < :cutoffMs")
    suspend fun deleteFeatureWindowsBefore(cutoffMs: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrediction(prediction: PredictionEntity): Long

    @Query("SELECT EXISTS(SELECT 1 FROM predictions WHERE window_id = :windowId AND model_version = :modelVersion)")
    suspend fun hasPrediction(windowId: Long, modelVersion: String): Boolean

    @Query(
        "SELECT p.* FROM predictions p JOIN feature_windows w ON p.window_id = w.id " +
            "WHERE w.participant_id = :participantId AND p.model_version = :modelVersion " +
            "ORDER BY p.target_start_ms DESC, p.id DESC LIMIT 1"
    )
    suspend fun getLatestPrediction(participantId: String, modelVersion: String): PredictionEntity?

    @Query(
        "DELETE FROM predictions WHERE window_id IN (SELECT id FROM feature_windows WHERE end_ms < :cutoffMs)"
    )
    suspend fun deletePredictionsForExpiredWindows(cutoffMs: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminderEvent(reminder: ReminderEventEntity): Long

    @Query("SELECT EXISTS(SELECT 1 FROM reminder_events WHERE prediction_id = :predictionId)")
    suspend fun hasReminderAttempt(predictionId: Long): Boolean

    @Query(
        "SELECT EXISTS(SELECT 1 FROM predictions p JOIN feature_windows w ON w.id = p.window_id " +
            "WHERE p.id = :predictionId AND w.participant_id = :participantId)"
    )
    suspend fun ownsPrediction(participantId: String, predictionId: Long): Boolean

    @Query(
        "SELECT r.* FROM reminder_events r JOIN predictions p ON p.id = r.prediction_id " +
            "JOIN feature_windows w ON w.id = p.window_id " +
            "WHERE w.participant_id = :participantId AND r.suppressed_reason IS NULL " +
            "AND (r.target_start_ms = :targetStartMs OR COALESCE(r.shown_at_ms, r.scheduled_at_ms) >= :sinceMs)"
    )
    suspend fun getRecentReminderReservations(
        participantId: String,
        targetStartMs: Long,
        sinceMs: Long
    ): List<ReminderEventEntity>

    @Query("UPDATE reminder_events SET shown_at_ms = :shownAtMs, suppressed_reason = :reason WHERE id = :id")
    suspend fun completeReminderAttempt(id: Long, shownAtMs: Long?, reason: String?)

    @Query(
        "UPDATE reminder_events SET user_action = :action " +
            "WHERE id = :id AND suppressed_reason IS NULL AND user_action IS NULL"
    )
    suspend fun recordReminderAction(id: Long, action: String)

    @Query("SELECT * FROM reminder_events WHERE prediction_id = :predictionId LIMIT 1")
    suspend fun getReminderForPrediction(predictionId: Long): ReminderEventEntity?

    @Query(
        "DELETE FROM reminder_events WHERE prediction_id IN " +
            "(SELECT p.id FROM predictions p JOIN feature_windows w ON w.id = p.window_id WHERE w.end_ms < :cutoffMs)"
    )
    suspend fun deleteRemindersForExpiredWindows(cutoffMs: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFeedback(feedback: FeedbackEntity): Long

    @Query(
        "SELECT * FROM feedbacks WHERE participant_id = :participantId " +
            "AND window_start_ms = :windowStartMs AND window_end_ms = :windowEndMs LIMIT 1"
    )
    suspend fun findFeedback(
        participantId: String,
        windowStartMs: Long,
        windowEndMs: Long
    ): FeedbackEntity?

    @Query("SELECT * FROM feedbacks WHERE participant_id = :participantId ORDER BY window_start_ms DESC")
    suspend fun getAllFeedbacks(participantId: String): List<FeedbackEntity>

    @Query("SELECT COUNT(*) FROM feedbacks WHERE participant_id = :participantId")
    suspend fun countFeedbacks(participantId: String): Int

    @Query("DELETE FROM feedbacks WHERE window_end_ms < :cutoffMs")
    suspend fun deleteFeedbacksBefore(cutoffMs: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: AppSettingsEntity)

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): AppSettingsEntity?

    @Query("DELETE FROM raw_usage_events")
    suspend fun clearRawEvents()

    @Query("DELETE FROM collection_batches")
    suspend fun clearCollectionBatches()

    @Query("DELETE FROM feature_windows")
    suspend fun clearFeatureWindows()

    @Query("DELETE FROM predictions")
    suspend fun clearPredictions()

    @Query("DELETE FROM reminder_events")
    suspend fun clearReminderEvents()

    @Query("DELETE FROM feedbacks")
    suspend fun clearFeedbacks()

    @Query("DELETE FROM app_settings")
    suspend fun clearSettings()
}
