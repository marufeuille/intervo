package dev.marufeuille.intervo.companion.sync

import android.content.Context
import dev.marufeuille.intervo.companion.BuildConfig

class CompanionSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var ingestEndpoint: String
        get() = prefs.getString(KEY_INGEST_ENDPOINT, BuildConfig.DEFAULT_INGEST_ENDPOINT)
            ?: BuildConfig.DEFAULT_INGEST_ENDPOINT
        set(value) {
            prefs.edit().putString(KEY_INGEST_ENDPOINT, value.trim()).apply()
        }

    companion object {
        private const val PREFS_NAME = "intervo_companion_settings"
        private const val KEY_INGEST_ENDPOINT = "ingest_endpoint"
    }
}
