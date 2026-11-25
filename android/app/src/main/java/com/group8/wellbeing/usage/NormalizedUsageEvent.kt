package com.group8.wellbeing.usage

enum class NormalizedEventType {
    SCREEN_INTERACTIVE,
    KEYGUARD_HIDDEN,
    KEYGUARD_SHOWN,
    SCREEN_NON_INTERACTIVE,
    APP_FOREGROUND
}

data class RawSystemEvent(
    val timestampMs: Long,
    val androidEventType: Int,
    val packageName: String?,
    val className: String?
)

data class NormalizedUsageEvent(
    val timestampMs: Long,
    val eventType: NormalizedEventType,
    val packageName: String?,
    val packageToken: String?,
    val className: String?,
    val isNonAppPackage: Boolean,
    val sourceEventCode: Int,
    val sourceVersion: String
)

enum class UsageQueryErrorCode {
    NONE,
    CONSENT_REQUIRED,
    PERMISSION_REQUIRED,
    PAUSED_BY_USER,
    USER_LOCKED_AFTER_REBOOT,
    NULL_EVENTS,
    SECURITY_EXCEPTION,
    UNEXPECTED_ERROR
}

data class UsageQueryBatchResult(
    val queriedStartMs: Long,
    val queriedEndMs: Long,
    val collectedAtMs: Long,
    val permissionState: UsagePermissionState,
    val events: List<NormalizedUsageEvent>,
    val rawScannedCount: Int,
    val errorCode: UsageQueryErrorCode,
    val errorDetail: String? = null
)
