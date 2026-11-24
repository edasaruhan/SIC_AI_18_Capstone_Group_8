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

    @Query("DELETE FROM feature_windows WHERE end_ms < :cutoffMs")
    suspend fun deleteFeatureWindowsBefore(cutoffMs: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrediction(prediction: PredictionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminderEvent(reminder: ReminderEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFeedback(feedback: FeedbackEntity): Long

    @Query("SELECT * FROM feedbacks WHERE participant_id = :participantId ORDER BY window_start_ms DESC")
    suspend fun getAllFeedbacks(participantId: String): List<FeedbackEntity>

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
