package com.group8.wellbeing.usage

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

enum class NotificationPermissionState {
    GRANTED,
    DENIED,
    NOT_REQUIRED
}

object NotificationPermissionChecker {
    fun check(context: Context): NotificationPermissionState {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return NotificationPermissionState.NOT_REQUIRED
        }
        val result = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        )
        return if (result == PackageManager.PERMISSION_GRANTED) {
            NotificationPermissionState.GRANTED
        } else {
            NotificationPermissionState.DENIED
        }
    }
}
