package com.group8.wellbeing.domain

enum class WindowQualityStatus {
    OBSERVATION_SUFFICIENT,
    UNCERTAIN,
    INVALID
}

enum class WindowQualityReason {
    PERMISSION_DISABLED,
    QUERY_ERROR,
    REBOOT_GAP,
    OUT_OF_RETENTION,
    CONSENT_OR_PAUSE_BOUNDARY,
    CLOCK_JUMP,
    MISSING_SESSION_START,
    OPEN_SESSION_AT_END,
    INCOMPLETE_WINDOW
}
