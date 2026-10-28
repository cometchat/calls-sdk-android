package com.cometchat.samplecallsringing.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/** The System / Light / Dark choice on the profile screen, kept across launches. */
enum class Appearance(val nightMode: Int) {
    SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    LIGHT(AppCompatDelegate.MODE_NIGHT_NO),
    DARK(AppCompatDelegate.MODE_NIGHT_YES);

    companion object {
        private const val PREFS_NAME = "sample_calls_ringing_prefs"
        private const val KEY_APPEARANCE = "appearance"

        fun current(context: Context): Appearance {
            val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_APPEARANCE, null)
            return entries.firstOrNull { it.name == stored } ?: SYSTEM
        }

        /** Saves [appearance] and applies it; open screens recreate in the new theme. */
        fun set(context: Context, appearance: Appearance) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_APPEARANCE, appearance.name)
                .apply()
            AppCompatDelegate.setDefaultNightMode(appearance.nightMode)
        }

        /** Runs at process start, before any screen is drawn. */
        fun apply(context: Context) {
            AppCompatDelegate.setDefaultNightMode(current(context).nightMode)
        }
    }
}
