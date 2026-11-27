package com.group8.wellbeing.inference

import android.app.Notification
import android.app.NotificationManager
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.group8.wellbeing.data.AppConfig
import com.group8.wellbeing.data.ControlSnapshot
import com.group8.wellbeing.data.RoomWellbeingRepository
import com.group8.wellbeing.data.local.WellbeingDatabase
import com.group8.wellbeing.domain.ReminderPolicy
import com.group8.wellbeing.domain.ReminderSuppressedReason
import com.group8.wellbeing.reminders.ReminderDelivery
import com.group8.wellbeing.reminders.ReminderNotifications
import com.group8.wellbeing.usage.NormalizedEventType
import com.group8.wellbeing.usage.NormalizedUsageEvent
import com.group8.wellbeing.usage.UsagePermissionState
import com.group8.wellbeing.usage.UsageQueryBatchResult
import com.group8.wellbeing.usage.UsageQueryErrorCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class ReminderDeliveryTest {
    private val app = InstrumentationRegistry.getInstrumentation().targetContext
    private val manager = requireNotNull(app.getSystemService(NotificationManager::class.java))
    private val target = Instant.parse("2026-11-26T14:30:00Z").toEpochMilli()
    private val featureStart = target - AppConfig.WINDOW_DURATION_MS

    @Test
    fun currentPositivePredictionPostsOneAndroidNotification() = runBlocking {
        withFixture(enabled = true, now = target + 60_000) { repository, database, collect ->
            collect()
            val prediction = requireNotNull(repository.readPredictionStatus(PARTICIPANT).latest)
            assertTrue(prediction.decision)
            assertEquals(PredictionMode.LIVE.name, prediction.mode)
            val first = requireNotNull(database.wellbeingDao().getReminderForPrediction(prediction.id))
            assertNotNull(first.shownAtMs)
            assertNull(first.suppressedReason)
            val notification = withTimeout(2_000) {
                var active = manager.activeNotifications.singleOrNull()
                while (active == null) {
                    delay(30)
                    active = manager.activeNotifications.singleOrNull()
                }
                active.notification
            }
            assertEquals(ReminderNotifications.CHANNEL_ID, notification.channelId)
            assertEquals(ReminderPolicy.DEFAULT_MESSAGE, notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
            assertNotNull(notification.contentIntent)
            assertNotNull(notification.deleteIntent)
            collect()
            assertEquals(first, database.wellbeingDao().getReminderForPrediction(prediction.id))
            assertEquals(1, manager.activeNotifications.size)
        }
    }

    @Test
    fun disabledAndDelayedPredictionsDoNotPost() = runBlocking {
        for ((enabled, minutes, expected) in listOf(
            Triple(false, 1, ReminderSuppressedReason.REMINDERS_DISABLED_OR_PAUSED),
            Triple(true, 6, ReminderSuppressedReason.NON_LIVE_PREDICTION)
        )) {
            withFixture(enabled, target + minutes * 60_000L) { repository, database, collect ->
                collect()
                val prediction = requireNotNull(repository.readPredictionStatus(PARTICIPANT).latest)
                assertTrue(prediction.decision)
                val reminder = requireNotNull(database.wellbeingDao().getReminderForPrediction(prediction.id))
                assertEquals(expected.name, reminder.suppressedReason)
                assertNull(reminder.shownAtMs)
                assertTrue(manager.activeNotifications.isEmpty())
            }
        }
    }

    private suspend fun withFixture(
        enabled: Boolean,
        now: Long,
        exercise: suspend (RoomWellbeingRepository, WellbeingDatabase, suspend () -> Unit) -> Unit
    ) {
        val database = Room.inMemoryDatabaseBuilder(app, WellbeingDatabase::class.java).build()
        val control = ControlSnapshot(
            participantId = PARTICIPANT, consentVersion = AppConfig.CONSENT_VERSION,
            consentAcceptedAtMs = featureStart, consentDeclined = false,
            hadUsagePermissionPreviously = true, pausedByUser = false, pausedAtMs = null,
            resumedAtMs = null, backfillPausedPeriod = false, darkThemeEnabled = true,
            remindersEnabled = enabled
        )
        val batch = UsageQueryBatchResult(
            queriedStartMs = featureStart,
            queriedEndMs = now,
            collectedAtMs = now,
            permissionState = UsagePermissionState.GRANTED,
            events = listOf(
                event(featureStart + 60_000, NormalizedEventType.KEYGUARD_HIDDEN),
                event(featureStart + 1_590_000, NormalizedEventType.SCREEN_NON_INTERACTIVE)
            ),
            rawScannedCount = 2,
            errorCode = UsageQueryErrorCode.NONE
        )
        try {
            OnnxPredictor.load(app.assets).use { predictor ->
                val delivery = ReminderDelivery(app, database, { control }, { now })
                val repository = RoomWellbeingRepository(database, { predictor }, { now }, delivery)
                exercise(repository, database) {
                    repository.persistCollectionBatch(
                        PARTICIPANT, AppConfig.CONSENT_VERSION, featureStart, true, batch, ZoneOffset.UTC
                    )
                }
            }
        } finally {
            ReminderNotifications(app).cancel()
            database.close()
        }
    }

    private fun event(time: Long, type: NormalizedEventType) = NormalizedUsageEvent(
        timestampMs = time, eventType = type, packageName = null, packageToken = null,
        className = null, isNonAppPackage = false, sourceEventCode = type.ordinal, sourceVersion = "test"
    )

    private companion object {
        const val PARTICIPANT = "reminder-test"
    }
}
