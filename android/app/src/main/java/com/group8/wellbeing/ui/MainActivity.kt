package com.group8.wellbeing.ui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.group8.wellbeing.ui.theme.WellbeingTheme
import com.group8.wellbeing.usage.UsageAccessChecker

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        viewModel.refreshPermissionState(UsageAccessChecker.check(this))
        setContent {
            WellbeingTheme {
                val state by viewModel.uiState.collectAsState()
                MainScreen(
                    state = state,
                    onManualCheckClick = {
                        viewModel.runManualCheck(UsageAccessChecker.check(this))
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissionState(UsageAccessChecker.check(this))
    }
}
