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
                nextStepMessage = "Veri toplamaya başlamak için bilgilendirilmiş onam vermeniz gerekir."
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
                    nextStepMessage = "Sistem kullanım erişimi kapatılmış. Eksik dönem sıfır kullanım sayılmaz; devam etmek için ayarlardan izni yeniden açın."
                )
            } else {
                ResolvedCollectionState(
                    status = CollectionStatus.PERMISSION_REQUIRED,
                    canQueryUsageData = false,
                    nextStepMessage = "Sistem ayarlarından Kullanım Erişimi iznini verip uygulamaya dönün."
                )
            }
        }
        if (pausedByUser) {
            return ResolvedCollectionState(
                status = CollectionStatus.PAUSED_BY_USER,
                canQueryUsageData = false,
                nextStepMessage = "Toplama duraklatıldı. Yeniden başlattığınızda duraklatılan dönem geriye dönük toplanmaz."
            )
        }
        return ResolvedCollectionState(
            status = CollectionStatus.ACTIVE,
            canQueryUsageData = true,
            nextStepMessage = "Onam ve kullanım erişimi hazır. M03 ile manuel olay okuması etkinleştirilecektir."
        )
    }
}
