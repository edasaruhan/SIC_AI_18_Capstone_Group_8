package com.group8.wellbeing.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.group8.wellbeing.data.AppConfig
import com.group8.wellbeing.data.ControlSnapshot
import com.group8.wellbeing.data.ControlStore
import com.group8.wellbeing.domain.CollectionStateResolver
import com.group8.wellbeing.usage.NormalizedEventType
import com.group8.wellbeing.usage.NotificationPermissionState
import com.group8.wellbeing.usage.UsageEventReader
import com.group8.wellbeing.usage.UsagePermissionState
import com.group8.wellbeing.usage.UsageQueryErrorCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(
    private val controlStore: ControlStore,
    private val usageEventReader: UsageEventReader? = null,
    private val nowMsProvider: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = mutableUiState.asStateFlow()

    private var lastUsagePermission: UsagePermissionState = UsagePermissionState.REQUIRED
    private var lastNotificationPermission: NotificationPermissionState = NotificationPermissionState.NOT_REQUIRED
    private var errorMessage: String? = null

    fun syncPermissions(
        usagePermission: UsagePermissionState,
        notificationPermission: NotificationPermissionState
    ) {
        lastUsagePermission = usagePermission
        lastNotificationPermission = notificationPermission
        val snapshot = if (usagePermission == UsagePermissionState.GRANTED) {
            controlStore.markUsagePermissionSeenGranted()
        } else {
            controlStore.read()
        }
        publish(snapshot)
    }

    fun toggleTheme() {
        val current = controlStore.read()
        val updated = controlStore.setDarkThemeEnabled(!current.darkThemeEnabled)
        publish(updated)
    }

    fun selectTab(tab: MainTab) {
        mutableUiState.value = mutableUiState.value.copy(selectedTab = tab)
    }

    fun selectLookbackMinutes(minutes: Int) {
        mutableUiState.value = mutableUiState.value.copy(selectedLookbackMinutes = minutes)
    }

    fun acceptConsent() {
        val snapshot = controlStore.acceptConsent(
            version = AppConfig.CONSENT_VERSION,
            acceptedAtMs = nowMsProvider()
        )
        publish(
            snapshot = snapshot,
            notice = "Katılım onayınız kaydedildi (${AppConfig.CONSENT_VERSION})."
        )
    }

    fun declineConsent() {
        val snapshot = controlStore.declineConsent()
        publish(
            snapshot = snapshot,
            notice = "Katılım onayı verilmedi. Hiçbir kullanım kaydı okunmuyor."
        )
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        lastNotificationPermission = if (granted) {
            NotificationPermissionState.GRANTED
        } else {
            NotificationPermissionState.DENIED
        }
        val notice = if (granted) {
            "Bildirim izni verildi."
        } else {
            "Bildirim izni kapalı. Veri toplama ve geri bildirim bildirim olmadan çalışmaya devam eder."
        }
        publish(controlStore.read(), notice = notice)
    }

    fun pauseCollection() {
        val snapshot = controlStore.pauseCollection(nowMsProvider())
        publish(
            snapshot = snapshot,
            notice = "Veri toplama duraklatıldı. Yeni sorgular ve arka plan işleri durduruldu."
        )
    }

    fun resumeCollection() {
        val snapshot = controlStore.resumeCollection(
            resumedAtMs = nowMsProvider(),
            backfillPausedPeriod = false
        )
        publish(
            snapshot = snapshot,
            notice = "Veri toplama yeniden başlatıldı. Duraklatılan dönem geriye dönük toplanmaz."
        )
    }

    fun requestDeleteDialog() {
        mutableUiState.value = mutableUiState.value.copy(showDeleteDialog = true)
    }

    fun dismissDeleteDialog() {
        mutableUiState.value = mutableUiState.value.copy(showDeleteDialog = false)
    }

    fun confirmDeleteAllLocalData() {
        errorMessage = null
        val snapshot = controlStore.deleteAllLocalData()
        publish(
            snapshot = snapshot,
            showDeleteDialog = false,
            notice = "Yerel kayıtlar, katılım onayı ve bekleyen bildirimler temizlendi. Yeni yerel kimlik oluşturuldu; daha önce dışa aktardığınız dosyalar otomatik silinmez.",
            clearInspector = true
        )
    }

    fun runManualCheck(
        usagePermission: UsagePermissionState,
        notificationPermission: NotificationPermissionState
    ) {
        lastUsagePermission = usagePermission
        lastNotificationPermission = notificationPermission
        val snapshot = if (usagePermission == UsagePermissionState.GRANTED) {
            controlStore.markUsagePermissionSeenGranted()
        } else {
            controlStore.read()
        }
        val resolved = CollectionStateResolver.resolve(
            consentAccepted = snapshot.isConsentAccepted,
            usagePermission = usagePermission,
            hadUsagePermissionPreviously = snapshot.hadUsagePermissionPreviously,
            pausedByUser = snapshot.pausedByUser,
            errorMessage = errorMessage
        )
        val nextCount = mutableUiState.value.manualCheckCount + 1
        val timeText = formatTime(nowMsProvider())
        val queryNote = if (resolved.canQueryUsageData) {
            "Yetki ve onay uygun. Olay İnceleyici üzerinden okuma yapabilirsiniz."
        } else {
            "Veri sorgusu kapalı (${resolved.status.name})."
        }
        publish(
            snapshot = snapshot,
            notice = "Kontrol #$nextCount ($timeText): $queryNote",
            manualCheckCount = nextCount
        )
    }

    fun queryRecentEvents(
        usagePermission: UsagePermissionState,
        notificationPermission: NotificationPermissionState
    ) {
        lastUsagePermission = usagePermission
        lastNotificationPermission = notificationPermission
        val snapshot = if (usagePermission == UsagePermissionState.GRANTED) {
            controlStore.markUsagePermissionSeenGranted()
        } else {
            controlStore.read()
        }
        val resolved = CollectionStateResolver.resolve(
            consentAccepted = snapshot.isConsentAccepted,
            usagePermission = usagePermission,
            hadUsagePermissionPreviously = snapshot.hadUsagePermissionPreviously,
            pausedByUser = snapshot.pausedByUser,
            errorMessage = errorMessage
        )
        val nowMs = nowMsProvider()
        val lookbackMs = mutableUiState.value.selectedLookbackMinutes * 60_000L
        val startMs = nowMs - lookbackMs

        if (!resolved.canQueryUsageData) {
            val blockedCode = when {
                !snapshot.isConsentAccepted -> UsageQueryErrorCode.CONSENT_REQUIRED
                snapshot.pausedByUser -> UsageQueryErrorCode.PAUSED_BY_USER
                else -> UsageQueryErrorCode.PERMISSION_REQUIRED
            }
            val summary = QuerySummaryUi(
                rangeText = "${formatTime(startMs)} - ${formatTime(nowMs)}",
                collectedAtText = formatTime(nowMs),
                rawScannedCount = 0,
                normalizedCount = 0,
                errorCode = blockedCode,
                errorDetail = resolved.nextStepMessage,
                countsByType = emptyMap()
            )
            publish(
                snapshot = snapshot,
                notice = "Sorgu yapılmadı: ${resolved.nextStepMessage}",
                querySummary = summary,
                inspectedEvents = emptyList()
            )
            return
        }

        val reader = usageEventReader ?: return
        val result = reader.queryEvents(
            startMs = startMs,
            endMs = nowMs,
            collectedAtMs = nowMs
        )
        val counts = result.events.groupingBy { it.eventType }.eachCount()
        val summary = QuerySummaryUi(
            rangeText = "${formatTime(result.queriedStartMs)} - ${formatTime(result.queriedEndMs)}",
            collectedAtText = formatTime(result.collectedAtMs),
            rawScannedCount = result.rawScannedCount,
            normalizedCount = result.events.size,
            errorCode = result.errorCode,
            errorDetail = result.errorDetail,
            countsByType = counts
        )
        val items = result.events.map { event ->
            val detail = when (event.eventType) {
                NormalizedEventType.APP_FOREGROUND -> {
                    val pkg = event.packageName ?: "Bilinmeyen paket"
                    val shortClass = event.className?.substringAfterLast('.')
                    if (shortClass != null) "$pkg · $shortClass" else pkg
                }
                NormalizedEventType.SCREEN_INTERACTIVE -> "Ekran aktifleşti (Interactive)"
                NormalizedEventType.KEYGUARD_HIDDEN -> "Kilit ekranı gizlendi (Unlocked adayı)"
                NormalizedEventType.KEYGUARD_SHOWN -> "Kilit ekranı gösterildi"
                NormalizedEventType.SCREEN_NON_INTERACTIVE -> "Ekran kapandı (Off adayı)"
            }
            InspectedEventUi(
                timeText = formatTimeWithMillis(event.timestampMs),
                eventType = event.eventType,
                detailText = detail,
                packageToken = event.packageToken,
                isNonAppPackage = event.isNonAppPackage
            )
        }
        val noticeText = if (result.errorCode == UsageQueryErrorCode.NONE) {
            "Son ${mutableUiState.value.selectedLookbackMinutes} dakikada ${result.events.size} normalize olay okundu."
        } else {
            "Sorgu uyarısı (${result.errorCode.name}): ${result.errorDetail ?: "Kayıt alınamadı."}"
        }
        publish(
            snapshot = snapshot,
            notice = noticeText,
            querySummary = summary,
            inspectedEvents = items
        )
    }

    private fun publish(
        snapshot: ControlSnapshot,
        notice: String? = null,
        showDeleteDialog: Boolean = mutableUiState.value.showDeleteDialog,
        manualCheckCount: Int = mutableUiState.value.manualCheckCount,
        querySummary: QuerySummaryUi? = mutableUiState.value.lastQuerySummary,
        inspectedEvents: List<InspectedEventUi> = mutableUiState.value.inspectedEvents,
        clearInspector: Boolean = false
    ) {
        val resolved = CollectionStateResolver.resolve(
            consentAccepted = snapshot.isConsentAccepted,
            usagePermission = lastUsagePermission,
            hadUsagePermissionPreviously = snapshot.hadUsagePermissionPreviously,
            pausedByUser = snapshot.pausedByUser,
            errorMessage = errorMessage
        )
        val current = mutableUiState.value
        mutableUiState.value = current.copy(
            darkThemeEnabled = snapshot.darkThemeEnabled,
            participantIdShort = snapshot.participantId.take(8),
            consentAccepted = snapshot.isConsentAccepted,
            consentDeclined = snapshot.consentDeclined,
            consentVersion = snapshot.consentVersion,
            consentAcceptedTimeText = snapshot.consentAcceptedAtMs?.let { formatDateTime(it) },
            collectionStatus = resolved.status,
            canQueryUsageData = resolved.canQueryUsageData,
            nextStepMessage = resolved.nextStepMessage,
            usagePermissionState = lastUsagePermission,
            notificationPermissionState = lastNotificationPermission,
            pausedByUser = snapshot.pausedByUser,
            showDeleteDialog = showDeleteDialog,
            statusNotice = notice ?: current.statusNotice,
            manualCheckCount = manualCheckCount,
            lastQuerySummary = if (clearInspector) null else querySummary,
            inspectedEvents = if (clearInspector) emptyList() else inspectedEvents
        )
    }

    private fun formatTime(epochMs: Long): String {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return formatter.format(Date(epochMs))
    }

    private fun formatTimeWithMillis(epochMs: Long): String {
        val formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        return formatter.format(Date(epochMs))
    }

    private fun formatDateTime(epochMs: Long): String {
        val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return formatter.format(Date(epochMs))
    }

    class Factory(
        private val controlStore: ControlStore,
        private val usageEventReader: UsageEventReader
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(controlStore, usageEventReader) as T
        }
    }
}
