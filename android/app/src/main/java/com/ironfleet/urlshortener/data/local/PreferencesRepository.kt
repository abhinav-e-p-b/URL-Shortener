package com.ironfleet.urlshortener.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** DataStore instance scoped to the Application context */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Preferences repository backed by DataStore.
 * Replaces the web frontend's localStorage.getItem(BASE_KEY) pattern.
 */
@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        /** Default API base URL — points to the Render-deployed Flask backend */
        const val DEFAULT_BASE_URL = "https://url-shortener-1-m70j.onrender.com/"

        private val KEY_BASE_URL = stringPreferencesKey("api_base_url")
    }

    /** Observe the API base URL; emits the default when no value is saved */
    val baseUrlFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_BASE_URL] ?: DEFAULT_BASE_URL
    }

    /** Persist a new API base URL chosen by the user in Settings */
    suspend fun saveBaseUrl(url: String) {
        context.dataStore.edit { prefs ->
            // Ensure the URL always ends with a trailing slash for Retrofit
            prefs[KEY_BASE_URL] = if (url.endsWith("/")) url else "$url/"
        }
    }
}
