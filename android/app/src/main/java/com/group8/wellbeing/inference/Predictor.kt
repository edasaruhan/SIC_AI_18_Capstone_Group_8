package com.group8.wellbeing.inference

import com.group8.wellbeing.data.AppConfig
import com.group8.wellbeing.domain.WindowQualityStatus

enum class PredictionMode {
    LIVE,
    DELAYED,
    REPLAY,
    DEMO
}

enum class PredictionRejectionReason {
    MODEL_NOT_INSTALLED,
    SCHEMA_MISMATCH,
    INVALID_QUALITY_WINDOW,
    NON_FINITE_FEATURE,
    MODEL_RUNTIME_ERROR
}

data class FeatureVector(
    val screenSeconds: Double,
    val sessionsStarted: Int,
    val longestRunSeconds: Double,
    val appSwitches: Int,
    val previousSessionSeconds: Double,
    val hour: Int,
    val dayOfWeek: Int,
    val schemaVersion: String
)

data class ModelMetadata(
    val modelVersion: String,
    val expectedFeatureSchemaVersion: String,
    val preprocessingVersion: String,
    val decisionThreshold: Double,
    val isRealModel: Boolean
)

sealed interface PredictionOutcome {
    data class Success(
        val score: Double,
        val threshold: Double,
        val decision: Boolean,
        val mode: PredictionMode,
        val modelVersion: String,
        val producedAtMs: Long
    ) : PredictionOutcome

    data class Rejected(
        val reason: PredictionRejectionReason,
        val detail: String
    ) : PredictionOutcome
}

interface Predictor {
    val metadata: ModelMetadata
    val isReady: Boolean
    fun predictRawScore(vector: FeatureVector): Double
}

object DisabledPredictor : Predictor {
    override val metadata: ModelMetadata = ModelMetadata(
        modelVersion = "none",
        expectedFeatureSchemaVersion = AppConfig.FEATURE_SCHEMA_VERSION,
        preprocessingVersion = "none",
        decisionThreshold = 0.5,
        isRealModel = false
    )
    override val isReady: Boolean = false

    override fun predictRawScore(vector: FeatureVector): Double {
        throw IllegalStateException("Gerçek model henüz bağlanmadı.")
    }
}

object PredictionRunner {
    fun evaluate(
        predictor: Predictor,
        vector: FeatureVector,
        qualityStatus: WindowQualityStatus,
        targetStartMs: Long,
        producedAtMs: Long,
        isReplay: Boolean = false
    ): PredictionOutcome {
        if (!predictor.isReady) {
            return PredictionOutcome.Rejected(
                reason = PredictionRejectionReason.MODEL_NOT_INSTALLED,
                detail = "Model dosyası henüz yüklenmedi; tahmin kapalı."
            )
        }
        if (vector.schemaVersion != predictor.metadata.expectedFeatureSchemaVersion) {
            return PredictionOutcome.Rejected(
                reason = PredictionRejectionReason.SCHEMA_MISMATCH,
                detail = "Özellik şeması uyumsuz: beklenen ${predictor.metadata.expectedFeatureSchemaVersion}, gelen ${vector.schemaVersion}."
            )
        }
        if (qualityStatus != WindowQualityStatus.OBSERVATION_SUFFICIENT) {
            return PredictionOutcome.Rejected(
                reason = PredictionRejectionReason.INVALID_QUALITY_WINDOW,
                detail = "Pencere veri kalitesi tahmin için yeterli değil (${qualityStatus.name})."
            )
        }
        if (!hasValidBounds(vector)) {
            return PredictionOutcome.Rejected(
                reason = PredictionRejectionReason.NON_FINITE_FEATURE,
                detail = "Geçersiz veya aralık dışı özellik değeri."
            )
        }

        val score = try {
            predictor.predictRawScore(vector)
        } catch (_: PredictionRuntimeException) {
            return PredictionOutcome.Rejected(
                reason = PredictionRejectionReason.MODEL_RUNTIME_ERROR,
                detail = "Yerel model bu pencere için skor üretemedi."
            )
        }
        if (!score.isFinite()) {
            return PredictionOutcome.Rejected(
                reason = PredictionRejectionReason.NON_FINITE_FEATURE,
                detail = "Model sonlu olmayan skor üretti."
            )
        }

        val mode = when {
            !predictor.metadata.isRealModel -> PredictionMode.DEMO
            isReplay -> PredictionMode.REPLAY
            (producedAtMs - targetStartMs) > AppConfig.LIVE_PREDICTION_MAX_DELAY_MS -> PredictionMode.DELAYED
            else -> PredictionMode.LIVE
        }
        val threshold = predictor.metadata.decisionThreshold
        return PredictionOutcome.Success(
            score = score,
            threshold = threshold,
            decision = score >= threshold,
            mode = mode,
            modelVersion = predictor.metadata.modelVersion,
            producedAtMs = producedAtMs
        )
    }

    private fun hasValidBounds(v: FeatureVector): Boolean {
        if (!v.screenSeconds.isFinite() || v.screenSeconds < 0.0 || v.screenSeconds > 1800.0) {
            return false
        }
        if (!v.longestRunSeconds.isFinite() || v.longestRunSeconds < 0.0 || v.longestRunSeconds > v.screenSeconds + 0.001) {
            return false
        }
        if (!v.previousSessionSeconds.isFinite() || v.previousSessionSeconds < 0.0 || v.previousSessionSeconds > 10800.0) {
            return false
        }
        if (v.sessionsStarted < 0 || v.appSwitches < 0) {
            return false
        }
        if (v.hour !in 0..23 || v.dayOfWeek !in 0..6) {
            return false
        }
        return true
    }
}
