package com.group8.wellbeing.domain

import com.group8.wellbeing.usage.UsagePermissionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionStateResolverTest {
    @Test
    fun t02_noConsentBlocksQueryEvenWhenSystemPermissionGranted() {
        val resolved = CollectionStateResolver.resolve(
            consentAccepted = false,
            usagePermission = UsagePermissionState.GRANTED,
            hadUsagePermissionPreviously = true,
            pausedByUser = false,
            errorMessage = null
        )
        assertEquals(CollectionStatus.CONSENT_PENDING, resolved.status)
        assertFalse(resolved.canQueryUsageData)
    }

    @Test
    fun t03_returningWithoutUsageAccessRemainsPermissionRequired() {
        val resolved = CollectionStateResolver.resolve(
            consentAccepted = true,
            usagePermission = UsagePermissionState.REQUIRED,
            hadUsagePermissionPreviously = false,
            pausedByUser = false,
            errorMessage = null
        )
        assertEquals(CollectionStatus.PERMISSION_REQUIRED, resolved.status)
        assertFalse(resolved.canQueryUsageData)
    }

    @Test
    fun revokedUsageAccessAfterGrantTransitionsToAccessRevoked() {
        val resolved = CollectionStateResolver.resolve(
            consentAccepted = true,
            usagePermission = UsagePermissionState.REQUIRED,
            hadUsagePermissionPreviously = true,
            pausedByUser = false,
            errorMessage = null
        )
        assertEquals(CollectionStatus.ACCESS_REVOKED, resolved.status)
        assertFalse(resolved.canQueryUsageData)
    }

    @Test
    fun activeWhenConsentAndUsageAccessGranted() {
        val resolved = CollectionStateResolver.resolve(
            consentAccepted = true,
            usagePermission = UsagePermissionState.GRANTED,
            hadUsagePermissionPreviously = true,
            pausedByUser = false,
            errorMessage = null
        )
        assertEquals(CollectionStatus.ACTIVE, resolved.status)
        assertTrue(resolved.canQueryUsageData)
    }

    @Test
    fun t22_pausedByUserBlocksQueries() {
        val resolved = CollectionStateResolver.resolve(
            consentAccepted = true,
            usagePermission = UsagePermissionState.GRANTED,
            hadUsagePermissionPreviously = true,
            pausedByUser = true,
            errorMessage = null
        )
        assertEquals(CollectionStatus.PAUSED_BY_USER, resolved.status)
        assertFalse(resolved.canQueryUsageData)
    }

    @Test
    fun explicitErrorBlocksQueries() {
        val resolved = CollectionStateResolver.resolve(
            consentAccepted = true,
            usagePermission = UsagePermissionState.GRANTED,
            hadUsagePermissionPreviously = true,
            pausedByUser = false,
            errorMessage = "API hatası"
        )
        assertEquals(CollectionStatus.ERROR, resolved.status)
        assertFalse(resolved.canQueryUsageData)
    }
}
