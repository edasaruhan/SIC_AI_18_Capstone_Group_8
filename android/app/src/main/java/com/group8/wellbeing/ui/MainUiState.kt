package com.group8.wellbeing.ui

import com.group8.wellbeing.domain.CollectionStatus
import com.group8.wellbeing.usage.NotificationPermissionState
import com.group8.wellbeing.usage.UsagePermissionState

data class MainUiState(
    val appTitle: String = "Adaptive Digital Wellbeing Coach",
    val participantIdShort: String = "",
    val consentAccepted: Boolean = false,
    val consentDeclined: Boolean = false,
    val consentVersion: String? = null,
    val consentAcceptedTimeText: String? = null,
    val collectionStatus: CollectionStatus = CollectionStatus.CONSENT_PENDING,
    val canQueryUsageData: Boolean = false,
    val nextStepMessage: String = "",
    val usagePermissionState: UsagePermissionState = UsagePermissionState.REQUIRED,
    val notificationPermissionState: NotificationPermissionState = NotificationPermissionState.NOT_REQUIRED,
    val pausedByUser: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val statusNotice: String = "Henüz elle deneme yapılmadı.",
    val manualCheckCount: Int = 0
)
