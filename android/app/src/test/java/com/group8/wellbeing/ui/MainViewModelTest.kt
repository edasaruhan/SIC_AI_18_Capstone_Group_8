package com.group8.wellbeing.ui

import com.group8.wellbeing.data.BatchPersistenceSummary
import com.group8.wellbeing.data.ControlSnapshot
import com.group8.wellbeing.data.ControlStore
import com.group8.wellbeing.data.EventFingerprint
import com.group8.wellbeing.data.ExportFormatter
import com.group8.wellbeing.data.ExportPayload
import com.group8.wellbeing.data.LocalDataRepository
import com.group8.wellbeing.data.StorageStats
import com.group8.wellbeing.data.local.FeatureWindowEntity
import com.group8.wellbeing.data.local.FeedbackEntity
import com.group8.wellbeing.domain.CollectionStatus
import com.group8.wellbeing.domain.FeedbackAnswer
import com.group8.wellbeing.domain.FeedbackRecord
import com.group8.wellbeing.domain.FeedbackResponseStatus
import com.group8.wellbeing.usage.NormalizedEventType
import com.group8.wellbeing.usage.NormalizedUsageEvent
import com.group8.wellbeing.usage.NotificationPermissionState
import com.group8.wellbeing.usage.UsageEventNormalizer
import com.group8.wellbeing.usage.UsageEventReader
import com.group8.wellbeing.usage.UsagePermissionState
import com.group8.wellbeing.usage.UsageQueryBatchResult
import com.group8.wellbeing.usage.UsageQueryErrorCode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @Test
    fun t02_declinedConsentPreventsCollectionAndReaderCalls() {
        val store = FakeControlStore()
        val reader = CountingUsageEventReader()
        val repository = InMemoryLocalDataRepository()
        val viewModel = MainViewModel(store, reader, repository, testDispatcher) { 1_700_000_000_000L }

        viewModel.syncPermissions(
            usagePermission = UsagePermissionState.GRANTED,
            notificationPermission = NotificationPermissionState.GRANTED
        )
        viewModel.declineConsent()
        viewModel.queryRecentEvents(
            usagePermission = UsagePermissionState.GRANTED,
            notificationPermission = NotificationPermissionState.GRANTED
        )

        val state = viewModel.uiState.value
        assertFalse(state.consentAccepted)
        assertTrue(state.consentDeclined)
        assertEquals(CollectionStatus.CONSENT_PENDING, state.collectionStatus)
        assertFalse(state.canQueryUsageData)
        assertEquals(0, reader.callCount)
    }

    @Test
    fun t03_and_t04_notificationDenialDoesNotBlockActiveCollection() {
        val store = FakeControlStore()
        val viewModel = MainViewModel(store, ioDispatcher = testDispatcher) { 1_700_000_000_000L }

        viewModel.acceptConsent()
        viewModel.syncPermissions(
            usagePermission = UsagePermissionState.REQUIRED,
            notificationPermission = NotificationPermissionState.DENIED
        )
        assertEquals(CollectionStatus.PERMISSION_REQUIRED, viewModel.uiState.value.collectionStatus)
        assertFalse(viewModel.uiState.value.canQueryUsageData)

        viewModel.syncPermissions(
            usagePermission = UsagePermissionState.GRANTED,
            notificationPermission = NotificationPermissionState.DENIED
        )
        viewModel.onNotificationPermissionResult(granted = false)

        val state = viewModel.uiState.value
        assertEquals(CollectionStatus.ACTIVE, state.collectionStatus)
        assertTrue(state.canQueryUsageData)
        assertEquals(NotificationPermissionState.DENIED, state.notificationPermissionState)
    }

    @Test
    fun t22_pauseAndResumeDoesNotBackfillPausedPeriodByDefault() {
        val store = FakeControlStore()
        val reader = CountingUsageEventReader()
        val repository = InMemoryLocalDataRepository()
        val viewModel = MainViewModel(store, reader, repository, testDispatcher) { 1_700_000_000_000L }

        viewModel.acceptConsent()
        viewModel.syncPermissions(
            usagePermission = UsagePermissionState.GRANTED,
            notificationPermission = NotificationPermissionState.NOT_REQUIRED
        )
        viewModel.pauseCollection()
        viewModel.queryRecentEvents(
            usagePermission = UsagePermissionState.GRANTED,
            notificationPermission = NotificationPermissionState.NOT_REQUIRED
        )
        assertEquals(CollectionStatus.PAUSED_BY_USER, viewModel.uiState.value.collectionStatus)
        assertFalse(viewModel.uiState.value.canQueryUsageData)
        assertEquals(0, reader.callCount)

        viewModel.resumeCollection()
        assertEquals(CollectionStatus.ACTIVE, viewModel.uiState.value.collectionStatus)
        assertTrue(viewModel.uiState.value.canQueryUsageData)
        assertFalse(store.read().backfillPausedPeriod)
    }

    @Test
    fun t23_deleteAllLocalDataResetsConsentRotatesIdAndClearsRepository() {
        val store = FakeControlStore()
        val reader = CountingUsageEventReader()
        val repository = InMemoryLocalDataRepository()
        val viewModel = MainViewModel(store, reader, repository, testDispatcher) { 1_700_000_000_000L }
        val initialParticipantId = store.read().participantId

        viewModel.acceptConsent()
        viewModel.syncPermissions(
            usagePermission = UsagePermissionState.GRANTED,
            notificationPermission = NotificationPermissionState.GRANTED
        )
        viewModel.queryRecentEvents(
            usagePermission = UsagePermissionState.GRANTED,
            notificationPermission = NotificationPermissionState.GRANTED
        )
        assertEquals(2, viewModel.uiState.value.storedRawEventCount)

        viewModel.confirmDeleteAllLocalData()

        val state = viewModel.uiState.value
        assertFalse(state.consentAccepted)
        assertEquals(CollectionStatus.CONSENT_PENDING, state.collectionStatus)
        assertFalse(state.canQueryUsageData)
        assertEquals(0, state.storedRawEventCount)
        assertTrue(repository.cleared)
        assertNotEquals(initialParticipantId, store.read().participantId)
    }

    @Test
    fun themeToggleFlipsDarkModePreference() {
        val store = FakeControlStore()
        val viewModel = MainViewModel(store, ioDispatcher = testDispatcher) { 1_700_000_000_000L }

        viewModel.syncPermissions(
            usagePermission = UsagePermissionState.REQUIRED,
            notificationPermission = NotificationPermissionState.NOT_REQUIRED
        )
        assertTrue(viewModel.uiState.value.darkThemeEnabled)

        viewModel.toggleTheme()
        assertFalse(viewModel.uiState.value.darkThemeEnabled)

        viewModel.toggleTheme()
        assertTrue(viewModel.uiState.value.darkThemeEnabled)
    }

    @Test
    fun t05_requeryingSameRangeDoesNotDuplicateStoredEvents() {
        val store = FakeControlStore()
        val reader = CountingUsageEventReader()
        val repository = InMemoryLocalDataRepository()
        val viewModel = MainViewModel(store, reader, repository, testDispatcher) { 1_700_000_000_000L }

        viewModel.acceptConsent()
        viewModel.syncPermissions(
            usagePermission = UsagePermissionState.GRANTED,
            notificationPermission = NotificationPermissionState.GRANTED
        )

        viewModel.queryRecentEvents(
            usagePermission = UsagePermissionState.GRANTED,
            notificationPermission = NotificationPermissionState.GRANTED
        )
        val afterFirst = viewModel.uiState.value
        assertEquals(2, afterFirst.storedRawEventCount)
        assertEquals(2, afterFirst.lastQuerySummary?.insertedCount)
        assertEquals(0, afterFirst.lastQuerySummary?.duplicateSkippedCount)

        viewModel.queryRecentEvents(
            usagePermission = UsagePermissionState.GRANTED,
            notificationPermission = NotificationPermissionState.GRANTED
        )
        val afterSecond = viewModel.uiState.value
        assertEquals(2, afterSecond.storedRawEventCount)
        assertEquals(0, afterSecond.lastQuerySummary?.insertedCount)
        assertEquals(2, afterSecond.lastQuerySummary?.duplicateSkippedCount)
    }

    @Test
    fun t17_feedbackSeparatesYesNoSkipUnansweredAndPreventsDoubleTapDuplicates() {
        val store = FakeControlStore()
        val repository = InMemoryLocalDataRepository()
        repository.seedWindow(1L, 1_795_417_200_000L, 1_795_419_000_000L)
        repository.seedWindow(2L, 1_795_419_000_000L, 1_795_420_800_000L)
        val viewModel = MainViewModel(store, null, repository, testDispatcher) { 1_795_421_000_000L }

        viewModel.acceptConsent()
        viewModel.syncPermissions(
            usagePermission = UsagePermissionState.GRANTED,
            notificationPermission = NotificationPermissionState.GRANTED
        )
        assertEquals(2, viewModel.uiState.value.periodItems.size)
        assertEquals(FeedbackResponseStatus.UNANSWERED, viewModel.uiState.value.periodItems[0].feedbackStatus)

        viewModel.submitFeedback(1L, 1_795_417_200_000L, 1_795_419_000_000L, FeedbackAnswer.YES, FeedbackResponseStatus.ANSWERED)
        viewModel.submitFeedback(1L, 1_795_417_200_000L, 1_795_419_000_000L, FeedbackAnswer.YES, FeedbackResponseStatus.ANSWERED)
        viewModel.submitFeedback(2L, 1_795_419_000_000L, 1_795_420_800_000L, null, FeedbackResponseStatus.SKIPPED)

        assertEquals(2, viewModel.uiState.value.storedFeedbackCount)
        val p1 = viewModel.uiState.value.periodItems.first { it.windowId == 1L }
        val p2 = viewModel.uiState.value.periodItems.first { it.windowId == 2L }
        assertEquals(FeedbackAnswer.YES, p1.feedbackAnswer)
        assertEquals(FeedbackResponseStatus.ANSWERED, p1.feedbackStatus)
        assertNull(p2.feedbackAnswer)
        assertEquals(FeedbackResponseStatus.SKIPPED, p2.feedbackStatus)

        viewModel.prepareExportPreview()
        assertNotNull(viewModel.uiState.value.exportPreview)
        val zipBytes = viewModel.consumePendingExportZipBytes()
        assertNotNull(zipBytes)
        assertTrue(zipBytes!!.isNotEmpty())
    }

    private class CountingUsageEventReader : UsageEventReader {
        var callCount: Int = 0

        override fun queryEvents(startMs: Long, endMs: Long, collectedAtMs: Long): UsageQueryBatchResult {
            callCount += 1
            return UsageQueryBatchResult(
                queriedStartMs = startMs,
                queriedEndMs = endMs,
                collectedAtMs = collectedAtMs,
                permissionState = UsagePermissionState.GRANTED,
                events = listOf(
                    NormalizedUsageEvent(
                        timestampMs = startMs + 1_000L,
                        eventType = NormalizedEventType.KEYGUARD_HIDDEN,
                        packageName = null,
                        packageToken = null,
                        className = null,
                        isNonAppPackage = false,
                        sourceEventCode = UsageEventNormalizer.EVENT_KEYGUARD_HIDDEN,
                        sourceVersion = UsageEventNormalizer.SOURCE_VERSION
                    ),
                    NormalizedUsageEvent(
                        timestampMs = startMs + 2_000L,
                        eventType = NormalizedEventType.APP_FOREGROUND,
                        packageName = "com.example.news",
                        packageToken = "pkg_1234567890ab",
                        className = "com.example.news.HomeActivity",
                        isNonAppPackage = false,
                        sourceEventCode = UsageEventNormalizer.EVENT_ACTIVITY_RESUMED,
                        sourceVersion = UsageEventNormalizer.SOURCE_VERSION
                    )
                ),
                rawScannedCount = 5,
                errorCode = UsageQueryErrorCode.NONE
            )
        }
    }

    private class InMemoryLocalDataRepository : LocalDataRepository {
        private val fingerprintsByParticipant = mutableMapOf<String, MutableSet<String>>()
        private val windows = mutableListOf<FeatureWindowEntity>()
        private val feedbacksByRange = mutableMapOf<Pair<Long, Long>, FeedbackEntity>()
        private var batchCount = 0
        private var lastCheckpointMs: Long? = null
        var cleared: Boolean = false

        fun seedWindow(id: Long, startMs: Long, endMs: Long) {
            windows.add(
                FeatureWindowEntity(
                    id = id,
                    participantId = "participant-0001",
                    startMs = startMs,
                    endMs = endMs,
                    timezoneId = "Europe/Istanbul",
                    offsetSeconds = 10800,
                    screenSeconds = 600.0,
                    sessionsStarted = 1,
                    longestRunSeconds = 600.0,
                    appSwitches = 2,
                    previousSessionSeconds = 0.0,
                    hour = 10,
                    dayOfWeek = 0,
                    schemaVersion = "v1",
                    revision = 1,
                    qualityStatus = "OBSERVATION_SUFFICIENT",
                    qualityReasons = "",
                    computedAtMs = endMs
                )
            )
        }

        override suspend fun readStorageStats(participantId: String): StorageStats {
            val count = fingerprintsByParticipant[participantId]?.size ?: 0
            return StorageStats(
                storedRawEventCount = count,
                storedBatchCount = batchCount,
                storedWindowCount = windows.size,
                storedFeedbackCount = feedbacksByRange.size,
                lastCheckpointMs = lastCheckpointMs
            )
        }

        override suspend fun readFeatureWindows(participantId: String): List<FeatureWindowEntity> {
            return windows.filter { it.participantId == participantId }.sortedByDescending { it.startMs }
        }

        override suspend fun readFeedbacks(participantId: String): List<FeedbackEntity> {
            return feedbacksByRange.values.filter { it.participantId == participantId }
        }

        override suspend fun persistCollectionBatch(
            participantId: String,
            consentVersion: String?,
            consentTimeMs: Long?,
            collectionEnabled: Boolean,
            batch: UsageQueryBatchResult,
            zoneId: ZoneId
        ): BatchPersistenceSummary {
            val set = fingerprintsByParticipant.getOrPut(participantId) { mutableSetOf() }
            var inserted = 0
            batch.events.forEach { event ->
                if (set.add(EventFingerprint.compute(event))) {
                    inserted += 1
                }
            }
            val skipped = batch.events.size - inserted
            batchCount += 1
            if (batch.errorCode == UsageQueryErrorCode.NONE) {
                lastCheckpointMs = maxOf(lastCheckpointMs ?: 0L, batch.queriedEndMs)
            }
            return BatchPersistenceSummary(
                insertedEventCount = inserted,
                duplicateSkippedCount = skipped,
                computedWindowCount = windows.size,
                stats = StorageStats(
                    storedRawEventCount = set.size,
                    storedBatchCount = batchCount,
                    storedWindowCount = windows.size,
                    storedFeedbackCount = feedbacksByRange.size,
                    lastCheckpointMs = lastCheckpointMs
                )
            )
        }

        override suspend fun saveFeedback(
            participantId: String,
            windowId: Long?,
            predictionId: Long?,
            windowStartMs: Long,
            windowEndMs: Long,
            answer: FeedbackAnswer?,
            responseStatus: FeedbackResponseStatus,
            nowMs: Long
        ): FeedbackEntity {
            val key = windowStartMs to windowEndMs
            val existing = feedbacksByRange[key]
            val entity = FeedbackEntity(
                id = existing?.id ?: (feedbacksByRange.size + 1L),
                participantId = participantId,
                windowId = windowId,
                predictionId = predictionId,
                windowStartMs = windowStartMs,
                windowEndMs = windowEndMs,
                answer = if (responseStatus == FeedbackResponseStatus.ANSWERED) answer?.name else null,
                responseStatus = responseStatus.name,
                askedAtMs = existing?.askedAtMs ?: nowMs,
                answeredAtMs = if (responseStatus == FeedbackResponseStatus.ANSWERED) {
                    existing?.answeredAtMs ?: nowMs
                } else {
                    null
                },
                updatedAtMs = if (existing != null) nowMs else null,
                questionVersion = FeedbackRecord.QUESTION_VERSION
            )
            feedbacksByRange[key] = entity
            return entity
        }

        override suspend fun buildExportPayload(
            participantId: String,
            appVersion: String,
            nowMs: Long
        ): ExportPayload {
            return ExportFormatter.buildPayload(
                participantId = participantId,
                appVersion = appVersion,
                createdAtMs = nowMs,
                windows = readFeatureWindows(participantId),
                feedbacks = readFeedbacks(participantId)
            )
        }

        override suspend fun clearAllLocalData() {
            fingerprintsByParticipant.clear()
            windows.clear()
            feedbacksByRange.clear()
            batchCount = 0
            lastCheckpointMs = null
            cleared = true
        }
    }

    private class FakeControlStore : ControlStore {
        private var idCounter = 1
        private var snapshot = ControlSnapshot(
            participantId = "participant-0001",
            consentVersion = null,
            consentAcceptedAtMs = null,
            consentDeclined = false,
            hadUsagePermissionPreviously = false,
            pausedByUser = false,
            pausedAtMs = null,
            resumedAtMs = null,
            backfillPausedPeriod = false,
            darkThemeEnabled = true
        )

        override fun read(): ControlSnapshot = snapshot

        override fun acceptConsent(version: String, acceptedAtMs: Long): ControlSnapshot {
            snapshot = snapshot.copy(
                consentVersion = version,
                consentAcceptedAtMs = acceptedAtMs,
                consentDeclined = false
            )
            return snapshot
        }

        override fun declineConsent(): ControlSnapshot {
            snapshot = snapshot.copy(
                consentVersion = null,
                consentAcceptedAtMs = null,
                consentDeclined = true
            )
            return snapshot
        }

        override fun markUsagePermissionSeenGranted(): ControlSnapshot {
            snapshot = snapshot.copy(hadUsagePermissionPreviously = true)
            return snapshot
        }

        override fun pauseCollection(pausedAtMs: Long): ControlSnapshot {
            snapshot = snapshot.copy(
                pausedByUser = true,
                pausedAtMs = pausedAtMs,
                backfillPausedPeriod = false
            )
            return snapshot
        }

        override fun resumeCollection(resumedAtMs: Long, backfillPausedPeriod: Boolean): ControlSnapshot {
            snapshot = snapshot.copy(
                pausedByUser = false,
                resumedAtMs = resumedAtMs,
                backfillPausedPeriod = backfillPausedPeriod
            )
            return snapshot
        }

        override fun setDarkThemeEnabled(enabled: Boolean): ControlSnapshot {
            snapshot = snapshot.copy(darkThemeEnabled = enabled)
            return snapshot
        }

        override fun deleteAllLocalData(): ControlSnapshot {
            idCounter += 1
            snapshot = ControlSnapshot(
                participantId = "participant-000$idCounter",
                consentVersion = null,
                consentAcceptedAtMs = null,
                consentDeclined = false,
                hadUsagePermissionPreviously = false,
                pausedByUser = false,
                pausedAtMs = null,
                resumedAtMs = null,
                backfillPausedPeriod = false,
                darkThemeEnabled = snapshot.darkThemeEnabled
            )
            return snapshot
        }
    }
}
