package com.group8.wellbeing.data

import com.group8.wellbeing.data.local.FeatureWindowEntity
import com.group8.wellbeing.data.local.FeedbackEntity
import com.group8.wellbeing.domain.FeedbackAnswer
import com.group8.wellbeing.domain.FeedbackRecord
import com.group8.wellbeing.domain.FeedbackResponseStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

class ExportFormatterTest {
    @Test
    fun t24_roundTripZipExportPreservesSchemaUnitsNullsAndFeedbackDistinction() {
        val windows = listOf(
            FeatureWindowEntity(
                id = 10L,
                participantId = "p-test-01",
                startMs = 1_795_417_200_000L,
                endMs = 1_795_419_000_000L,
                timezoneId = "Europe/Istanbul",
                offsetSeconds = 10800,
                screenSeconds = 600.25,
                sessionsStarted = 2,
                longestRunSeconds = 420.5,
                appSwitches = 4,
                previousSessionSeconds = 120.0,
                hour = 10,
                dayOfWeek = 0,
                schemaVersion = "v1",
                revision = 1,
                qualityStatus = "OBSERVATION_SUFFICIENT",
                qualityReasons = "",
                computedAtMs = 1_795_419_010_000L
            )
        )
        val feedbacks = listOf(
            FeedbackEntity(
                id = 1L,
                participantId = "p-test-01",
                windowId = 10L,
                predictionId = null,
                windowStartMs = 1_795_417_200_000L,
                windowEndMs = 1_795_419_000_000L,
                answer = FeedbackAnswer.NO.name,
                responseStatus = FeedbackResponseStatus.ANSWERED.name,
                askedAtMs = 1_795_419_100_000L,
                answeredAtMs = 1_795_419_120_000L,
                updatedAtMs = null,
                questionVersion = FeedbackRecord.QUESTION_VERSION
            ),
            FeedbackEntity(
                id = 2L,
                participantId = "p-test-01",
                windowId = 11L,
                predictionId = null,
                windowStartMs = 1_795_419_000_000L,
                windowEndMs = 1_795_420_800_000L,
                answer = null,
                responseStatus = FeedbackResponseStatus.SKIPPED.name,
                askedAtMs = 1_795_420_900_000L,
                answeredAtMs = null,
                updatedAtMs = null,
                questionVersion = FeedbackRecord.QUESTION_VERSION
            )
        )

        val payload = ExportFormatter.buildPayload(
            participantId = "p-test-01",
            appVersion = "0.6.0-m09-m13",
            createdAtMs = 1_795_421_000_000L,
            windows = windows,
            feedbacks = feedbacks
        )
        val zipBytes = ExportFormatter.toZipBytes(payload)

        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entries[entry.name] = zis.readBytes().toString(Charsets.UTF_8)
                entry = zis.nextEntry
            }
        }

        assertEquals(setOf("manifest.json", "feature_windows.csv", "feedbacks.csv"), entries.keys)

        val manifest = entries.getValue("manifest.json")
        assertTrue(manifest.contains("\"schema_version\": 1"))
        assertTrue(manifest.contains("\"feature_version\": \"v1\""))
        assertTrue(manifest.contains("\"time_unit\": \"utc_epoch_ms\""))
        assertTrue(manifest.contains("\"contains_raw_package_names\": false"))
        assertTrue(manifest.contains("\"window_count\": 1"))
        assertTrue(manifest.contains("\"feedback_count\": 2"))

        val windowLines = entries.getValue("feature_windows.csv").trim().lines()
        assertEquals(2, windowLines.size)
        assertEquals(ExportFormatter.FEATURE_WINDOWS_HEADER, windowLines[0])
        assertTrue(windowLines[1].contains("600.250,2,420.500,4,120.000,10,0"))
        assertFalse(windowLines[1].contains("com."))

        val feedbackLines = entries.getValue("feedbacks.csv").trim().lines()
        assertEquals(3, feedbackLines.size)
        assertEquals(ExportFormatter.FEEDBACKS_HEADER, feedbackLines[0])
        val rowNo = feedbackLines[1].split(",")
        val rowSkipped = feedbackLines[2].split(",")
        assertEquals("NO", rowNo[5])
        assertEquals("ANSWERED", rowNo[6])
        assertEquals("", rowSkipped[5])
        assertEquals("SKIPPED", rowSkipped[6])
    }
}
