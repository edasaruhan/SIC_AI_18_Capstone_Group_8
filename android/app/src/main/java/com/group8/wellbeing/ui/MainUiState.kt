package com.group8.wellbeing.ui

import com.group8.wellbeing.domain.CollectionStatus
import com.group8.wellbeing.usage.UsagePermissionState

data class MainUiState(
    val appTitle: String = "Adaptive Digital Wellbeing Coach",
    val collectionStatus: CollectionStatus = CollectionStatus.CONSENT_PENDING,
    val permissionState: UsagePermissionState = UsagePermissionState.REQUIRED,
    val manualCheckCount: Int = 0,
    val lastManualCheckSummary: String = "Henüz elle deneme yapılmadı."
)
