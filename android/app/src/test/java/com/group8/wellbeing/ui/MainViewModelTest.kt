package com.group8.wellbeing.ui

import com.group8.wellbeing.data.ControlSnapshot
import com.group8.wellbeing.data.ControlStore
import com.group8.wellbeing.domain.CollectionStatus
import com.group8.wellbeing.usage.NormalizedEventType
import com.group8.wellbeing.usage.NormalizedUsageEvent
import com.group8.wellbeing.usage.NotificationPermissionState
import com.group8.wellbeing.usage.UsageEventNormalizer
import com.group8.wellbeing.usage.UsageEventReader
import com.group8.wellbeing.usage.UsagePermissionState
import com.group8.wellbeing.usage.UsageQueryBatchResult
import com.group8.wellbeing.usage.UsageQueryErrorCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MainViewModelTest {
    @Test
    fun t02_declinedConsentPreventsCollectionAndReaderCalls() {
        val store = FakeControlStore()
        val reader = CountingUsageEventReader()
        val viewModel = MainViewModel(store, reader) { 1_700_000_000_000L }

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
        val viewModel = MainViewModel(store) { 1_700_000_000_000L }

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
        val viewModel = MainViewModel(store, reader) { 1_700_000_000_000L }

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
    fun t23_deleteAllLocalDataResetsConsentAndRotatesParticipantId() {
        val store = FakeControlStore()
        val viewModel = MainViewModel(store) { 1_700_000_000_000L }
        val initialParticipantId = store.read().participantId

        viewModel.acceptConsent()
        viewModel.syncPermissions(
            usagePermission = UsagePermissionState.GRANTED,
            notificationPermission = NotificationPermissionState.GRANTED
        )
        viewModel.confirmDeleteAllLocalData()

        val state = viewModel.uiState.value
        assertFalse(state.consentAccepted)
        assertEquals(CollectionStatus.CONSENT_PENDING, state.collectionStatus)
        assertFalse(state.canQueryUsageData)
        assertNotEquals(initialParticipantId, store.read().participantId)
    }

    @Test
    fun themeToggleFlipsDarkModePreference() {
        val store = FakeControlStore()
        val viewModel = MainViewModel(store) { 1_700_000_000_000L }

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
    fun m03_activeQueryPopulatesNormalizedEventsAndSummary() {
        val store = FakeControlStore()
        val reader = CountingUsageEventReader()
        val viewModel = MainViewModel(store, reader) { 1_700_000_000_000L }

        viewModel.acceptConsent()
        viewModel.syncPermissions(
            usagePermission = UsagePermissionState.GRANTED,
            notificationPermission = NotificationPermissionState.GRANTED
        )
        viewModel.queryRecentEvents(
            usagePermission = UsagePermissionState.GRANTED,
            notificationPermission = NotificationPermissionState.GRANTED
        )

        val state = viewModel.uiState.value
        assertEquals(1, reader.callCount)
        assertEquals(2, state.inspectedEvents.size)
        assertEquals(UsageQueryErrorCode.NONE, state.lastQuerySummary?.errorCode)
        assertEquals(2, state.lastQuerySummary?.normalizedCount)
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
