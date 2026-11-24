package com.group8.wellbeing.domain

enum class CollectionStatus {
    CONSENT_PENDING,
    PERMISSION_REQUIRED,
    ACTIVE,
    PAUSED_BY_USER,
    ACCESS_REVOKED,
    ERROR
}
