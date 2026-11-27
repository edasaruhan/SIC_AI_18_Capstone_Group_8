package com.group8.wellbeing.domain

import com.group8.wellbeing.data.AppConfig
import com.group8.wellbeing.inference.PredictionMode
import java.time.Instant
import java.time.ZoneId

enum class ReminderSuppressedReason {
    NO_CONSENT,
    NO_USAGE_PERMISSION,
    REMINDERS_DISABLED_OR_PAUSED,
    NO_NOTIFICATION_PERMISSION,
    INSUFFICIENT_QUALITY,
    NON_LIVE_PREDICTION,
    NEGATIVE_PREDICTION,
    DUPLICATE_TARGET_WINDOW,
    QUIET_HOURS,
    DAILY_CAP_REACHED,
    COOLDOWN_ACTIVE,
    NOTIFICATION_POST_FAILED
}

enum class ReminderUserAction {
    OPENED,
    DISMISSED,
    SNOOZED,
    DISABLED
}

data class ReminderPolicyInput(
    val consentAccepted: Boolean,
    val usagePermissionGranted: Boolean,
    val remindersEnabled: Boolean,
    val pausedByUser: Boolean,
    val notificationPermissionGranted: Boolean,
    val windowQualityStatus: WindowQualityStatus,
    val predictionMode: PredictionMode,
    val predictionDecision: Boolean,
    val alreadyRemindedForTargetWindow: Boolean,
    val nowMs: Long,
    val zoneId: ZoneId,
    val remindersShownTodayLocal: Int,
    val lastReminderShownAtMs: Long?,
    val dailyCap: Int = AppConfig.DAILY_REMINDER_CAP,
    val cooldownMinutes: Int = AppConfig.REMINDER_COOLDOWN_MINUTES,
    val quietStartHour: Int = AppConfig.QUIET_START_HOUR,
    val quietEndHour: Int = AppConfig.QUIET_END_HOUR
)

data class ReminderDecision(
    val shouldShow: Boolean,
    val suppressedReason: ReminderSuppressedReason?,
    val messageText: String?,
    val policyVersion: String = ReminderPolicy.POLICY_VERSION
)

object ReminderPolicy {
    const val POLICY_VERSION: String = "reminder-policy-v1"
    const val DEFAULT_MESSAGE: String = "Biraz ara vermek ister misin?"

    fun isWithinQuietHours(localHour: Int, quietStartHour: Int, quietEndHour: Int): Boolean {
        return if (quietStartHour > quietEndHour) {
            localHour >= quietStartHour || localHour < quietEndHour
        } else {
            localHour in quietStartHour until quietEndHour
        }
    }

    fun evaluate(input: ReminderPolicyInput): ReminderDecision {
        if (!input.consentAccepted) {
            return suppress(ReminderSuppressedReason.NO_CONSENT)
        }
        if (!input.usagePermissionGranted) {
            return suppress(ReminderSuppressedReason.NO_USAGE_PERMISSION)
        }
        if (!input.remindersEnabled || input.pausedByUser) {
            return suppress(ReminderSuppressedReason.REMINDERS_DISABLED_OR_PAUSED)
        }
        if (!input.notificationPermissionGranted) {
            return suppress(ReminderSuppressedReason.NO_NOTIFICATION_PERMISSION)
        }
        if (input.windowQualityStatus != WindowQualityStatus.OBSERVATION_SUFFICIENT) {
            return suppress(ReminderSuppressedReason.INSUFFICIENT_QUALITY)
        }
        if (input.predictionMode != PredictionMode.LIVE) {
            return suppress(ReminderSuppressedReason.NON_LIVE_PREDICTION)
        }
        if (!input.predictionDecision) {
            return suppress(ReminderSuppressedReason.NEGATIVE_PREDICTION)
        }
        if (input.alreadyRemindedForTargetWindow) {
            return suppress(ReminderSuppressedReason.DUPLICATE_TARGET_WINDOW)
        }

        val localHour = Instant.ofEpochMilli(input.nowMs).atZone(input.zoneId).hour
        if (isWithinQuietHours(localHour, input.quietStartHour, input.quietEndHour)) {
            return suppress(ReminderSuppressedReason.QUIET_HOURS)
        }
        if (input.remindersShownTodayLocal >= input.dailyCap) {
            return suppress(ReminderSuppressedReason.DAILY_CAP_REACHED)
        }
        val lastShown = input.lastReminderShownAtMs
        val cooldownMs = input.cooldownMinutes * 60_000L
        if (lastShown != null && (input.nowMs - lastShown) < cooldownMs) {
            return suppress(ReminderSuppressedReason.COOLDOWN_ACTIVE)
        }

        return ReminderDecision(
            shouldShow = true,
            suppressedReason = null,
            messageText = DEFAULT_MESSAGE
        )
    }

    private fun suppress(reason: ReminderSuppressedReason): ReminderDecision {
        return ReminderDecision(
            shouldShow = false,
            suppressedReason = reason,
            messageText = null
        )
    }
}
