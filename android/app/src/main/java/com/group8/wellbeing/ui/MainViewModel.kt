package com.group8.wellbeing.ui

import androidx.lifecycle.ViewModel
import com.group8.wellbeing.domain.CollectionStatus
import com.group8.wellbeing.usage.UsagePermissionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel : ViewModel() {
    private val mutableUiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = mutableUiState.asStateFlow()

    fun refreshPermissionState(permissionState: UsagePermissionState) {
        mutableUiState.update { current ->
            current.copy(
                permissionState = permissionState,
                collectionStatus = CollectionStatus.CONSENT_PENDING
            )
        }
    }

    fun runManualCheck(permissionState: UsagePermissionState) {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val nowText = formatter.format(Date())
        val permissionLabel = when (permissionState) {
            UsagePermissionState.GRANTED -> "Kullanım erişimi açık"
            UsagePermissionState.REQUIRED -> "Kullanım erişimi kapalı"
        }
        mutableUiState.update { current ->
            val nextCount = current.manualCheckCount + 1
            current.copy(
                permissionState = permissionState,
                manualCheckCount = nextCount,
                lastManualCheckSummary = "Deneme #$nextCount ($nowText): $permissionLabel. Onam öncesi veri sorgusu yapılmadı."
            )
        }
    }
}
