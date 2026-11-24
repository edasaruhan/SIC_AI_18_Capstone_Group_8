package com.group8.wellbeing.ui

import com.group8.wellbeing.domain.CollectionStatus
import com.group8.wellbeing.usage.NormalizedEventType
import com.group8.wellbeing.usage.NotificationPermissionState
import com.group8.wellbeing.usage.UsagePermissionState
import com.group8.wellbeing.usage.UsageQueryErrorCode

enum class MainTab {
    STATUS,
    INSPECTOR
}

data class InspectedEventUi(
    val timeText: String,
    val eventType: NormalizedEventType,
    val detailText: String,
    val packageToken: String?,
    val isNonAppPackage: Boolean
)

data class QuerySummaryUi(
    val rangeText: String,
    val collectedAtText: String,
    val rawScannedCount: Int,
    val normalizedCount: Int,
    val insertedCount: Int,
    val duplicateSkippedCount: Int,
    val errorCode: UsageQueryErrorCode,
    val errorDetail: String?,
    val countsByType: Map<NormalizedEventType, Int>
)

data class MainUiState(
    val appTitle: String = "Adaptive Digital Wellbeing Coach",
    val darkThemeEnabled: Boolean = true,
    val selectedTab: MainTab = MainTab.STATUS,
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
    val statusNotice: String = "Henüz kontrol yapılmadı.",
    val manualCheckCount: Int = 0,
    val storedRawEventCount: Int = 0,
    val storedBatchCount: Int = 0,
    val lastCheckpointText: String = "Henüz kayıt yok",
    val selectedLookbackMinutes: Int = 30,
    val lastQuerySummary: QuerySummaryUi? = null,
    val inspectedEvents: List<InspectedEventUi> = emptyList()
)
