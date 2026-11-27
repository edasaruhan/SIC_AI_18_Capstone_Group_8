package com.group8.wellbeing.reminders

import android.content.Context
import androidx.room.withTransaction
import com.group8.wellbeing.data.AppConfig
import com.group8.wellbeing.data.ControlSnapshot
import com.group8.wellbeing.data.SharedPrefsControlStore
import com.group8.wellbeing.data.local.PredictionEntity
import com.group8.wellbeing.data.local.ReminderEventEntity
import com.group8.wellbeing.data.local.WellbeingDatabase
import com.group8.wellbeing.domain.ReminderPolicy
import com.group8.wellbeing.domain.ReminderPolicyInput
import com.group8.wellbeing.domain.ReminderSuppressedReason
import com.group8.wellbeing.domain.WindowQualityStatus
import com.group8.wellbeing.inference.PredictionMode
import com.group8.wellbeing.usage.UsageAccessChecker
import com.group8.wellbeing.usage.UsagePermissionState
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

class ReminderDelivery(
    context: Context,
    private val database: WellbeingDatabase,
    private val readControl: () -> ControlSnapshot = SharedPrefsControlStore(context)::read,
    private val nowMs: () -> Long = System::currentTimeMillis
) {
    private val appContext = context.applicationContext
    private val dao = database.wellbeingDao()
    private val notifications = ReminderNotifications(appContext)

    fun remindersEnabled(participantId: String): Boolean = readControl().let {
        it.participantId == participantId && it.remindersEnabled
    }

    suspend fun deliver(participantId: String, prediction: PredictionEntity, zoneId: ZoneId) {
        deliveryMutex.withLock {
            val reservation = reserve(participantId, prediction, zoneId) ?: return@withLock
            val posted = notifications.post(reservation.id, reservation.message, prediction.targetEndMs - nowMs()) {
                val current = readControl()
                current.participantId == participantId && consentAllows(current, prediction) &&
                    current.remindersEnabled && !current.pausedByUser &&
                    usageGranted() && isFresh(prediction, nowMs())
            }
            withContext(NonCancellable) {
                dao.completeReminderAttempt(
                    reservation.id,
                    shownAtMs = if (posted) nowMs() else null,
                    reason = if (posted) null else ReminderSuppressedReason.NOTIFICATION_POST_FAILED.name
                )
            }
        }
    }

    private suspend fun reserve(participantId: String, prediction: PredictionEntity, zoneId: ZoneId): Reservation? =
        database.withTransaction {
            val control = readControl()
            if (control.participantId != participantId || !dao.ownsPrediction(participantId, prediction.id) ||
                dao.hasReminderAttempt(prediction.id)
            ) {
                return@withTransaction null
            }
            val now = nowMs()
            val dayStart = Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate().atStartOfDay(zoneId)
            val dayStartMs = dayStart.toInstant().toEpochMilli()
            val dayEndMs = dayStart.toLocalDate().plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val recent = dao.getRecentReminderReservations(
                participantId, prediction.targetStartMs,
                minOf(dayStartMs, now - AppConfig.REMINDER_COOLDOWN_MINUTES * 60_000L)
            )
            val recentTimes = recent.map { it.shownAtMs ?: it.scheduledAtMs }
            val evaluation = ReminderPolicy.evaluate(
                ReminderPolicyInput(
                    consentAccepted = consentAllows(control, prediction),
                    usagePermissionGranted = usageGranted(),
                    remindersEnabled = control.remindersEnabled,
                    pausedByUser = control.pausedByUser,
                    notificationPermissionGranted = notifications.canPost(),
                    windowQualityStatus = WindowQualityStatus.valueOf(prediction.qualityStatus),
                    predictionMode = if (isFresh(prediction, now)) PredictionMode.LIVE else PredictionMode.DELAYED,
                    predictionDecision = prediction.decision,
                    alreadyRemindedForTargetWindow = recent.any { it.targetStartMs == prediction.targetStartMs },
                    nowMs = now,
                    zoneId = zoneId,
                    remindersShownTodayLocal = recentTimes.count { it in dayStartMs until dayEndMs },
                    lastReminderShownAtMs = recentTimes.maxOrNull()
                )
            )
            // Reserve before posting. A partial attempt still counts toward the cap and cooldown.
            val eventId = dao.insertReminderEvent(
                ReminderEventEntity(
                    predictionId = prediction.id,
                    targetStartMs = prediction.targetStartMs,
                    scheduledAtMs = now,
                    shownAtMs = null,
                    suppressedReason = evaluation.suppressedReason?.name,
                    userAction = null,
                    policyVersion = evaluation.policyVersion
                )
            )
            if (evaluation.shouldShow) Reservation(eventId, requireNotNull(evaluation.messageText)) else null
        }

    private fun consentAllows(control: ControlSnapshot, prediction: PredictionEntity): Boolean =
        control.isConsentAccepted && (control.consentAcceptedAtMs ?: Long.MAX_VALUE) <= prediction.targetStartMs

    private fun isFresh(prediction: PredictionEntity, now: Long): Boolean =
        prediction.mode == PredictionMode.LIVE.name && now < prediction.targetEndMs &&
            now - prediction.targetStartMs in 0L..AppConfig.LIVE_PREDICTION_MAX_DELAY_MS

    private fun usageGranted(): Boolean = UsageAccessChecker.check(appContext) == UsagePermissionState.GRANTED

    private data class Reservation(val id: Long, val message: String)

    private companion object {
        val deliveryMutex = Mutex()
    }
}
