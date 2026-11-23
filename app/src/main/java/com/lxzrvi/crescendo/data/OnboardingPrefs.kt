package com.lxzrvi.crescendo.data

import android.content.Context

/** Persists onboarding completion until DataStore is wired in. */
class OnboardingPrefs(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isOnboardingComplete(): Boolean = prefs.getBoolean(KEY_DONE, false)

    fun setOnboardingComplete(value: Boolean) {
        prefs.edit().putBoolean(KEY_DONE, value).apply()
    }

    companion object {
        private const val PREFS_NAME = "crescendo_prefs"
        private const val KEY_DONE = "onboarding_complete"
    }
}
