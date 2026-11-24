package com.group8.wellbeing.ui

import com.group8.wellbeing.data.ControlSnapshot
import com.group8.wellbeing.data.ControlStore
import com.group8.wellbeing.domain.CollectionStatus
import com.group8.wellbeing.usage.NotificationPermissionState
import com.group8.wellbeing.usage.UsagePermissionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MainViewModelTest {
    @Test
    fun t02_declinedConsentPreventsCollection() {
        val store = FakeControlStore()
        val viewModel = MainViewModel(store) { 1_700_000_000_000L }

        viewModel.syncPermissions(
            usagePermission = UsagePermissionState.GRANTED,
            notificationPermission = NotificationPermissionState.GRANTED
        )
        viewModel.declineConsent()

        val state = viewModel.uiState.value
        assertFalse(state.consentAccepted)
        assertTrue(state.consentDeclined)
        assertEquals(CollectionStatus.CONSENT_PENDING, state.collectionStatus)
        assertFalse(state.canQueryUsageData)
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
        val viewModel = MainViewModel(store) { 1_700_000_000_000L }

        viewModel.acceptConsent()
        viewModel.syncPermissions(
            usagePermission = UsagePermissionState.GRANTED,
            notificationPermission = NotificationPermissionState.NOT_REQUIRED
        )
        viewModel.pauseCollection()
        assertEquals(CollectionStatus.PAUSED_BY_USER, viewModel.uiState.value.collectionStatus)
        assertFalse(viewModel.uiState.value.canQueryUsageData)

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
            backfillPausedPeriod = false
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
                backfillPausedPeriod = false
            )
            return snapshot
        }
    }
}
