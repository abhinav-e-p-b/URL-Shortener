package com.ironfleet.urlshortener.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─── Relevo & Vinco — Raw Material / Kraft Cardboard Palette ─────────────────
// The "zero-ink" tactile theme: off-white paper, kraft brown, charcoal stamp.

private val paperWhite  = Color(0xFFF5F2EB)   // base background — off-white paper
private val kraftBrown  = Color(0xFFD7C9BA)   // raised surface — kraft cardboard
private val kraftDark   = Color(0xFFC4B5A5)   // inset surface — debossed well
private val charcoal    = Color(0xFF2C2B29)   // primary text — letterpress ink
private val greyboard   = Color(0xFFA29E99)   // muted text — neutral greyboard
private val inkStamp    = Color(0xFF3D3A35)   // primary accent — dark stamp ink
private val outlineColor = Color(0xFFBEB5AA)  // dividers / borders
private val errorRed    = Color(0xFF8B4040)   // warm muted error red

// ─── Single light colour scheme — always light, mimics physical paper ────────
private val KraftLightColorScheme = lightColorScheme(
    primary              = inkStamp,
    onPrimary            = paperWhite,
    primaryContainer     = kraftBrown,
    onPrimaryContainer   = charcoal,
    secondary            = greyboard,
    onSecondary          = paperWhite,
    secondaryContainer   = kraftDark,
    onSecondaryContainer = charcoal,
    background           = paperWhite,
    onBackground         = charcoal,
    surface              = kraftBrown,
    onSurface            = charcoal,
    surfaceVariant       = kraftDark,
    onSurfaceVariant     = greyboard,
    outline              = outlineColor,
    error                = errorRed,
    onError              = paperWhite
)

@Composable
fun URLShortenerTheme(
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = KraftLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Status bar matches the paper background — light icons mode
            window.statusBarColor = paperWhite.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
