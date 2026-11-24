package com.group8.wellbeing.ui

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.group8.wellbeing.data.RoomWellbeingRepository
import com.group8.wellbeing.data.SharedPrefsControlStore
import com.group8.wellbeing.data.local.WellbeingDatabase
import com.group8.wellbeing.ui.theme.WellbeingTheme
import com.group8.wellbeing.usage.AndroidUsageEventReader
import com.group8.wellbeing.usage.NotificationPermissionChecker
import com.group8.wellbeing.usage.UsageAccessChecker
import com.group8.wellbeing.worker.WorkManagerCollectionScheduler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(
            controlStore = SharedPrefsControlStore(applicationContext),
            usageEventReader = AndroidUsageEventReader(applicationContext),
            localDataRepository = RoomWellbeingRepository(
                WellbeingDatabase.getInstance(applicationContext)
            ),
            collectionScheduler = WorkManagerCollectionScheduler(applicationContext)
        )
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onNotificationPermissionResult(granted)
    }

    private val createExportZipLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri == null) {
            viewModel.onExportCompleted(success = false)
            return@registerForActivityResult
        }
        val bytes = viewModel.consumePendingExportZipBytes()
        if (bytes == null) {
            viewModel.onExportCompleted(success = false, detail = "Dışa aktarılacak paket bulunamadı.")
            return@registerForActivityResult
        }
        val writeResult = runCatching {
            contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(bytes)
                stream.flush()
            } ?: error("Çıkış akışı açılamadı.")
        }
        if (writeResult.isSuccess) {
            viewModel.onExportCompleted(success = true)
        } else {
            viewModel.onExportCompleted(
                success = false,
                detail = "Dışa aktarım yazılamadı: ${writeResult.exceptionOrNull()?.message ?: "Bilinmeyen hata"}"
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshPermissions()
        setContent {
            val state by viewModel.uiState.collectAsState()
            LaunchedEffect(state.darkThemeEnabled) {
                val style = if (state.darkThemeEnabled) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(
                    statusBarStyle = style,
                    navigationBarStyle = style
                )
            }
            WellbeingTheme(darkTheme = state.darkThemeEnabled) {
                if (!state.consentAccepted) {
                    ConsentScreen(
                        state = state,
                        onToggleTheme = { viewModel.toggleTheme() },
                        onAcceptConsent = { viewModel.acceptConsent() },
                        onDeclineConsent = { viewModel.declineConsent() }
                    )
                } else {
                    MainScreen(
                        state = state,
                        onToggleTheme = { viewModel.toggleTheme() },
                        onSelectTab = { viewModel.selectTab(it) },
                        onSelectLookbackMinutes = { viewModel.selectLookbackMinutes(it) },
                        onQueryRecentEvents = {
                            viewModel.queryRecentEvents(
                                usagePermission = UsageAccessChecker.check(this),
                                notificationPermission = NotificationPermissionChecker.check(this)
                            )
                        },
                        onSubmitFeedback = { windowId, startMs, endMs, answer, status ->
                            viewModel.submitFeedback(windowId, startMs, endMs, answer, status)
                        },
                        onRequestExportPreview = { viewModel.prepareExportPreview() },
                        onDismissExportPreview = { viewModel.dismissExportPreview() },
                        onConfirmExportZip = { launchExportDocumentPicker() },
                        onOpenUsageAccessSettings = { openUsageAccessSettings() },
                        onRequestNotificationPermission = { requestNotificationPermission() },
                        onPauseCollection = { viewModel.pauseCollection() },
                        onResumeCollection = { viewModel.resumeCollection() },
                        onManualCheckClick = {
                            viewModel.runManualCheck(
                                usagePermission = UsageAccessChecker.check(this),
                                notificationPermission = NotificationPermissionChecker.check(this)
                            )
                        },
                        onRequestDeleteAllData = { viewModel.requestDeleteDialog() },
                        onDismissDeleteDialog = { viewModel.dismissDeleteDialog() },
                        onConfirmDeleteAllData = { viewModel.confirmDeleteAllLocalData() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
    }

    private fun refreshPermissions() {
        viewModel.syncPermissions(
            usagePermission = UsageAccessChecker.check(this),
            notificationPermission = NotificationPermissionChecker.check(this)
        )
    }

    private fun launchExportDocumentPicker() {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        createExportZipLauncher.launch("wellbeing_export_$stamp.zip")
    }

    private fun openUsageAccessSettings() {
        val packageIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
        val fallbackIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        if (packageIntent.resolveActivity(packageManager) != null) {
            startActivity(packageIntent)
        } else {
            startActivity(fallbackIntent)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
