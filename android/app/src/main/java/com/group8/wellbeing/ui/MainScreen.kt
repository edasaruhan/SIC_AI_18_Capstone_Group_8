package com.group8.wellbeing.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.group8.wellbeing.domain.CollectionStatus
import com.group8.wellbeing.usage.UsagePermissionState

@Composable
fun MainScreen(
    state: MainUiState,
    onManualCheckClick: () -> Unit
) {
    val collectionLabel = when (state.collectionStatus) {
        CollectionStatus.CONSENT_PENDING -> "Onam bekleniyor"
        CollectionStatus.PERMISSION_REQUIRED -> "Kullanım erişimi gerekiyor"
        CollectionStatus.ACTIVE -> "Toplama açık"
        CollectionStatus.PAUSED_BY_USER -> "Kullanıcı duraklattı"
        CollectionStatus.ACCESS_REVOKED -> "Sistem erişimi kesildi"
        CollectionStatus.ERROR -> "Hata"
    }
    val permissionLabel = when (state.permissionState) {
        UsagePermissionState.GRANTED -> "Verildi"
        UsagePermissionState.REQUIRED -> "Gerekiyor"
    }
    val badgeContainerColor = when (state.permissionState) {
        UsagePermissionState.GRANTED -> MaterialTheme.colorScheme.primaryContainer
        UsagePermissionState.REQUIRED -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val badgeTextColor = when (state.permissionState) {
        UsagePermissionState.GRANTED -> MaterialTheme.colorScheme.onPrimaryContainer
        UsagePermissionState.REQUIRED -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = state.appTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )

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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Toplama durumu",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = collectionLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Kullanım erişimi",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = badgeContainerColor
                        ) {
                            Text(
                                text = permissionLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = badgeTextColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onManualCheckClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Elle Deneme")
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = state.lastManualCheckSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}
