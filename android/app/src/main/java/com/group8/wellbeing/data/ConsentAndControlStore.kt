package com.group8.wellbeing.data

import android.app.NotificationManager
import android.content.Context
import java.util.UUID

data class ControlSnapshot(
    val participantId: String,
    val consentVersion: String?,
    val consentAcceptedAtMs: Long?,
    val consentDeclined: Boolean,
    val hadUsagePermissionPreviously: Boolean,
    val pausedByUser: Boolean,
    val pausedAtMs: Long?,
    val resumedAtMs: Long?,
    val backfillPausedPeriod: Boolean,
    val darkThemeEnabled: Boolean
) {
    val isConsentAccepted: Boolean
        get() = consentVersion != null && consentAcceptedAtMs != null && !consentDeclined
}

interface ControlStore {
    fun read(): ControlSnapshot
    fun acceptConsent(version: String, acceptedAtMs: Long): ControlSnapshot
    fun declineConsent(): ControlSnapshot
    fun markUsagePermissionSeenGranted(): ControlSnapshot
    fun pauseCollection(pausedAtMs: Long): ControlSnapshot
    fun resumeCollection(resumedAtMs: Long, backfillPausedPeriod: Boolean = false): ControlSnapshot
    fun setDarkThemeEnabled(enabled: Boolean): ControlSnapshot
    fun deleteAllLocalData(): ControlSnapshot
}

class SharedPrefsControlStore(context: Context) : ControlStore {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun read(): ControlSnapshot {
        val participantId = ensureParticipantId()
        val version = prefs.getString(KEY_CONSENT_VERSION, null)
        val acceptedAt = if (prefs.contains(KEY_CONSENT_ACCEPTED_AT)) {
            prefs.getLong(KEY_CONSENT_ACCEPTED_AT, 0L)
        } else {
            null
        }
        val pausedAt = if (prefs.contains(KEY_PAUSED_AT)) {
            prefs.getLong(KEY_PAUSED_AT, 0L)
        } else {
            null
        }
        val resumedAt = if (prefs.contains(KEY_RESUMED_AT)) {
            prefs.getLong(KEY_RESUMED_AT, 0L)
        } else {
            null
        }
        return ControlSnapshot(
            participantId = participantId,
            consentVersion = version,
            consentAcceptedAtMs = acceptedAt,
            consentDeclined = prefs.getBoolean(KEY_CONSENT_DECLINED, false),
            hadUsagePermissionPreviously = prefs.getBoolean(KEY_HAD_USAGE_PERMISSION, false),
            pausedByUser = prefs.getBoolean(KEY_PAUSED_BY_USER, false),
            pausedAtMs = pausedAt,
            resumedAtMs = resumedAt,
            backfillPausedPeriod = prefs.getBoolean(KEY_BACKFILL_PAUSED, false),
            darkThemeEnabled = prefs.getBoolean(KEY_DARK_THEME, true)
        )
    }

    override fun acceptConsent(version: String, acceptedAtMs: Long): ControlSnapshot {
        ensureParticipantId()
        prefs.edit()
            .putString(KEY_CONSENT_VERSION, version)
            .putLong(KEY_CONSENT_ACCEPTED_AT, acceptedAtMs)
            .putBoolean(KEY_CONSENT_DECLINED, false)
            .apply()
        return read()
    }

    override fun declineConsent(): ControlSnapshot {
        ensureParticipantId()
        prefs.edit()
            .remove(KEY_CONSENT_VERSION)
            .remove(KEY_CONSENT_ACCEPTED_AT)
            .putBoolean(KEY_CONSENT_DECLINED, true)
            .apply()
        return read()
    }

    override fun markUsagePermissionSeenGranted(): ControlSnapshot {
        if (!prefs.getBoolean(KEY_HAD_USAGE_PERMISSION, false)) {
            prefs.edit().putBoolean(KEY_HAD_USAGE_PERMISSION, true).apply()
        }
        return read()
    }

    override fun pauseCollection(pausedAtMs: Long): ControlSnapshot {
        prefs.edit()
            .putBoolean(KEY_PAUSED_BY_USER, true)
            .putLong(KEY_PAUSED_AT, pausedAtMs)
            .putBoolean(KEY_BACKFILL_PAUSED, false)
            .apply()
        return read()
    }

    override fun resumeCollection(resumedAtMs: Long, backfillPausedPeriod: Boolean): ControlSnapshot {
        prefs.edit()
            .putBoolean(KEY_PAUSED_BY_USER, false)
            .putLong(KEY_RESUMED_AT, resumedAtMs)
            .putBoolean(KEY_BACKFILL_PAUSED, backfillPausedPeriod)
            .apply()
        return read()
    }

    override fun setDarkThemeEnabled(enabled: Boolean): ControlSnapshot {
        prefs.edit().putBoolean(KEY_DARK_THEME, enabled).apply()
        return read()
    }

    override fun deleteAllLocalData(): ControlSnapshot {
        val notificationManager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancelAll()
        val keepDarkTheme = prefs.getBoolean(KEY_DARK_THEME, true)
        val nextParticipantId = UUID.randomUUID().toString()
        prefs.edit()
            .clear()
            .putString(KEY_PARTICIPANT_ID, nextParticipantId)
            .putBoolean(KEY_DARK_THEME, keepDarkTheme)
            .apply()
        return read()
    }

    private fun ensureParticipantId(): String {
        val existing = prefs.getString(KEY_PARTICIPANT_ID, null)
        if (existing != null) {
            return existing
        }
        val created = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_PARTICIPANT_ID, created).apply()
        return created
    }

    companion object {
        private const val PREFS_NAME = "wellbeing_control_store"
        private const val KEY_PARTICIPANT_ID = "participant_id"
        private const val KEY_CONSENT_VERSION = "consent_version"
        private const val KEY_CONSENT_ACCEPTED_AT = "consent_accepted_at_ms"
        private const val KEY_CONSENT_DECLINED = "consent_declined"
        private const val KEY_HAD_USAGE_PERMISSION = "had_usage_permission"
        private const val KEY_PAUSED_BY_USER = "paused_by_user"
        private const val KEY_PAUSED_AT = "paused_at_ms"
        private const val KEY_RESUMED_AT = "resumed_at_ms"
        private const val KEY_BACKFILL_PAUSED = "backfill_paused_period"
        private const val KEY_DARK_THEME = "dark_theme_enabled"
    }
}
