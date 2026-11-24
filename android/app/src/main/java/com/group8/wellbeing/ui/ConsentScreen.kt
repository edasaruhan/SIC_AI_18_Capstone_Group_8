package com.group8.wellbeing.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.group8.wellbeing.data.AppConfig

@Composable
fun ConsentScreen(
    state: MainUiState,
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
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = state.appTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Bilgilendirilmiş Onam (${AppConfig.CONSENT_VERSION})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
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
                    ConsentInfoItem(
                        title = "Hangi veriler toplanır?",
                        body = "Yalnızca ekran açma/kapama, kilit açma ve uygulama geçiş metaverisi okunur. Mesaj içeriği, konum, rehber veya ekran görüntüsü toplanmaz."
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    ConsentInfoItem(
                        title = "Neden ve nerede saklanır?",
                        body = "Amaç dijital kullanım farkındalığı sağlamaktır; klinik tanı veya tedavi sunmaz. Kayıtlar yalnızca bu cihazda saklanır (ham olaylar ${AppConfig.RAW_EVENT_RETENTION_DAYS} gün, türetilmiş kayıtlar ${AppConfig.DERIVED_RETENTION_DAYS} gün)."
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    ConsentInfoItem(
                        title = "Duraklatma ve silme hakkı",
                        body = "Toplamayı istediğiniz zaman duraklatabilir veya tüm yerel verileri silebilirsiniz. Daha önce kendi dışa aktardığınız dosyalar otomatik silinmez."
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
                        text = "Onam verilmediği için hiçbir kullanım verisi sorgulanmıyor. Devam etmek isterseniz aşağıdan onam verebilirsiniz.",
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
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Onam Ver ve Devam Et")
            }

            OutlinedButton(
                onClick = onDeclineConsent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Şimdi Değil")
            }
        }
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
