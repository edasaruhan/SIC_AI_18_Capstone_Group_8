package com.group8.wellbeing.data

object AppConfig {
    const val APP_SCHEMA_VERSION: Int = 1
    const val FEATURE_SCHEMA_VERSION: String = "v1"
    const val MIN_API_LEVEL: Int = 28
    const val WINDOW_DURATION_MS: Long = 1_800_000L
    const val MAX_SESSION_DURATION_MS: Long = 10_800_000L
    const val WORK_INTERVAL_MINUTES: Long = 15L
    const val LIVE_PREDICTION_MAX_DELAY_MS: Long = 300_000L
    const val DAILY_REMINDER_CAP: Int = 3
    const val REMINDER_COOLDOWN_MINUTES: Int = 60
    const val QUIET_START_HOUR: Int = 23
    const val QUIET_END_HOUR: Int = 8
    const val RAW_EVENT_RETENTION_DAYS: Int = 7
    const val DERIVED_RETENTION_DAYS: Int = 30
}
