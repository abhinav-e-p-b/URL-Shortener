package com.ironfleet.urlshortener

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
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            URLShortenerTheme {
                UrlShortenerNavHost()
            }
        }
    }
}
