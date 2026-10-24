package com.ironfleet.urlshortener.ui.theme

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─── Kraft Cardboard Color Constants ─────────────────────────────────────────
// Mirrored from Theme.kt for use in draw operations (no MaterialTheme access in drawBehind)
internal val KraftHighlight = Color(0xFFEDE7DE)   // top-left light highlight
internal val KraftShadowDeep = Color(0xFF2C2B29)   // bottom-right charcoal shadow
internal val KraftShadowMid  = Color(0xFF9E9388)   // mid-tone shadow
internal val KraftSurface    = Color(0xFFD7C9BA)   // kraft brown surface

// ─── Extension: coloredShadow ─────────────────────────────────────────────────
/**
 * Draws a colored blur shadow behind the composable.
 * Used internally by the tactile modifier stack.
 */
fun Modifier.coloredShadow(
    color: Color,
    borderRadius: Dp = 6.dp,
    blurRadius: Dp = 8.dp,
    offsetY: Dp = 4.dp,
    offsetX: Dp = 4.dp,
    spread: Float = 0f,
) = this.drawBehind {
    this.drawIntoCanvas {
        val paint = Paint().apply {
            asFrameworkPaint().apply {
                isAntiAlias = true
                this.color = android.graphics.Color.TRANSPARENT
                setShadowLayer(
                    blurRadius.toPx(),
                    offsetX.toPx(),
                    offsetY.toPx(),
                    color.copy(alpha = color.alpha).toArgb()
                )
            }
        }
        it.drawRoundRect(
            left        = spread,
            top         = spread,
            right       = this.size.width - spread,
            bottom      = this.size.height - spread,
            radiusX     = borderRadius.toPx(),
            radiusY     = borderRadius.toPx(),
            paint       = paint
        )
    }
}

// ─── Embossed Card Modifier ───────────────────────────────────────────────────
/**
 * Makes a composable appear physically raised OUT of the cardboard surface.
 * Applies multi-layer neumorphic shadows:
 *   • Top-left: bright highlight (light source from top-left)
 *   • Bottom-right: deep charcoal shadow
 *   • Clip to rounded corners
 *
 * Usage: Modifier.embossedCard()
 */
fun Modifier.embossedCard(
    cornerRadius: Dp = 8.dp,
    backgroundColor: Color = KraftSurface
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    // Deep bottom-right shadow
    .coloredShadow(
        color        = KraftShadowDeep.copy(alpha = 0.35f),
        borderRadius = cornerRadius,
        blurRadius   = 10.dp,
        offsetX      = 4.dp,
        offsetY      = 6.dp
    )
    // Mid shadow for depth
    .coloredShadow(
        color        = KraftShadowMid.copy(alpha = 0.20f),
        borderRadius = cornerRadius,
        blurRadius   = 4.dp,
        offsetX      = 2.dp,
        offsetY      = 3.dp
    )
    // Top-left highlight (negative offset = top-left)
    .coloredShadow(
        color        = KraftHighlight.copy(alpha = 0.90f),
        borderRadius = cornerRadius,
        blurRadius   = 4.dp,
        offsetX      = (-2).dp,
        offsetY      = (-2).dp
    )
    .background(backgroundColor, RoundedCornerShape(cornerRadius))

// ─── Debossed Container Modifier ─────────────────────────────────────────────
/**
 * Makes a composable appear physically pressed INTO the cardboard surface.
 * Inverts the lighting: shadow top-left, highlight bottom-right.
 * Use for: text fields, wells, inset containers.
 *
 * Usage: Modifier.debossedContainer()
 */
fun Modifier.debossedContainer(
    cornerRadius: Dp = 6.dp,
    backgroundColor: Color = Color(0xFFC4B5A5)
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(backgroundColor, RoundedCornerShape(cornerRadius))
    .drawBehind {
        // Inset shadow — top-left deep shadow (light source blocked)
        drawIntoCanvas {
            val shadowPaint = Paint().apply {
                asFrameworkPaint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.TRANSPARENT
                    setShadowLayer(
                        8.dp.toPx(),
                        3.dp.toPx(),
                        4.dp.toPx(),
                        KraftShadowDeep.copy(alpha = 0.45f).toArgb()
                    )
                }
            }
            // We can't do true CSS inset shadows in Compose drawBehind easily,
            // so we draw a subtle top-left inner border stripe for the deboss feel
            drawLine(
                color       = KraftShadowMid.copy(alpha = 0.5f),
                start       = Offset(cornerRadius.toPx(), 0f),
                end         = Offset(size.width - cornerRadius.toPx(), 0f),
                strokeWidth = 1.5f
            )
            drawLine(
                color       = KraftShadowMid.copy(alpha = 0.4f),
                start       = Offset(0f, cornerRadius.toPx()),
                end         = Offset(0f, size.height - cornerRadius.toPx()),
                strokeWidth = 1.5f
            )
            // Bottom-right highlight stripe (light reflecting from below)
            drawLine(
                color       = KraftHighlight.copy(alpha = 0.7f),
                start       = Offset(cornerRadius.toPx(), size.height),
                end         = Offset(size.width - cornerRadius.toPx(), size.height),
                strokeWidth = 1.5f
            )
            drawLine(
                color       = KraftHighlight.copy(alpha = 0.6f),
                start       = Offset(size.width, cornerRadius.toPx()),
                end         = Offset(size.width, size.height - cornerRadius.toPx()),
                strokeWidth = 1.5f
            )
        }
    }

// ─── Pressable / Tactile Button Modifier ─────────────────────────────────────
/**
 * Wraps a composable to behave as a physically pressing button.
 * On press: shifts down-right (deboss offset) + inner shadow.
 * On release: springs back to raised position.
 *
 * Usage:
 * ```
 * val interactionSource = remember { MutableInteractionSource() }
 * Box(
 *     modifier = Modifier
 *         .tactileButton(interactionSource = interactionSource)
 *         .clickable(interactionSource = interactionSource, indication = null) { ... }
 * )
 * ```
 */
fun Modifier.tactileButton(
    interactionSource: InteractionSource,
    cornerRadius: Dp = 6.dp,
    backgroundColor: Color = Color(0xFF3D3A35)
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animate the press offset — simulates physical depression
    val pressOffset by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 0.dp,
        animationSpec = tween(durationMillis = if (isPressed) 80 else 200),
        label = "pressOffset"
    )

    this
        .offset(x = pressOffset, y = pressOffset)
        .clip(RoundedCornerShape(cornerRadius))
        // When NOT pressed: raised shadows (embossed state)
        .then(
            if (!isPressed) {
                Modifier
                    .coloredShadow(
                        color        = KraftShadowDeep.copy(alpha = 0.45f),
                        borderRadius = cornerRadius,
                        blurRadius   = 8.dp,
                        offsetX      = 3.dp,
                        offsetY      = 4.dp
                    )
                    .coloredShadow(
                        color        = KraftHighlight.copy(alpha = 0.80f),
                        borderRadius = cornerRadius,
                        blurRadius   = 3.dp,
                        offsetX      = (-1).dp,
                        offsetY      = (-1).dp
                    )
            } else {
                // When pressed: flat / debossed shadow
                Modifier.coloredShadow(
                    color        = KraftShadowDeep.copy(alpha = 0.20f),
                    borderRadius = cornerRadius,
                    blurRadius   = 2.dp,
                    offsetX      = 1.dp,
                    offsetY      = 1.dp
                )
            }
        )
        .background(backgroundColor, RoundedCornerShape(cornerRadius))
}
