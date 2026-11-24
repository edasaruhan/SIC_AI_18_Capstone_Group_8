package com.group8.wellbeing.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.group8.wellbeing.data.AppConfig
import com.group8.wellbeing.data.ControlSnapshot
import com.group8.wellbeing.data.ControlStore
import com.group8.wellbeing.domain.CollectionStateResolver
import com.group8.wellbeing.usage.NotificationPermissionState
import com.group8.wellbeing.usage.UsagePermissionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(
    private val controlStore: ControlStore,
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

    fun acceptConsent() {
        val snapshot = controlStore.acceptConsent(
            version = AppConfig.CONSENT_VERSION,
            acceptedAtMs = nowMsProvider()
        )
        publish(
            snapshot = snapshot,
            notice = "Bilgilendirilmiş onam kaydedildi (${AppConfig.CONSENT_VERSION})."
        )
    }

    fun declineConsent() {
        val snapshot = controlStore.declineConsent()
        publish(
            snapshot = snapshot,
            notice = "Onam verilmedi. Kullanım geçmişi sorgulanmıyor."
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
            "Bildirim izni verilmedi. Veri toplama ve geri bildirim bildirim olmadan çalışmaya devam eder."
        }
        publish(controlStore.read(), notice = notice)
    }

    fun pauseCollection() {
        val snapshot = controlStore.pauseCollection(nowMsProvider())
        publish(
            snapshot = snapshot,
            notice = "Toplama duraklatıldı. Yeni sorgular ve arka plan işleri durduruldu."
        )
    }

    fun resumeCollection() {
        val snapshot = controlStore.resumeCollection(
            resumedAtMs = nowMsProvider(),
            backfillPausedPeriod = false
        )
        publish(
            snapshot = snapshot,
            notice = "Toplama yeniden başlatıldı. Duraklatma dönemi geriye dönük toplanmayacak."
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
            notice = "Yerel kayıtlar, onam durumu ve bekleyen bildirimler temizlendi. Yeni yerel kimlik oluşturuldu; daha önce dışa aktarılan dosyalar otomatik silinmez."
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
            "Sorgulama yetkisi uygun; M03 olay okuyucu için hazır."
        } else {
            "Veri sorgusu engellendi (${resolved.status.name})."
        }
        publish(
            snapshot = snapshot,
            notice = "Deneme #$nextCount ($timeText): $queryNote",
            manualCheckCount = nextCount
        )
    }

    private fun publish(
        snapshot: ControlSnapshot,
        notice: String? = null,
        showDeleteDialog: Boolean = mutableUiState.value.showDeleteDialog,
        manualCheckCount: Int = mutableUiState.value.manualCheckCount
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
            manualCheckCount = manualCheckCount
        )
    }

    private fun formatTime(epochMs: Long): String {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return formatter.format(Date(epochMs))
    }

    private fun formatDateTime(epochMs: Long): String {
        val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return formatter.format(Date(epochMs))
    }

    class Factory(
        private val controlStore: ControlStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(controlStore) as T
        }
    }
}
