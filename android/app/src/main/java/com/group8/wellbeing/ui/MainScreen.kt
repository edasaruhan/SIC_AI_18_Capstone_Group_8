package com.group8.wellbeing.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import com.group8.wellbeing.domain.FeedbackAnswer
import com.group8.wellbeing.domain.FeedbackResponseStatus
import com.group8.wellbeing.usage.NormalizedEventType
import com.group8.wellbeing.usage.NotificationPermissionState
import com.group8.wellbeing.usage.UsagePermissionState
import com.group8.wellbeing.usage.UsageQueryErrorCode

@Composable
fun MainScreen(
    state: MainUiState,
    onToggleTheme: () -> Unit,
    onSelectTab: (MainTab) -> Unit,
    onSelectLookbackMinutes: (Int) -> Unit,
    onQueryRecentEvents: () -> Unit,
    onSubmitFeedback: (Long, Long, Long, FeedbackAnswer?, FeedbackResponseStatus) -> Unit,
    onRequestExportPreview: () -> Unit,
    onDismissExportPreview: () -> Unit,
    onConfirmExportZip: () -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onPauseCollection: () -> Unit,
    onResumeCollection: () -> Unit,
    onManualCheckClick: () -> Unit,
    onRequestDeleteAllData: () -> Unit,
    onDismissDeleteDialog: () -> Unit,
    onConfirmDeleteAllData: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
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
                ThemeToggleChip(
                    darkThemeEnabled = state.darkThemeEnabled,
                    onClick = onToggleTheme
                )
            }

            TabSwitcher(
                selectedTab = state.selectedTab,
                onSelectTab = onSelectTab
            )

            when (state.selectedTab) {
                MainTab.STATUS -> StatusTabContent(
                    state = state,
                    onOpenUsageAccessSettings = onOpenUsageAccessSettings,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onPauseCollection = onPauseCollection,
                    onResumeCollection = onResumeCollection,
                    onManualCheckClick = onManualCheckClick,
                    onRequestExportPreview = onRequestExportPreview,
                    onRequestDeleteAllData = onRequestDeleteAllData
                )
                MainTab.PERIODS -> PeriodsTabContent(
                    state = state,
                    onSubmitFeedback = onSubmitFeedback
                )
                MainTab.INSPECTOR -> InspectorTabContent(
                    state = state,
                    onSelectLookbackMinutes = onSelectLookbackMinutes,
                    onQueryRecentEvents = onQueryRecentEvents
                )
            }
        }
    }

    val preview = state.exportPreview
    if (preview != null) {
        AlertDialog(
            onDismissRequest = onDismissExportPreview,
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(text = "Verileri Dışa Aktar") },
            text = {
                Text(
                    text = "Kapsam: ${preview.windowCount} dönem penceresi ve ${preview.feedbackCount} geri bildirim.\n" +
                        "Tarih aralığı: ${preview.dateRangeText}.\n\n" +
                        "Dosya biçimi: UTF-8 CSV + JSON manifest (ZIP). Ham paket adları, mesaj veya kişisel bilgiler eklenmez."
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmExportZip) {
                    Text(text = "Konum Seç ve Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissExportPreview) {
                    Text(text = "Vazgeç")
                }
            }
        )
    }

    if (state.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = onDismissDeleteDialog,
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(text = "Tüm yerel veriler silinsin mi?") },
            text = {
                Text(
                    text = "Bekleyen işler ve bildirimler iptal edilir, tüm yerel kayıtlar ile katılım onayınız temizlenir ve yeni bir yerel kimlik oluşturulur. Daha önce kendi dışa aktardığınız dosyalar otomatik silinmez."
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
private fun TabSwitcher(
    selectedTab: MainTab,
    onSelectTab: (MainTab) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TabButton(
                title = "Durum",
                selected = selectedTab == MainTab.STATUS,
                modifier = Modifier.weight(1f),
                onClick = { onSelectTab(MainTab.STATUS) }
            )
            TabButton(
                title = "Dönemler",
                selected = selectedTab == MainTab.PERIODS,
                modifier = Modifier.weight(1f),
                onClick = { onSelectTab(MainTab.PERIODS) }
            )
            TabButton(
                title = "Olaylar",
                selected = selectedTab == MainTab.INSPECTOR,
                modifier = Modifier.weight(1f),
                onClick = { onSelectTab(MainTab.INSPECTOR) }
            )
        }
    }
}

@Composable
private fun TabButton(
    title: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(9.dp),
        color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun StatusTabContent(
    state: MainUiState,
    onOpenUsageAccessSettings: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onPauseCollection: () -> Unit,
    onResumeCollection: () -> Unit,
    onManualCheckClick: () -> Unit,
    onRequestExportPreview: () -> Unit,
    onRequestDeleteAllData: () -> Unit
) {
    val collectionLabel = when (state.collectionStatus) {
        CollectionStatus.CONSENT_PENDING -> "Onay bekleniyor"
        CollectionStatus.PERMISSION_REQUIRED -> "Erişim izni gerekiyor"
        CollectionStatus.ACTIVE -> "Toplama açık"
        CollectionStatus.PAUSED_BY_USER -> "Duraklatıldı"
        CollectionStatus.ACCESS_REVOKED -> "Erişim kesildi"
        CollectionStatus.ERROR -> "Hata"
    }
    val usageLabel = when (state.usagePermissionState) {
        UsagePermissionState.GRANTED -> "Açık"
        UsagePermissionState.REQUIRED -> if (state.collectionStatus == CollectionStatus.ACCESS_REVOKED) {
            "Kesildi"
        } else {
            "Gerekiyor"
        }
    }
    val notificationLabel = when (state.notificationPermissionState) {
        NotificationPermissionState.GRANTED -> "Açık"
        NotificationPermissionState.DENIED -> "Kapalı (isteğe bağlı)"
        NotificationPermissionState.NOT_REQUIRED -> "Gerekli değil"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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
                label = "Katılım onayı",
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            StatusRow(
                label = "Yerel kayıt (Room)",
                value = "${state.storedRawEventCount} olay · ${state.storedBatchCount} sorgu",
                isPositive = state.storedRawEventCount > 0
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            StatusRow(
                label = "Dönem / Geri bildirim",
                value = "${state.storedWindowCount} pencere · ${state.storedFeedbackCount} yanıt",
                isPositive = state.storedWindowCount > 0
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            StatusRow(
                label = "Arka plan işi",
                value = if (state.backgroundWorkScheduled) "Planlandı (15 dk)" else "Kapalı",
                isPositive = state.backgroundWorkScheduled
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            StatusRow(
                label = "Model durumu",
                value = "Bağlı değil (kapalı)",
                isPositive = false
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            StatusRow(
                label = "Son başarılı kayıt",
                value = state.lastCheckpointText,
                isPositive = state.lastCheckpointText != "Henüz kayıt yok"
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
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
                Text(text = "Toplamaya Devam Et")
            }
        } else {
            OutlinedButton(
                onClick = onPauseCollection,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Text(text = "Durumu Yenile")
    }

    Button(
        onClick = onRequestExportPreview,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text = "Verileri Dışa Aktar (CSV + JSON)")
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

@Composable
private fun PeriodsTabContent(
    state: MainUiState,
    onSubmitFeedback: (Long, Long, Long, FeedbackAnswer?, FeedbackResponseStatus) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = "Model henüz bağlı değil; tahmin skoru kapalı. Aşağıda tamamlanan 30 dakikalık dönemleri inceleyip kendi değerlendirmenizi kaydedebilirsiniz.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(14.dp)
        )
    }

    if (state.periodItems.isEmpty()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Text(
                text = "Henüz tamamlanmış 30 dakikalık dönem kaydı yok. Olaylar sekmesinden en az 2 saatlik aralık seçip \"Olayları Oku\" dediğinizde tamamlanan dönemler burada listelenir.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        state.periodItems.take(24).forEach { period ->
            val qualityLabel = when (period.qualityStatus) {
                "OBSERVATION_SUFFICIENT" -> "Gözlem yeterli"
                "UNCERTAIN" -> "Belirsiz"
                else -> "Geçersiz"
            }
            val feedbackBadge = when {
                period.feedbackStatus == FeedbackResponseStatus.ANSWERED && period.feedbackAnswer == FeedbackAnswer.YES -> "Yanıt: Evet"
                period.feedbackStatus == FeedbackResponseStatus.ANSWERED && period.feedbackAnswer == FeedbackAnswer.NO -> "Yanıt: Hayır"
                period.feedbackStatus == FeedbackResponseStatus.SKIPPED -> "Atlandı"
                else -> "Cevapsız"
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = period.rangeLabel,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = period.dateLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (period.qualityStatus == "OBSERVATION_SUFFICIENT") {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.tertiaryContainer
                            }
                        ) {
                            Text(
                                text = qualityLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (period.qualityStatus == "OBSERVATION_SUFFICIENT") {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "Ekran: ${period.screenText} · En uzun: ${period.longestRunText} · Oturum: ${period.sessionsStarted} · Geçiş: ${period.appSwitches}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (period.qualityReasons.isNotBlank()) {
                        Text(
                            text = "Kalite notu: ${period.qualityReasons}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${period.rangeLabel} arasındaki telefon kullanımın istediğinden uzun sürdü mü?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FeedbackChoiceChip(
                            label = "Evet",
                            selected = period.feedbackStatus == FeedbackResponseStatus.ANSWERED &&
                                period.feedbackAnswer == FeedbackAnswer.YES,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onSubmitFeedback(
                                    period.windowId,
                                    period.windowStartMs,
                                    period.windowEndMs,
                                    FeedbackAnswer.YES,
                                    FeedbackResponseStatus.ANSWERED
                                )
                            }
                        )
                        FeedbackChoiceChip(
                            label = "Hayır",
                            selected = period.feedbackStatus == FeedbackResponseStatus.ANSWERED &&
                                period.feedbackAnswer == FeedbackAnswer.NO,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onSubmitFeedback(
                                    period.windowId,
                                    period.windowStartMs,
                                    period.windowEndMs,
                                    FeedbackAnswer.NO,
                                    FeedbackResponseStatus.ANSWERED
                                )
                            }
                        )
                        FeedbackChoiceChip(
                            label = "Atla",
                            selected = period.feedbackStatus == FeedbackResponseStatus.SKIPPED,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onSubmitFeedback(
                                    period.windowId,
                                    period.windowStartMs,
                                    period.windowEndMs,
                                    null,
                                    FeedbackResponseStatus.SKIPPED
                                )
                            }
                        )
                    }

                    Text(
                        text = "Durum: $feedbackBadge",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedbackChoiceChip(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InspectorTabContent(
    state: MainUiState,
    onSelectLookbackMinutes: (Int) -> Unit,
    onQueryRecentEvents: () -> Unit
) {
    val lookbackOptions = listOf(30 to "30 dk", 120 to "2 saat", 360 to "6 saat")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Kısa Aralık Olay Okuma (M03–M04)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Seçilen aralıktaki sistem olaylarını okuyup Room veritabanına kaydeder ve tamamlanan 30 dakikalık dönemleri günceller.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                lookbackOptions.forEach { (minutes, label) ->
                    val selected = state.selectedLookbackMinutes == minutes
                    Surface(
                        onClick = { onSelectLookbackMinutes(minutes) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = BorderStroke(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 9.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onQueryRecentEvents,
                enabled = state.canQueryUsageData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Olayları Oku ve Kaydet")
            }

            if (!state.canQueryUsageData) {
                Text(
                    text = state.nextStepMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }

    val summary = state.lastQuerySummary
    if (summary != null) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatusRow(
                    label = "Sorgu aralığı",
                    value = summary.rangeText,
                    isPositive = summary.errorCode == UsageQueryErrorCode.NONE
                )
                StatusRow(
                    label = "Normalize / Ham",
                    value = "${summary.normalizedCount} / ${summary.rawScannedCount}",
                    isPositive = summary.errorCode == UsageQueryErrorCode.NONE
                )
                StatusRow(
                    label = "Yeni / Yinelenen",
                    value = "+${summary.insertedCount} yeni · ${summary.duplicateSkippedCount} atlandı",
                    isPositive = summary.errorCode == UsageQueryErrorCode.NONE
                )
                StatusRow(
                    label = "Hesaplanan dönem",
                    value = "${summary.computedWindowCount} pencere",
                    isPositive = summary.computedWindowCount > 0
                )
                if (summary.errorCode != UsageQueryErrorCode.NONE) {
                    Text(
                        text = "Durum kodu: ${summary.errorCode.name} ${summary.errorDetail ?: ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                if (summary.countsByType.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        summary.countsByType.forEach { (type, count) ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "${eventTypeShortLabel(type)}: $count",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.inspectedEvents.isNotEmpty()) {
        Text(
            text = "Okunan Normalize Olaylar (En fazla 60 kayıt)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.inspectedEvents.takeLast(60).reversed().forEach { item ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.timeText,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (item.isNonAppPackage) {
                                    MaterialTheme.colorScheme.tertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer
                                }
                            ) {
                                Text(
                                    text = eventTypeShortLabel(item.eventType),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (item.isNonAppPackage) {
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Text(
                            text = item.detailText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (item.packageToken != null) {
                            val filterNote = if (item.isNonAppPackage) " · Sistem/Klavye/Launcher" else ""
                            Text(
                                text = "${item.packageToken}$filterNote",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun eventTypeShortLabel(type: NormalizedEventType): String {
    return when (type) {
        NormalizedEventType.SCREEN_INTERACTIVE -> "SCREEN_ON"
        NormalizedEventType.KEYGUARD_HIDDEN -> "UNLOCKED"
        NormalizedEventType.KEYGUARD_SHOWN -> "LOCKED"
        NormalizedEventType.SCREEN_NON_INTERACTIVE -> "SCREEN_OFF"
        NormalizedEventType.APP_FOREGROUND -> "APP"
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
