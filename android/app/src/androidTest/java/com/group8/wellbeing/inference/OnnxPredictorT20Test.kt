package com.group8.wellbeing.inference

import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.group8.wellbeing.data.AppConfig
import com.group8.wellbeing.data.RoomWellbeingRepository
import com.group8.wellbeing.data.local.WellbeingDatabase
import com.group8.wellbeing.domain.WindowQualityStatus
import com.group8.wellbeing.usage.UsagePermissionState
import com.group8.wellbeing.usage.UsageQueryBatchResult
import com.group8.wellbeing.usage.UsageQueryErrorCode
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.abs

class OnnxPredictorT20Test {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val appContext = instrumentation.targetContext
    private val targetStartMs = Instant.parse("2026-11-23T00:30:00Z").toEpochMilli()
    private val producedAtMs = targetStartMs + 1_000

    @Test
    fun t20MatchesPythonForAllGoldenSamples() {
        val golden = readGolden()
        val tolerance = golden.getDouble("scoreAbsoluteTolerance")
        val samples = golden.getJSONArray("samples")
        var maxDifference = 0.0
        var vectorCount = 0
        val timings = LongArray(100)
        OnnxPredictor.load(appContext.assets).use { predictor ->
            assertEquals(golden.getString("modelVersion"), predictor.metadata.modelVersion)
            assertEquals(0.25, predictor.metadata.decisionThreshold, 0.0)
            for (sampleIndex in 0 until samples.length()) {
                val sample = samples.getJSONObject(sampleIndex)
                val id = sample.getString("id")
                val features = sample.getJSONArray("features")
                val values = DoubleArray(features.length()) { index ->
                    if (features.isNull(index)) Double.NaN else features.getDouble(index)
                }
                val expectedScore = sample.getDouble("expectedPythonScore")
                val expectedDecision = sample.getBoolean("expectedDecision")
                val score = predictor.score(FloatArray(values.size) { values[it].toFloat() })
                maxDifference = maxOf(maxDifference, abs(score - expectedScore))
                assertEquals(id, expectedScore, score, tolerance)
                assertEquals(id, expectedDecision, score >= predictor.metadata.decisionThreshold)
                if (values.all { it.isFinite() }) {
                    val outcome = evaluate(predictor, vector(values)) as PredictionOutcome.Success
                    assertEquals(id, expectedScore, outcome.score, tolerance)
                    assertEquals(id, expectedDecision, outcome.decision)
                    assertEquals(id, PredictionMode.LIVE, outcome.mode)
                    vectorCount++
                }
            }
            val sampleInput = vector(DoubleArray(7))
            repeat(10) { predictor.predictRawScore(sampleInput) }
            for (index in timings.indices) {
                val started = SystemClock.elapsedRealtimeNanos()
                predictor.predictRawScore(sampleInput)
                timings[index] = SystemClock.elapsedRealtimeNanos() - started
            }
        }
        assertEquals(16, samples.length())
        assertEquals(8, vectorCount)
        timings.sort()
        instrumentation.sendStatus(0, Bundle().apply {
            putString("stream", "\nT20: ${samples.length()} samples, $vectorCount normal vectors, " +
                "max absolute score difference $maxDifference, 0 decision mismatches; " +
                "ABI=${Build.SUPPORTED_ABIS.first()}, API=${Build.VERSION.SDK_INT}\n" +
                "Warm CPU inference (100 calls): median ${(timings[49] + timings[50]) / 2e6} ms, " +
                "p95 ${timings[94] / 1e6} ms\n")
        })
    }

    @Test
    fun rejectsWrongSchemaAndUnusableWindowsInTheNormalRunner() {
        OnnxPredictor.load(appContext.assets).use { predictor ->
            val valid = vector(DoubleArray(7))
            val wrongSchema = evaluate(predictor, valid.copy(schemaVersion = "v0"))
                as PredictionOutcome.Rejected
            val missing = evaluate(predictor, valid.copy(screenSeconds = Double.NaN))
                as PredictionOutcome.Rejected
            val uncertain = evaluate(predictor, valid, WindowQualityStatus.UNCERTAIN)
                as PredictionOutcome.Rejected
            assertEquals(PredictionRejectionReason.SCHEMA_MISMATCH, wrongSchema.reason)
            assertEquals(PredictionRejectionReason.NON_FINITE_FEATURE, missing.reason)
            assertEquals(PredictionRejectionReason.INVALID_QUALITY_WINDOW, uncertain.reason)
        }
    }

    @Test
    fun collectionPersistsTheNextWindowScoreOnce() = runBlocking {
        val golden = readGolden()
        val noActivity = golden.getJSONArray("samples").getJSONObject(0)
        assertEquals("no_activity", noActivity.getString("id"))
        val startMs = targetStartMs - AppConfig.WINDOW_DURATION_MS
        val batch = UsageQueryBatchResult(
            queriedStartMs = startMs,
            queriedEndMs = targetStartMs,
            collectedAtMs = producedAtMs,
            permissionState = UsagePermissionState.GRANTED,
            events = emptyList(),
            rawScannedCount = 0,
            errorCode = UsageQueryErrorCode.NONE
        )
        OnnxPredictor.load(appContext.assets).use { predictor ->
            val database = Room.inMemoryDatabaseBuilder(appContext, WellbeingDatabase::class.java).build()
            try {
                val repository = RoomWellbeingRepository(database, { predictor }, { producedAtMs })
                repository.persistCollectionBatch("t20", AppConfig.CONSENT_VERSION, startMs, true, batch, ZoneOffset.UTC)
                val status = repository.readPredictionStatus("t20")
                val prediction = requireNotNull(status.latest)
                assertEquals(predictor.metadata.modelVersion, status.modelVersion)
                assertEquals(repository.readFeatureWindows("t20").single().id, prediction.windowId)
                assertEquals(targetStartMs, prediction.targetStartMs)
                assertEquals(targetStartMs + AppConfig.WINDOW_DURATION_MS, prediction.targetEndMs)
                assertEquals(PredictionMode.LIVE.name, prediction.mode)
                assertEquals(0.25, prediction.threshold, 0.0)
                assertEquals(noActivity.getBoolean("expectedDecision"), prediction.decision)
                assertEquals(noActivity.getDouble("expectedPythonScore"), prediction.score,
                    golden.getDouble("scoreAbsoluteTolerance"))

                repository.persistCollectionBatch("t20", AppConfig.CONSENT_VERSION, startMs, true, batch, ZoneOffset.UTC)
                assertEquals(status, repository.readPredictionStatus("t20"))
                assertNull(repository.readPredictionStatus("another_participant").latest)
            } finally {
                database.close()
            }
        }
    }

    private fun readGolden(): JSONObject = instrumentation.context.assets
        .open("general_model/golden_samples.json").bufferedReader().use { JSONObject(it.readText()) }

    private fun evaluate(
        predictor: Predictor,
        vector: FeatureVector,
        quality: WindowQualityStatus = WindowQualityStatus.OBSERVATION_SUFFICIENT
    ): PredictionOutcome = PredictionRunner.evaluate(predictor, vector, quality, targetStartMs, producedAtMs)

    private fun vector(values: DoubleArray): FeatureVector = FeatureVector(
        screenSeconds = values[0],
        sessionsStarted = values[1].toInt(),
        longestRunSeconds = values[2],
        appSwitches = values[3].toInt(),
        previousSessionSeconds = values[4],
        hour = values[5].toInt(),
        dayOfWeek = values[6].toInt(),
        schemaVersion = AppConfig.FEATURE_SCHEMA_VERSION
    )
}
