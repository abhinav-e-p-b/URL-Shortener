package com.ironfleet.urlshortener

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ironfleet.urlshortener.ui.UrlShortenerNavHost
import com.ironfleet.urlshortener.ui.theme.URLShortenerTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity entry point for the app.
 * Hilt injects dependencies via @AndroidEntryPoint.
 * The Compose NavHost handles all screen navigation.
 *
 * Also handles incoming share intents (ACTION_SEND / text/plain) so the app
 * appears in the Android share sheet — the shared URL is forwarded to the
 * NavHost via [sharedUrl], which HomeScreen observes to pre-fill and
 * auto-trigger shortening.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Extract the shared URL (if the activity was started from a share intent)
        val sharedUrl = extractSharedUrl(intent)

        setContent {
            URLShortenerTheme {
                UrlShortenerNavHost(sharedUrl = sharedUrl)
            }
        }
    }

    /**
     * Called when the activity is already running and a new share intent arrives
     * (e.g. the user shares a second URL without closing the app).
     * singleTop / singleTask launch modes route new intents here instead of
     * creating a fresh instance.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Update the stored intent so extractSharedUrl always sees the latest one
        setIntent(intent)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Returns the URL string carried by an ACTION_SEND / text/plain intent,
     * or null if this is a normal launcher start.
     */
    private fun extractSharedUrl(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND) return null
        if (intent.type != "text/plain") return null
        return intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()
            ?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
    }
}
