package com.ironfleet.urlshortener.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Brand colors — a professional blue/teal palette
private val brandPrimary = Color(0xFF1565C0)      // Deep Blue
private val brandOnPrimary = Color(0xFFFFFFFF)
private val brandPrimaryContainer = Color(0xFFD6E4FF)
private val brandOnPrimaryContainer = Color(0xFF001A43)
private val brandSecondary = Color(0xFF00838F)    // Teal
private val brandSurface = Color(0xFFF8F9FF)
private val brandOnSurface = Color(0xFF1A1C23)
private val brandError = Color(0xFFBA1A1A)

private val LightColorScheme = lightColorScheme(
    primary = brandPrimary,
    onPrimary = brandOnPrimary,
    primaryContainer = brandPrimaryContainer,
    onPrimaryContainer = brandOnPrimaryContainer,
    secondary = brandSecondary,
    surface = brandSurface,
    onSurface = brandOnSurface,
    error = brandError
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9BBEFF),
    onPrimary = Color(0xFF002E6C),
    primaryContainer = Color(0xFF00429A),
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = Color(0xFF4FD8EB),
    surface = Color(0xFF12131A),
    onSurface = Color(0xFFE3E2EC),
    error = Color(0xFFFFB4AB)
)

@Composable
fun URLShortenerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
