package com.group8.wellbeing.usage

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.Process

enum class UsagePermissionState {
    GRANTED,
    REQUIRED
}

object UsageAccessChecker {
    fun check(context: Context): UsagePermissionState {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return if (mode == AppOpsManager.MODE_ALLOWED) {
            UsagePermissionState.GRANTED
        } else {
            UsagePermissionState.REQUIRED
        }
    }
}
