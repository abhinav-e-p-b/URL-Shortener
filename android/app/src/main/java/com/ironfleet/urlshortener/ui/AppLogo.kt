package com.ironfleet.urlshortener.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Builds the chain-link SVG icon used as the app logo mark.
 * Drawn as a 24×24 vector to match Material icon conventions.
 */
fun chainLinkIcon(tint: Color): ImageVector = ImageVector.Builder(
    name = "ChainLink",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 48f,
    viewportHeight = 48f
).apply {
    // Left ring arc
    path(
        stroke = SolidColor(tint),
        strokeLineWidth = 3.8f,
        strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round
    ) {
        moveTo(20f, 24f)
        arcTo(6f, 6f, 0f, false, true, 26f, 18f)
        lineTo(30f, 18f)
        arcTo(6f, 6f, 0f, false, true, 30f, 30f)
        lineTo(26f, 30f)
    }
    // Right ring arc (dimmed via alpha in usage)
    path(
        stroke = SolidColor(tint.copy(alpha = 0.5f)),
        strokeLineWidth = 3.8f,
        strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round
    ) {
        moveTo(28f, 24f)
        arcTo(6f, 6f, 0f, false, true, 22f, 30f)
        lineTo(18f, 30f)
        arcTo(6f, 6f, 0f, false, true, 18f, 18f)
        lineTo(22f, 18f)
    }
    // Horizontal connector bar
    path(
        stroke = SolidColor(tint),
        strokeLineWidth = 3.8f,
        strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round
    ) {
        moveTo(18f, 24f)
        lineTo(30f, 24f)
    }
}.build()

/**
 * Composable that renders the SnipURL logo: chain-link icon + "Snip**URL**" wordmark.
 *
 * @param iconSize  Size of the icon. Defaults to 28 dp (suitable for a TopAppBar).
 * @param fontSize  Font size of the wordmark in sp. Defaults to 20.
 * @param modifier  Applied to the outer [Row].
 */
@Composable
fun AppLogo(
    iconSize: Dp = 28.dp,
    fontSize: Int = 20,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Icon(
            imageVector = chainLinkIcon(tint = onSurface),   // charcoal — matches letterpress ink
            contentDescription = "SnipURL logo",
            tint = Color.Unspecified,   // tint is baked into the vector paths
            modifier = Modifier.size(iconSize)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = onSurface)) { append("Snip") }
                withStyle(SpanStyle(color = primary))   { append("URL") }
            },
            fontWeight = FontWeight.ExtraBold,
            fontSize = fontSize.sp,
            letterSpacing = (-0.5).sp
        )
    }
}
