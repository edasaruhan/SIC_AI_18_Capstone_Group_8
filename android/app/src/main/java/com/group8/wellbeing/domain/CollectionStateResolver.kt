package com.group8.wellbeing.domain

import com.group8.wellbeing.usage.UsagePermissionState

data class ResolvedCollectionState(
    val status: CollectionStatus,
    val canQueryUsageData: Boolean,
    val nextStepMessage: String
)

object CollectionStateResolver {
    fun resolve(
        consentAccepted: Boolean,
        usagePermission: UsagePermissionState,
        hadUsagePermissionPreviously: Boolean,
        pausedByUser: Boolean,
        errorMessage: String?
    ): ResolvedCollectionState {
        if (!consentAccepted) {
            return ResolvedCollectionState(
                status = CollectionStatus.CONSENT_PENDING,
                canQueryUsageData = false,
                nextStepMessage = "Veri toplamaya başlamak için önce katılım onayınızı vermeniz gerekiyor."
            )
        }
        if (errorMessage != null) {
            return ResolvedCollectionState(
                status = CollectionStatus.ERROR,
                canQueryUsageData = false,
                nextStepMessage = errorMessage
            )
        }
        if (usagePermission == UsagePermissionState.REQUIRED) {
            return if (hadUsagePermissionPreviously) {
                ResolvedCollectionState(
                    status = CollectionStatus.ACCESS_REVOKED,
                    canQueryUsageData = false,
                    nextStepMessage = "Sistem ayarlarından kullanım erişimi kapatılmış. Eksik kalan süre sıfır kullanım sayılmaz; devam etmek için izni yeniden açabilirsiniz."
                )
            } else {
                ResolvedCollectionState(
                    status = CollectionStatus.PERMISSION_REQUIRED,
                    canQueryUsageData = false,
                    nextStepMessage = "Aşağıdaki butonla sistem ayarlarını açıp Kullanım Erişimi iznini verdikten sonra uygulamaya dönün."
                )
            }
        }
        if (pausedByUser) {
            return ResolvedCollectionState(
                status = CollectionStatus.PAUSED_BY_USER,
                canQueryUsageData = false,
                nextStepMessage = "Veri toplama şu an duraklatıldı. Devam ettiğinizde aradaki süre geriye dönük toplanmaz."
            )
        }
        return ResolvedCollectionState(
            status = CollectionStatus.ACTIVE,
            canQueryUsageData = true,
            nextStepMessage = "Katılım onayı ve kullanım erişimi aktif. Olay İnceleyici sekmesinden son kullanım kayıtlarını okuyabilirsiniz."
        )
    }
}
