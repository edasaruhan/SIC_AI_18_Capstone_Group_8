package com.group8.wellbeing.domain

import com.group8.wellbeing.inference.DisabledPredictor
import com.group8.wellbeing.inference.FeatureVector
import com.group8.wellbeing.inference.ModelMetadata
import com.group8.wellbeing.inference.PredictionMode
import com.group8.wellbeing.inference.PredictionOutcome
import com.group8.wellbeing.inference.PredictionRejectionReason
import com.group8.wellbeing.inference.PredictionRunner
import com.group8.wellbeing.inference.Predictor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderAndInferencePolicyTest {
    private val istanbul = ZoneId.of("Europe/Istanbul")

    private fun ms(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, istanbul)
            .toInstant()
            .toEpochMilli()
    }

    private fun validVector(schemaVersion: String = "v1"): FeatureVector {
        return FeatureVector(
            screenSeconds = 600.0,
            sessionsStarted = 2,
            longestRunSeconds = 400.0,
            appSwitches = 3,
            previousSessionSeconds = 120.0,
            hour = 14,
            dayOfWeek = 1,
            schemaVersion = schemaVersion
        )
    }

    private class StubRealPredictor(
        private val scoreToReturn: Double = 0.82
    ) : Predictor {
        override val metadata: ModelMetadata = ModelMetadata(
            modelVersion = "xgb-v1",
            expectedFeatureSchemaVersion = "v1",
            preprocessingVersion = "prep-v1",
            decisionThreshold = 0.30,
            isRealModel = true
        )
        override val isReady: Boolean = true
        override fun predictRawScore(vector: FeatureVector): Double = scoreToReturn
    }

    @Test
    fun disabledPredictorRejectsWhenNoRealModelInstalled() {
        val targetStart = ms(2026, 11, 24, 14, 30)
        val outcome = PredictionRunner.evaluate(
            predictor = DisabledPredictor,
            vector = validVector(),
            qualityStatus = WindowQualityStatus.OBSERVATION_SUFFICIENT,
            targetStartMs = targetStart,
            producedAtMs = targetStart + 10_000L
        )
        assertTrue(outcome is PredictionOutcome.Rejected)
        assertEquals(
            PredictionRejectionReason.MODEL_NOT_INSTALLED,
            (outcome as PredictionOutcome.Rejected).reason
        )
    }

    @Test
    fun t19_schemaMismatchIsRejectedExplicitly() {
        val targetStart = ms(2026, 11, 24, 14, 30)
        val outcome = PredictionRunner.evaluate(
            predictor = StubRealPredictor(),
            vector = validVector(schemaVersion = "v2"),
            qualityStatus = WindowQualityStatus.OBSERVATION_SUFFICIENT,
            targetStartMs = targetStart,
            producedAtMs = targetStart + 10_000L
        )
        assertTrue(outcome is PredictionOutcome.Rejected)
        assertEquals(
            PredictionRejectionReason.SCHEMA_MISMATCH,
            (outcome as PredictionOutcome.Rejected).reason
        )
    }

    @Test
    fun t15_delayedPredictionMarksDelayedModeAndSuppressesLiveReminder() {
        val targetStart = ms(2026, 11, 24, 14, 30)
        val twoHoursLater = targetStart + 7_200_000L
        val outcome = PredictionRunner.evaluate(
            predictor = StubRealPredictor(),
            vector = validVector(),
            qualityStatus = WindowQualityStatus.OBSERVATION_SUFFICIENT,
            targetStartMs = targetStart,
            producedAtMs = twoHoursLater
        )
        assertTrue(outcome is PredictionOutcome.Success)
        val success = outcome as PredictionOutcome.Success
        assertEquals(PredictionMode.DELAYED, success.mode)
        assertTrue(success.decision)

        val reminder = ReminderPolicy.evaluate(
            basePolicyInput(nowMs = twoHoursLater).copy(predictionMode = success.mode)
        )
        assertFalse(reminder.shouldShow)
        assertEquals(ReminderSuppressedReason.NON_LIVE_PREDICTION, reminder.suppressedReason)
    }

    @Test
    fun t21_quietHoursMidnightWrapDailyCapCooldownAndDuplicateSuppression() {
        val quietNight = ReminderPolicy.evaluate(basePolicyInput(nowMs = ms(2026, 11, 24, 23, 30)))
        assertFalse(quietNight.shouldShow)
        assertEquals(ReminderSuppressedReason.QUIET_HOURS, quietNight.suppressedReason)

        val quietMorning = ReminderPolicy.evaluate(basePolicyInput(nowMs = ms(2026, 11, 25, 7, 59)))
        assertFalse(quietMorning.shouldShow)
        assertEquals(ReminderSuppressedReason.QUIET_HOURS, quietMorning.suppressedReason)

        val allowedMorning = ReminderPolicy.evaluate(basePolicyInput(nowMs = ms(2026, 11, 25, 8, 0)))
        assertTrue(allowedMorning.shouldShow)
        assertNotNull(allowedMorning.messageText)

        val capReached = ReminderPolicy.evaluate(
            basePolicyInput(nowMs = ms(2026, 11, 24, 15, 0)).copy(remindersShownTodayLocal = 3)
        )
        assertFalse(capReached.shouldShow)
        assertEquals(ReminderSuppressedReason.DAILY_CAP_REACHED, capReached.suppressedReason)

        val now = ms(2026, 11, 24, 15, 0)
        val cooldownActive = ReminderPolicy.evaluate(
            basePolicyInput(nowMs = now).copy(lastReminderShownAtMs = now - 59 * 60_000L)
        )
        assertFalse(cooldownActive.shouldShow)
        assertEquals(ReminderSuppressedReason.COOLDOWN_ACTIVE, cooldownActive.suppressedReason)

        val cooldownElapsed = ReminderPolicy.evaluate(
            basePolicyInput(nowMs = now).copy(lastReminderShownAtMs = now - 60 * 60_000L)
        )
        assertTrue(cooldownElapsed.shouldShow)

        val duplicateTarget = ReminderPolicy.evaluate(
            basePolicyInput(nowMs = now).copy(alreadyRemindedForTargetWindow = true)
        )
        assertFalse(duplicateTarget.shouldShow)
        assertEquals(ReminderSuppressedReason.DUPLICATE_TARGET_WINDOW, duplicateTarget.suppressedReason)
    }

    @Test
    fun t04_and_t18_notificationPermissionSuppressionAndSeparateReminderAction() {
        val noPerm = ReminderPolicy.evaluate(
            basePolicyInput(nowMs = ms(2026, 11, 24, 15, 0)).copy(notificationPermissionGranted = false)
        )
        assertFalse(noPerm.shouldShow)
        assertEquals(ReminderSuppressedReason.NO_NOTIFICATION_PERMISSION, noPerm.suppressedReason)
        assertTrue(ReminderUserAction.DISMISSED.name != FeedbackAnswer.NO.name)
    }

    private fun basePolicyInput(nowMs: Long): ReminderPolicyInput {
        return ReminderPolicyInput(
            consentAccepted = true,
            usagePermissionGranted = true,
            remindersEnabled = true,
            pausedByUser = false,
            notificationPermissionGranted = true,
            windowQualityStatus = WindowQualityStatus.OBSERVATION_SUFFICIENT,
            predictionMode = PredictionMode.LIVE,
            predictionDecision = true,
            alreadyRemindedForTargetWindow = false,
            nowMs = nowMs,
            zoneId = istanbul,
            remindersShownTodayLocal = 0,
            lastReminderShownAtMs = null
        )
    }
}
