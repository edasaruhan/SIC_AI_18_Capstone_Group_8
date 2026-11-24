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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.group8.wellbeing.data.SharedPrefsControlStore
import com.group8.wellbeing.ui.theme.WellbeingTheme
import com.group8.wellbeing.usage.NotificationPermissionChecker
import com.group8.wellbeing.usage.UsageAccessChecker

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(SharedPrefsControlStore(applicationContext))
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onNotificationPermissionResult(granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        refreshPermissions()
        setContent {
            WellbeingTheme {
                val state by viewModel.uiState.collectAsState()
                if (!state.consentAccepted) {
                    ConsentScreen(
                        state = state,
                        onAcceptConsent = { viewModel.acceptConsent() },
                        onDeclineConsent = { viewModel.declineConsent() }
                    )
                } else {
                    MainScreen(
                        state = state,
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
