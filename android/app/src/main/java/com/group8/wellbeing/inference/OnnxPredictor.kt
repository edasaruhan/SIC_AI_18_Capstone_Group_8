package com.group8.wellbeing.inference

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtException
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import android.content.res.AssetManager
import android.util.Log
import com.group8.wellbeing.data.AppConfig
import org.json.JSONObject
import java.security.MessageDigest

internal class PredictionRuntimeException(cause: Throwable? = null) : RuntimeException(cause)

internal class OnnxPredictor private constructor(
    private val environment: OrtEnvironment,
    private val session: OrtSession,
    override val metadata: ModelMetadata
) : Predictor, AutoCloseable {
    override val isReady: Boolean = true

    override fun predictRawScore(vector: FeatureVector): Double = score(
        floatArrayOf(
            vector.screenSeconds.toFloat(),
            vector.sessionsStarted.toFloat(),
            vector.longestRunSeconds.toFloat(),
            vector.appSwitches.toFloat(),
            vector.previousSessionSeconds.toFloat(),
            vector.hour.toFloat(),
            vector.dayOfWeek.toFloat()
        )
    )

    // The live FeatureVector is validated by PredictionRunner. This also permits NaN fixture tests.
    internal fun score(features: FloatArray): Double {
        require(features.size == featureOrder.size)
        try {
            OnnxTensor.createTensor(environment, arrayOf(features)).use { tensor ->
                session.run(mapOf(INPUT_NAME to tensor)).use { result ->
                    val score = (result[0].value as? FloatArray)?.singleOrNull()
                    if (score == null || !score.isFinite() || score !in 0f..1f) {
                        throw PredictionRuntimeException()
                    }
                    return score.toDouble()
                }
            }
        } catch (error: OrtException) {
            throw PredictionRuntimeException(error)
        }
    }

    override fun close() {
        session.close()
    }

    companion object {
        private const val ASSET_DIRECTORY = "general_model"
        private const val INPUT_NAME = "features"
        private const val OUTPUT_NAME = "positive_score"
        private val featureOrder = listOf(
            "screen_s" to "screenSeconds",
            "sessions_started" to "sessionsStarted",
            "longest_run_s" to "longestRunSeconds",
            "app_switches" to "appSwitches",
            "prev_session_s" to "previousSessionSeconds",
            "hour" to "hour",
            "dow" to "dayOfWeek"
        )

        fun load(assets: AssetManager): OnnxPredictor {
            val manifest = readJson(assets, "manifest.json")
            val schema = readJson(assets, "feature_schema.json")
            val metadata = ModelMetadata(
                modelVersion = manifest.getString("modelVersion"),
                expectedFeatureSchemaVersion = manifest.getString("expectedFeatureSchemaVersion"),
                preprocessingVersion = manifest.getString("preprocessingVersion"),
                decisionThreshold = manifest.getDouble("decisionThreshold"),
                isRealModel = manifest.getBoolean("isRealModel")
            )
            require(manifest.getInt("packageVersion") == 1 && metadata.isRealModel)
            require(metadata.expectedFeatureSchemaVersion == AppConfig.FEATURE_SCHEMA_VERSION)
            require(schema.getString("version") == metadata.expectedFeatureSchemaVersion)
            require(schema.getLong("windowSeconds") * 1_000 == AppConfig.WINDOW_DURATION_MS)
            require(metadata.modelVersion.isNotBlank() && metadata.preprocessingVersion.isNotBlank())
            require(metadata.decisionThreshold in 0.0..1.0 && manifest.getString("decisionOperator") == ">=")
            val ordered = schema.getJSONArray("orderedFeatures")
            require(ordered.length() == featureOrder.size)
            featureOrder.forEachIndexed { index, (name, androidField) ->
                val feature = ordered.getJSONObject(index)
                require(feature.getString("name") == name && feature.getString("androidField") == androidField)
            }

            require(manifest.getString("modelFile") == "model.onnx")
            val bytes = assets.open("$ASSET_DIRECTORY/model.onnx").use { it.readBytes() }
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
            require(digest == manifest.getString("modelSha256")) { "ONNX model checksum mismatch" }

            val environment = OrtEnvironment.getEnvironment()
            val session = OrtSession.SessionOptions().use { options ->
                // CPU is the default execution provider. One window has no need for extra workers.
                options.setIntraOpNumThreads(1)
                environment.createSession(bytes, options)
            }
            try {
                val input = session.inputInfo[INPUT_NAME]?.info as? TensorInfo
                val output = session.outputInfo[OUTPUT_NAME]?.info as? TensorInfo
                require(session.inputNames == setOf(INPUT_NAME) && input?.type == OnnxJavaType.FLOAT)
                require(input.shape.contentEquals(longArrayOf(-1, featureOrder.size.toLong())))
                require(session.outputNames == setOf(OUTPUT_NAME) && output?.type == OnnxJavaType.FLOAT)
                require(output.shape.contentEquals(longArrayOf(-1)))
                val embedded = session.metadata.customMetadata
                require(embedded["modelVersion"] == metadata.modelVersion)
                require(embedded["featureSchemaVersion"] == metadata.expectedFeatureSchemaVersion)
                require(embedded["preprocessingVersion"] == metadata.preprocessingVersion)
                require(embedded["decisionThreshold"]?.toDoubleOrNull() == metadata.decisionThreshold)
                return OnnxPredictor(environment, session, metadata)
            } catch (error: Exception) {
                session.close()
                throw error
            }
        }

        private fun readJson(assets: AssetManager, name: String): JSONObject =
            assets.open("$ASSET_DIRECTORY/$name").bufferedReader().use { JSONObject(it.readText()) }
    }
}

/** One shared native session per app process for foreground and WorkManager collection. */
object BundledPredictor {
    private var cached: Predictor? = null

    @Synchronized
    fun get(assets: AssetManager): Predictor {
        cached?.let { return it }
        val predictor = try {
            OnnxPredictor.load(assets)
        } catch (error: Exception) {
            Log.w("BundledPredictor", "Yerel model yüklenemedi.", error)
            DisabledPredictor
        } catch (error: LinkageError) {
            Log.w("BundledPredictor", "ONNX Runtime yüklenemedi.", error)
            DisabledPredictor
        }
        cached = predictor
        return predictor
    }
}
