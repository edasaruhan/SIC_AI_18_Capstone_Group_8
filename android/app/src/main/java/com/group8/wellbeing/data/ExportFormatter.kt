package com.group8.wellbeing.data

import com.group8.wellbeing.data.local.FeatureWindowEntity
import com.group8.wellbeing.data.local.FeedbackEntity
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ExportPayload(
    val manifestJson: String,
    val featureWindowsCsv: String,
    val feedbacksCsv: String,
    val windowCount: Int,
    val feedbackCount: Int,
    val includedStartMs: Long?,
    val includedEndMs: Long?
)

object ExportFormatter {
    const val FEATURE_WINDOWS_HEADER: String =
        "participant_id,window_start_ms,window_end_ms,timezone_id,offset_seconds,screen_s,sessions_started,longest_run_s,app_switches,prev_session_s,hour,dow,quality_status,quality_reasons,feature_version"

    const val FEEDBACKS_HEADER: String =
        "participant_id,window_id,prediction_id,window_start_ms,window_end_ms,answer,response_status,asked_at_ms,answered_at_ms,updated_at_ms,question_version"

    fun buildPayload(
        participantId: String,
        appVersion: String,
        createdAtMs: Long,
        windows: List<FeatureWindowEntity>,
        feedbacks: List<FeedbackEntity>
    ): ExportPayload {
        val sortedWindows = windows.sortedBy { it.startMs }
        val sortedFeedbacks = feedbacks.sortedBy { it.windowStartMs }

        val minStart = listOfNotNull(
            sortedWindows.firstOrNull()?.startMs,
            sortedFeedbacks.firstOrNull()?.windowStartMs
        ).minOrNull()
        val maxEnd = listOfNotNull(
            sortedWindows.lastOrNull()?.endMs,
            sortedFeedbacks.lastOrNull()?.windowEndMs
        ).maxOrNull()

        val windowsCsv = buildString {
            appendLine(FEATURE_WINDOWS_HEADER)
            sortedWindows.forEach { w ->
                append(escapeCsv(w.participantId)).append(',')
                append(w.startMs).append(',')
                append(w.endMs).append(',')
                append(escapeCsv(w.timezoneId)).append(',')
                append(w.offsetSeconds).append(',')
                append(formatDouble(w.screenSeconds)).append(',')
                append(w.sessionsStarted).append(',')
                append(formatDouble(w.longestRunSeconds)).append(',')
                append(w.appSwitches).append(',')
                append(formatDouble(w.previousSessionSeconds)).append(',')
                append(w.hour).append(',')
                append(w.dayOfWeek).append(',')
                append(escapeCsv(w.qualityStatus)).append(',')
                append(escapeCsv(w.qualityReasons)).append(',')
                appendLine(escapeCsv(w.schemaVersion))
            }
        }

        val feedbacksCsv = buildString {
            appendLine(FEEDBACKS_HEADER)
            sortedFeedbacks.forEach { fb ->
                append(escapeCsv(fb.participantId)).append(',')
                append(fb.windowId?.toString() ?: "").append(',')
                append(fb.predictionId?.toString() ?: "").append(',')
                append(fb.windowStartMs).append(',')
                append(fb.windowEndMs).append(',')
                append(escapeCsv(fb.answer ?: "")).append(',')
                append(escapeCsv(fb.responseStatus)).append(',')
                append(fb.askedAtMs).append(',')
                append(fb.answeredAtMs?.toString() ?: "").append(',')
                append(fb.updatedAtMs?.toString() ?: "").append(',')
                appendLine(escapeCsv(fb.questionVersion))
            }
        }

        val manifestJson = buildString {
            appendLine("{")
            appendLine("  \"schema_version\": ${AppConfig.APP_SCHEMA_VERSION},")
            appendLine("  \"app_version\": \"${escapeJson(appVersion)}\",")
            appendLine("  \"feature_version\": \"${AppConfig.FEATURE_SCHEMA_VERSION}\",")
            appendLine("  \"time_unit\": \"utc_epoch_ms\",")
            appendLine("  \"timezone_rule\": \"local_zone_id_with_per_window_offset_seconds\",")
            appendLine("  \"decimal_separator\": \".\",")
            appendLine("  \"null_representation\": \"\",")
            appendLine("  \"contains_raw_package_names\": false,")
            appendLine("  \"created_at_ms\": $createdAtMs,")
            appendLine("  \"participant_id\": \"${escapeJson(participantId)}\",")
            appendLine("  \"window_count\": ${sortedWindows.size},")
            appendLine("  \"feedback_count\": ${sortedFeedbacks.size},")
            appendLine("  \"included_start_ms\": ${minStart?.toString() ?: "null"},")
            appendLine("  \"included_end_ms\": ${maxEnd?.toString() ?: "null"}")
            append("}")
        }

        return ExportPayload(
            manifestJson = manifestJson,
            featureWindowsCsv = windowsCsv,
            feedbacksCsv = feedbacksCsv,
            windowCount = sortedWindows.size,
            feedbackCount = sortedFeedbacks.size,
            includedStartMs = minStart,
            includedEndMs = maxEnd
        )
    }

    fun toZipBytes(payload: ExportPayload): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            writeZipEntry(zip, "manifest.json", payload.manifestJson)
            writeZipEntry(zip, "feature_windows.csv", payload.featureWindowsCsv)
            writeZipEntry(zip, "feedbacks.csv", payload.feedbacksCsv)
        }
        return output.toByteArray()
    }

    private fun writeZipEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun formatDouble(value: Double): String {
        return String.format(Locale.US, "%.3f", value)
    }

    private fun escapeCsv(value: String): String {
        if (!value.contains(',') && !value.contains('"') && !value.contains('\n')) {
            return value
        }
        return "\"${value.replace("\"", "\"\"")}\""
    }

    private fun escapeJson(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
    }
}
