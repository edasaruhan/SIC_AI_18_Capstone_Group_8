package com.group8.wellbeing.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.group8.wellbeing.data.AppConfig
import com.group8.wellbeing.data.ControlSnapshot
import com.group8.wellbeing.data.ControlStore
import com.group8.wellbeing.data.ExportFormatter
import com.group8.wellbeing.data.ExportPayload
import com.group8.wellbeing.data.LocalDataRepository
import com.group8.wellbeing.data.StorageStats
import com.group8.wellbeing.data.local.FeatureWindowEntity
import com.group8.wellbeing.data.local.FeedbackEntity
import com.group8.wellbeing.domain.CollectionStateResolver
import com.group8.wellbeing.domain.FeedbackAnswer
import com.group8.wellbeing.domain.FeedbackResponseStatus
import com.group8.wellbeing.usage.NormalizedEventType
import com.group8.wellbeing.usage.NotificationPermissionState
import com.group8.wellbeing.usage.UsageEventReader
import com.group8.wellbeing.usage.UsagePermissionState
import com.group8.wellbeing.usage.UsageQueryErrorCode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class MainViewModel(
    private val controlStore: ControlStore,
    private val usageEventReader: UsageEventReader? = null,
    private val localDataRepository: LocalDataRepository? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val appVersion: String = "0.6.0-m09-m13",
    private val nowMsProvider: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = mutableUiState.asStateFlow()

    private var lastUsagePermission: UsagePermissionState = UsagePermissionState.REQUIRED
    private var lastNotificationPermission: NotificationPermissionState = NotificationPermissionState.NOT_REQUIRED
    private var errorMessage: String? = null
    private var latestStats: StorageStats = StorageStats()
    private var latestPeriods: List<PeriodItemUi> = emptyList()
    private var pendingExportPayload: ExportPayload? = null

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
        refreshStorageAndPeriods(snapshot)
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
        refreshStorageAndPeriods(snapshot)
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
        latestStats = StorageStats()
        latestPeriods = emptyList()
        pendingExportPayload = null
        viewModelScope.launch(ioDispatcher) {
            localDataRepository?.clearAllLocalData()
        }
        publish(
            snapshot = snapshot,
            showDeleteDialog = false,
            exportPreview = null,
            notice = "Yerel veritabanı, katılım onayı ve bekleyen bildirimler temizlendi. Yeni yerel kimlik oluşturuldu; daha önce dışa aktardığınız dosyalar otomatik silinmez.",
            clearInspector = true
        )
    }

    fun submitFeedback(
        windowId: Long,
        windowStartMs: Long,
        windowEndMs: Long,
        answer: FeedbackAnswer?,
        responseStatus: FeedbackResponseStatus
    ) {
        val repository = localDataRepository ?: return
        val snapshot = controlStore.read()
        viewModelScope.launch(ioDispatcher) {
            repository.saveFeedback(
                participantId = snapshot.participantId,
                windowId = windowId,
                predictionId = null,
                windowStartMs = windowStartMs,
                windowEndMs = windowEndMs,
                answer = answer,
                responseStatus = responseStatus,
                nowMs = nowMsProvider()
            )
            val label = when {
                responseStatus == FeedbackResponseStatus.SKIPPED -> "Atlandı"
                answer == FeedbackAnswer.YES -> "Evet"
                answer == FeedbackAnswer.NO -> "Hayır"
                else -> "Kaydedildi"
            }
            loadStorageAndPeriodsBlocking(snapshot)
            publish(
                snapshot = snapshot,
                notice = "${formatHourMinute(windowStartMs)}–${formatHourMinute(windowEndMs)} dönemi için geri bildirim kaydedildi: $label."
            )
        }
    }

    fun prepareExportPreview() {
        val repository = localDataRepository ?: return
        val snapshot = controlStore.read()
        viewModelScope.launch(ioDispatcher) {
            val payload = repository.buildExportPayload(
                participantId = snapshot.participantId,
                appVersion = appVersion,
                nowMs = nowMsProvider()
            )
            pendingExportPayload = payload
            val dateRange = if (payload.includedStartMs != null && payload.includedEndMs != null) {
                "${formatDateTime(payload.includedStartMs)} – ${formatDateTime(payload.includedEndMs)}"
            } else {
                "Henüz kayıtlı dönem yok"
            }
            publish(
                snapshot = snapshot,
                exportPreview = ExportPreviewUi(
                    windowCount = payload.windowCount,
                    feedbackCount = payload.feedbackCount,
                    dateRangeText = dateRange
                )
            )
        }
    }

    fun dismissExportPreview() {
        mutableUiState.value = mutableUiState.value.copy(exportPreview = null)
    }

    fun consumePendingExportZipBytes(): ByteArray? {
        val payload = pendingExportPayload ?: return null
        return ExportFormatter.toZipBytes(payload)
    }

    fun onExportCompleted(success: Boolean, detail: String? = null) {
        pendingExportPayload = null
        val snapshot = controlStore.read()
        val message = if (success) {
            "Dışa aktarım tamamlandı (UTF-8 CSV + JSON manifest, ham paket adları hariç)."
        } else {
            detail ?: "Dışa aktarım iptal edildi."
        }
        publish(
            snapshot = snapshot,
            exportPreview = null,
            notice = message
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
            "Yetki ve onay uygun. Olaylar sekmesinden okuma ve yerel kayıt yapabilirsiniz."
        } else {
            "Veri sorgusu kapalı (${resolved.status.name})."
        }
        publish(
            snapshot = snapshot,
            notice = "Kontrol #$nextCount ($timeText): $queryNote",
            manualCheckCount = nextCount
        )
        refreshStorageAndPeriods(snapshot)
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
        val lookbackMinutes = mutableUiState.value.selectedLookbackMinutes
        val startMs = nowMs - lookbackMinutes * 60_000L

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
                insertedCount = 0,
                duplicateSkippedCount = 0,
                computedWindowCount = 0,
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
        viewModelScope.launch(ioDispatcher) {
            val result = reader.queryEvents(
                startMs = startMs,
                endMs = nowMs,
                collectedAtMs = nowMs
            )
            val persistence = localDataRepository?.persistCollectionBatch(
                participantId = snapshot.participantId,
                consentVersion = snapshot.consentVersion,
                consentTimeMs = snapshot.consentAcceptedAtMs,
                collectionEnabled = resolved.canQueryUsageData,
                batch = result
            )
            if (persistence != null) {
                latestStats = persistence.stats
            }
            loadStorageAndPeriodsBlocking(snapshot)
            val insertedCount = persistence?.insertedEventCount ?: result.events.size
            val duplicateSkippedCount = persistence?.duplicateSkippedCount ?: 0
            val computedWindowCount = persistence?.computedWindowCount ?: 0
            val counts = result.events.groupingBy { it.eventType }.eachCount()
            val summary = QuerySummaryUi(
                rangeText = "${formatTime(result.queriedStartMs)} - ${formatTime(result.queriedEndMs)}",
                collectedAtText = formatTime(result.collectedAtMs),
                rawScannedCount = result.rawScannedCount,
                normalizedCount = result.events.size,
                insertedCount = insertedCount,
                duplicateSkippedCount = duplicateSkippedCount,
                computedWindowCount = computedWindowCount,
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
                "Son $lookbackMinutes dk: ${result.events.size} olay (+$insertedCount yeni), $computedWindowCount tamamlanmış pencere hesaplandı."
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
    }

    private fun refreshStorageAndPeriods(snapshot: ControlSnapshot) {
        if (localDataRepository == null) {
            return
        }
        viewModelScope.launch(ioDispatcher) {
            loadStorageAndPeriodsBlocking(snapshot)
            publish(controlStore.read())
        }
    }

    private suspend fun loadStorageAndPeriodsBlocking(snapshot: ControlSnapshot) {
        val repository = localDataRepository ?: return
        latestStats = repository.readStorageStats(snapshot.participantId)
        val windows = repository.readFeatureWindows(snapshot.participantId)
        val feedbacks = repository.readFeedbacks(snapshot.participantId)
        latestPeriods = mapPeriods(windows, feedbacks)
    }

    private fun mapPeriods(
        windows: List<FeatureWindowEntity>,
        feedbacks: List<FeedbackEntity>
    ): List<PeriodItemUi> {
        val feedbackByRange = feedbacks.associateBy { fb -> fb.windowStartMs to fb.windowEndMs }
        return windows.map { w ->
            val fb = feedbackByRange[w.startMs to w.endMs]
            val parsedAnswer = fb?.answer?.let { raw ->
                runCatching { FeedbackAnswer.valueOf(raw) }.getOrNull()
            }
            val parsedStatus = fb?.responseStatus?.let { raw ->
                runCatching { FeedbackResponseStatus.valueOf(raw) }.getOrNull()
            } ?: FeedbackResponseStatus.UNANSWERED
            PeriodItemUi(
                windowId = w.id,
                windowStartMs = w.startMs,
                windowEndMs = w.endMs,
                rangeLabel = "${formatHourMinute(w.startMs)} – ${formatHourMinute(w.endMs)}",
                dateLabel = formatDateOnly(w.startMs),
                screenText = formatDuration(w.screenSeconds),
                sessionsStarted = w.sessionsStarted,
                appSwitches = w.appSwitches,
                longestRunText = formatDuration(w.longestRunSeconds),
                qualityStatus = w.qualityStatus,
                qualityReasons = w.qualityReasons,
                feedbackAnswer = parsedAnswer,
                feedbackStatus = parsedStatus
            )
        }
    }

    private fun publish(
        snapshot: ControlSnapshot,
        notice: String? = null,
        showDeleteDialog: Boolean = mutableUiState.value.showDeleteDialog,
        exportPreview: ExportPreviewUi? = mutableUiState.value.exportPreview,
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
            exportPreview = exportPreview,
            statusNotice = notice ?: current.statusNotice,
            manualCheckCount = manualCheckCount,
            storedRawEventCount = latestStats.storedRawEventCount,
            storedBatchCount = latestStats.storedBatchCount,
            storedWindowCount = latestStats.storedWindowCount,
            storedFeedbackCount = latestStats.storedFeedbackCount,
            lastCheckpointText = latestStats.lastCheckpointMs?.let { formatDateTime(it) } ?: "Henüz kayıt yok",
            lastQuerySummary = if (clearInspector) null else querySummary,
            inspectedEvents = if (clearInspector) emptyList() else inspectedEvents,
            periodItems = latestPeriods
        )
    }

    private fun formatDuration(seconds: Double): String {
        val totalSec = seconds.roundToInt()
        val mins = totalSec / 60
        val secs = totalSec % 60
        return if (mins > 0) "${mins} dk ${secs} sn" else "${secs} sn"
    }

    private fun formatHourMinute(epochMs: Long): String {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        return formatter.format(Date(epochMs))
    }

    private fun formatDateOnly(epochMs: Long): String {
        val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return formatter.format(Date(epochMs))
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
        private val usageEventReader: UsageEventReader,
        private val localDataRepository: LocalDataRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(
                controlStore = controlStore,
                usageEventReader = usageEventReader,
                localDataRepository = localDataRepository
            ) as T
        }
    }
}
