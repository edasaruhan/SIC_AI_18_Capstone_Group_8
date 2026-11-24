package com.group8.wellbeing.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "raw_usage_events",
    indices = [
        Index(
            value = ["participant_id", "event_fingerprint"],
            unique = true
        ),
        Index(value = ["participant_id", "timestamp_ms"])
    ]
)
data class RawUsageEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "participant_id")
    val participantId: String,
    @ColumnInfo(name = "timestamp_ms")
    val timestampMs: Long,
    @ColumnInfo(name = "event_type")
    val eventType: String,
    @ColumnInfo(name = "package_token")
    val packageToken: String?,
    @ColumnInfo(name = "is_non_app_package")
    val isNonAppPackage: Boolean,
    @ColumnInfo(name = "source_event_code")
    val sourceEventCode: Int,
    @ColumnInfo(name = "source_version")
    val sourceVersion: String,
    @ColumnInfo(name = "event_fingerprint")
    val eventFingerprint: String
)

@Entity(
    tableName = "collection_batches",
    indices = [Index(value = ["participant_id", "collected_at_ms"])]
)
data class CollectionBatchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "participant_id")
    val participantId: String,
    @ColumnInfo(name = "queried_start_ms")
    val queriedStartMs: Long,
    @ColumnInfo(name = "queried_end_ms")
    val queriedEndMs: Long,
    @ColumnInfo(name = "collected_at_ms")
    val collectedAtMs: Long,
    @ColumnInfo(name = "permission_state")
    val permissionState: String,
    @ColumnInfo(name = "raw_scanned_count")
    val rawScannedCount: Int,
    @ColumnInfo(name = "result_count")
    val resultCount: Int,
    @ColumnInfo(name = "inserted_count")
    val insertedCount: Int,
    @ColumnInfo(name = "error_code")
    val errorCode: String,
    @ColumnInfo(name = "error_detail")
    val errorDetail: String?
)

@Entity(
    tableName = "feature_windows",
    indices = [
        Index(
            value = ["participant_id", "start_ms", "schema_version"],
            unique = true
        )
    ]
)
data class FeatureWindowEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "participant_id")
    val participantId: String,
    @ColumnInfo(name = "start_ms")
    val startMs: Long,
    @ColumnInfo(name = "end_ms")
    val endMs: Long,
    @ColumnInfo(name = "timezone_id")
    val timezoneId: String,
    @ColumnInfo(name = "offset_seconds")
    val offsetSeconds: Int,
    @ColumnInfo(name = "screen_s")
    val screenSeconds: Double,
    @ColumnInfo(name = "sessions_started")
    val sessionsStarted: Int,
    @ColumnInfo(name = "longest_run_s")
    val longestRunSeconds: Double,
    @ColumnInfo(name = "app_switches")
    val appSwitches: Int,
    @ColumnInfo(name = "prev_session_s")
    val previousSessionSeconds: Double,
    @ColumnInfo(name = "hour")
    val hour: Int,
    @ColumnInfo(name = "dow")
    val dayOfWeek: Int,
    @ColumnInfo(name = "schema_version")
    val schemaVersion: String,
    @ColumnInfo(name = "revision")
    val revision: Int,
    @ColumnInfo(name = "quality_status")
    val qualityStatus: String,
    @ColumnInfo(name = "quality_reasons")
    val qualityReasons: String,
    @ColumnInfo(name = "computed_at_ms")
    val computedAtMs: Long
)

@Entity(
    tableName = "predictions",
    indices = [Index(value = ["window_id"])]
)
data class PredictionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "window_id")
    val windowId: Long,
    @ColumnInfo(name = "target_start_ms")
    val targetStartMs: Long,
    @ColumnInfo(name = "target_end_ms")
    val targetEndMs: Long,
    @ColumnInfo(name = "produced_at_ms")
    val producedAtMs: Long,
    @ColumnInfo(name = "model_version")
    val modelVersion: String,
    @ColumnInfo(name = "score")
    val score: Double,
    @ColumnInfo(name = "threshold")
    val threshold: Double,
    @ColumnInfo(name = "decision")
    val decision: Boolean,
    @ColumnInfo(name = "quality_status")
    val qualityStatus: String,
    @ColumnInfo(name = "mode")
    val mode: String
)

@Entity(
    tableName = "reminder_events",
    indices = [Index(value = ["prediction_id"])]
)
data class ReminderEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "prediction_id")
    val predictionId: Long,
    @ColumnInfo(name = "target_start_ms")
    val targetStartMs: Long,
    @ColumnInfo(name = "scheduled_at_ms")
    val scheduledAtMs: Long,
    @ColumnInfo(name = "shown_at_ms")
    val shownAtMs: Long?,
    @ColumnInfo(name = "suppressed_reason")
    val suppressedReason: String?,
    @ColumnInfo(name = "user_action")
    val userAction: String?,
    @ColumnInfo(name = "policy_version")
    val policyVersion: String
)

@Entity(
    tableName = "feedbacks",
    indices = [
        Index(
            value = ["participant_id", "window_start_ms", "window_end_ms"],
            unique = true
        )
    ]
)
data class FeedbackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "participant_id")
    val participantId: String,
    @ColumnInfo(name = "window_id")
    val windowId: Long?,
    @ColumnInfo(name = "prediction_id")
    val predictionId: Long?,
    @ColumnInfo(name = "window_start_ms")
    val windowStartMs: Long,
    @ColumnInfo(name = "window_end_ms")
    val windowEndMs: Long,
    @ColumnInfo(name = "answer")
    val answer: String?,
    @ColumnInfo(name = "response_status")
    val responseStatus: String,
    @ColumnInfo(name = "asked_at_ms")
    val askedAtMs: Long,
    @ColumnInfo(name = "answered_at_ms")
    val answeredAtMs: Long?,
    @ColumnInfo(name = "updated_at_ms")
    val updatedAtMs: Long?,
    @ColumnInfo(name = "question_version")
    val questionVersion: String
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    @ColumnInfo(name = "participant_id")
    val participantId: String,
    @ColumnInfo(name = "collection_enabled")
    val collectionEnabled: Boolean,
    @ColumnInfo(name = "reminders_enabled")
    val remindersEnabled: Boolean,
    @ColumnInfo(name = "quiet_start_hour")
    val quietStartHour: Int,
    @ColumnInfo(name = "quiet_end_hour")
    val quietEndHour: Int,
    @ColumnInfo(name = "daily_cap")
    val dailyCap: Int,
    @ColumnInfo(name = "consent_version")
    val consentVersion: String?,
    @ColumnInfo(name = "consent_time_ms")
    val consentTimeMs: Long?,
    @ColumnInfo(name = "last_checkpoint_ms")
    val lastCheckpointMs: Long?,
    @ColumnInfo(name = "schema_version")
    val schemaVersion: Int
)
