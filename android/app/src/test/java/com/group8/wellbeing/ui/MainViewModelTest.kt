package com.group8.wellbeing.ui

import com.group8.wellbeing.data.BatchPersistenceSummary
import com.group8.wellbeing.data.ControlSnapshot
import com.group8.wellbeing.data.ControlStore
import com.group8.wellbeing.data.EventFingerprint
import com.group8.wellbeing.data.LocalDataRepository
import com.group8.wellbeing.data.StorageStats
import com.group8.wellbeing.domain.CollectionStatus
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
import org.junit.Assert.assertTrue
import org.junit.Test

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
        private var batchCount = 0
        private var lastCheckpointMs: Long? = null
        var cleared: Boolean = false

        override suspend fun readStorageStats(participantId: String): StorageStats {
            val count = fingerprintsByParticipant[participantId]?.size ?: 0
            return StorageStats(
                storedRawEventCount = count,
                storedBatchCount = batchCount,
                lastCheckpointMs = lastCheckpointMs
            )
        }

        override suspend fun persistCollectionBatch(
            participantId: String,
            consentVersion: String?,
            consentTimeMs: Long?,
            collectionEnabled: Boolean,
            batch: UsageQueryBatchResult
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
                stats = StorageStats(
                    storedRawEventCount = set.size,
                    storedBatchCount = batchCount,
                    lastCheckpointMs = lastCheckpointMs
                )
            )
        }

        override suspend fun clearAllLocalData() {
            fingerprintsByParticipant.clear()
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
