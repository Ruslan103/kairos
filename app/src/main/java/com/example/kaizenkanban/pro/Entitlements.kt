package com.example.kaizenkanban.pro

import com.example.kaizenkanban.data.local.KairosPreferences

enum class ProFeature {
    VoiceAssistant,
    TaskPhotos
}

/**
 * PRO access gate. Stub for now: [KairosPreferences.proUnlocked] defaults to true.
 * Later swap the body for Play Billing / license-key verification.
 */
object Entitlements {
    fun has(prefs: KairosPreferences, feature: ProFeature): Boolean {
        if (!prefs.proUnlocked) return false
        return when (feature) {
            ProFeature.VoiceAssistant,
            ProFeature.TaskPhotos -> true
        }
    }
}
