package com.group8.wellbeing.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.group8.wellbeing.domain.CollectionStatus
import com.group8.wellbeing.usage.NotificationPermissionState
import com.group8.wellbeing.usage.UsagePermissionState

@Composable
fun MainScreen(
    state: MainUiState,
    onOpenUsageAccessSettings: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onPauseCollection: () -> Unit,
    onResumeCollection: () -> Unit,
    onManualCheckClick: () -> Unit,
    onRequestDeleteAllData: () -> Unit,
    onDismissDeleteDialog: () -> Unit,
    onConfirmDeleteAllData: () -> Unit
) {
    val collectionLabel = when (state.collectionStatus) {
        CollectionStatus.CONSENT_PENDING -> "Onam bekleniyor"
        CollectionStatus.PERMISSION_REQUIRED -> "Kullanım erişimi gerekiyor"
        CollectionStatus.ACTIVE -> "Toplama açık"
        CollectionStatus.PAUSED_BY_USER -> "Kullanıcı duraklattı"
        CollectionStatus.ACCESS_REVOKED -> "Sistem erişimi kesildi"
        CollectionStatus.ERROR -> "Hata"
    }
    val usageLabel = when (state.usagePermissionState) {
        UsagePermissionState.GRANTED -> "Verildi"
        UsagePermissionState.REQUIRED -> if (state.collectionStatus == CollectionStatus.ACCESS_REVOKED) {
            "Kesildi"
        } else {
            "Gerekiyor"
        }
    }
    val notificationLabel = when (state.notificationPermissionState) {
        NotificationPermissionState.GRANTED -> "Verildi"
        NotificationPermissionState.DENIED -> "Kapalı (isteğe bağlı)"
        NotificationPermissionState.NOT_REQUIRED -> "Gerekli değil"
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = state.appTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Yerel kimlik: ${state.participantIdShort}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatusRow(
                        label = "Toplama durumu",
                        value = collectionLabel,
                        isPositive = state.collectionStatus == CollectionStatus.ACTIVE
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    StatusRow(
                        label = "Onam (${state.consentVersion ?: "-"})",
                        value = state.consentAcceptedTimeText ?: "Verildi",
                        isPositive = state.consentAccepted
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    StatusRow(
                        label = "Kullanım erişimi",
                        value = usageLabel,
                        isPositive = state.usagePermissionState == UsagePermissionState.GRANTED
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    StatusRow(
                        label = "Bildirim izni",
                        value = notificationLabel,
                        isPositive = state.notificationPermissionState != NotificationPermissionState.DENIED
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (state.canQueryUsageData) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.tertiaryContainer
                }
            ) {
                Text(
                    text = state.nextStepMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.canQueryUsageData) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    },
                    modifier = Modifier.padding(14.dp)
                )
            }

            if (state.usagePermissionState == UsagePermissionState.REQUIRED) {
                Button(
                    onClick = onOpenUsageAccessSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Kullanım Erişimi Ayarlarını Aç")
                }
            }

            if (state.notificationPermissionState == NotificationPermissionState.DENIED) {
                OutlinedButton(
                    onClick = onRequestNotificationPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Bildirim İzni İste (İsteğe Bağlı)")
                }
            }

            if (state.usagePermissionState == UsagePermissionState.GRANTED) {
                if (state.pausedByUser) {
                    Button(
                        onClick = onResumeCollection,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Toplamayı Yeniden Başlat")
                    }
                } else {
                    OutlinedButton(
                        onClick = onPauseCollection,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Toplamayı Duraklat")
                    }
                }
            }

            OutlinedButton(
                onClick = onManualCheckClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Elle Durum Denemesi")
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = state.statusNotice,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            OutlinedButton(
                onClick = onRequestDeleteAllData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
            ) {
                Text(
                    text = "Tüm Yerel Verileri Sil",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }

    if (state.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = onDismissDeleteDialog,
            title = { Text(text = "Tüm yerel veriler silinsin mi?") },
            text = {
                Text(
                    text = "Bekleyen işler ve bildirimler iptal edilir, tüm yerel kayıtlar ile onam durumu temizlenir ve yeni bir yerel katılımcı kimliği oluşturulur. Daha önce dışa aktardığınız dosyalar otomatik silinmez."
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmDeleteAllData) {
                    Text(
                        text = "Sil",
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteDialog) {
                    Text(text = "Vazgeç")
                }
            }
        )
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
    isPositive: Boolean
) {
    val badgeBg = if (isPositive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer
    }
    val badgeFg = if (isPositive) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onTertiaryContainer
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = badgeBg
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = badgeFg,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}
