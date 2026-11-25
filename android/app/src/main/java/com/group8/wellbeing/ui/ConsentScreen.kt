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
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.group8.wellbeing.data.AppConfig

@Composable
fun ConsentScreen(
    state: MainUiState,
    onToggleTheme: () -> Unit,
    onAcceptConsent: () -> Unit,
    onDeclineConsent: () -> Unit
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
                        text = "Katılım Onayı · ${AppConfig.CONSENT_VERSION}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                ThemeToggleChip(
                    darkThemeEnabled = state.darkThemeEnabled,
                    onClick = onToggleTheme
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ConsentInfoItem(
                        title = "Hangi veriler okunur?",
                        body = "Yalnızca ekran açma/kapama, kilit açma ve uygulama geçiş bilgileri okunur. Mesaj içerikleri, konum, rehber veya ekran görüntüsü kesinlikle toplanmaz."
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    ConsentInfoItem(
                        title = "Amaç ve saklama süresi",
                        body = "Bu uygulama dijital kullanım farkındalığı içindir; tıbbi tanı veya tedavi sunmaz. Tüm kayıtlar yalnızca bu cihazda tutulur (ham olaylar ${AppConfig.RAW_EVENT_RETENTION_DAYS} gün, özet kayıtlar ${AppConfig.DERIVED_RETENTION_DAYS} gün)."
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    ConsentInfoItem(
                        title = "Duraklatma ve silme",
                        body = "İstediğiniz anda veri toplamayı duraklatabilir veya tüm yerel kayıtları silebilirsiniz. Daha önce kendi dışa aktardığınız dosyalar otomatik silinmez."
                    )
                }
            }

            if (state.consentDeclined) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = "Katılım onayı verilmediği için hiçbir kullanım verisi okunmuyor. Hazır olduğunuzda aşağıdan onay vererek devam edebilirsiniz.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            Button(
                onClick = onAcceptConsent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Onaylıyorum ve Devam Et")
            }

            OutlinedButton(
                onClick = onDeclineConsent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text(text = "Şimdi Değil")
            }
        }
    }
}

@Composable
fun ThemeToggleChip(
    darkThemeEnabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Text(
            text = if (darkThemeEnabled) "Açık Tema" else "Koyu Tema",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun ConsentInfoItem(
    title: String,
    body: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
