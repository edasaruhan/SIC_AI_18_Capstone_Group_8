package com.group8.wellbeing.usage

import java.security.MessageDigest

object UsageEventNormalizer {
    const val SOURCE_VERSION: String = "android-usage-v1"
    const val NON_APP_FILTER_VERSION: String = "non-app-filter-v1"

    const val EVENT_ACTIVITY_RESUMED: Int = 1
    const val EVENT_SCREEN_INTERACTIVE: Int = 15
    const val EVENT_SCREEN_NON_INTERACTIVE: Int = 16
    const val EVENT_KEYGUARD_SHOWN: Int = 17
    const val EVENT_KEYGUARD_HIDDEN: Int = 18

    private val exactSystemPackages = setOf(
        "android",
        "com.android.systemui",
        "com.android.launcher",
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher",
        "com.sec.android.app.launcher",
        "com.miui.home",
        "com.huawei.android.launcher",
        "com.oppo.launcher",
        "com.google.android.inputmethod.latin",
        "com.samsung.android.honeyboard",
        "com.touchtype.swiftkey"
    )

    private val nonAppSubstrings = listOf(
        "launcher",
        "inputmethod",
        "keyboard",
        "systemui"
    )

    fun mapEventType(androidEventType: Int): NormalizedEventType? {
        return when (androidEventType) {
            EVENT_SCREEN_INTERACTIVE -> NormalizedEventType.SCREEN_INTERACTIVE
            EVENT_KEYGUARD_HIDDEN -> NormalizedEventType.KEYGUARD_HIDDEN
            EVENT_KEYGUARD_SHOWN -> NormalizedEventType.KEYGUARD_SHOWN
            EVENT_SCREEN_NON_INTERACTIVE -> NormalizedEventType.SCREEN_NON_INTERACTIVE
            EVENT_ACTIVITY_RESUMED -> NormalizedEventType.APP_FOREGROUND
            else -> null
        }
    }

    fun isNonAppPackage(packageName: String?): Boolean {
        val normalized = packageName?.trim()?.lowercase() ?: return true
        if (normalized.isEmpty()) {
            return true
        }
        if (normalized in exactSystemPackages) {
            return true
        }
        return nonAppSubstrings.any { token -> normalized.contains(token) }
    }

    fun tokenizePackage(packageName: String?): String? {
        val cleaned = packageName?.trim()?.lowercase() ?: return null
        if (cleaned.isEmpty()) {
            return null
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(cleaned.toByteArray(Charsets.UTF_8))
        val hex = digest.take(6).joinToString("") { byte -> "%02x".format(byte) }
        return "pkg_$hex"
    }

    fun normalize(
        rawEvents: List<RawSystemEvent>,
        startMs: Long,
        endMs: Long
    ): List<NormalizedUsageEvent> {
        return rawEvents
            .asSequence()
            .filter { event -> event.timestampMs >= startMs && event.timestampMs < endMs }
            .mapNotNull { event ->
                val mappedType = mapEventType(event.androidEventType) ?: return@mapNotNull null
                val pkg = if (mappedType == NormalizedEventType.APP_FOREGROUND) {
                    event.packageName?.takeIf { it.isNotBlank() }
                } else {
                    null
                }
                NormalizedUsageEvent(
                    timestampMs = event.timestampMs,
                    eventType = mappedType,
                    packageName = pkg,
                    packageToken = tokenizePackage(pkg),
                    className = if (mappedType == NormalizedEventType.APP_FOREGROUND) {
                        event.className?.takeIf { it.isNotBlank() }
                    } else {
                        null
                    },
                    isNonAppPackage = if (mappedType == NormalizedEventType.APP_FOREGROUND) {
                        isNonAppPackage(pkg)
                    } else {
                        false
                    },
                    sourceEventCode = event.androidEventType,
                    sourceVersion = SOURCE_VERSION
                )
            }
            .sortedBy { it.timestampMs }
            .toList()
    }
}
