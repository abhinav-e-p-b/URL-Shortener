package com.ironfleet.urlshortener.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.ironfleet.urlshortener.R
import com.ironfleet.urlshortener.ui.theme.KraftSurface
import com.ironfleet.urlshortener.ui.theme.debossedContainer
import com.ironfleet.urlshortener.ui.theme.embossedCard
import com.ironfleet.urlshortener.viewmodel.HomeViewModel

/**
 * Home screen — the main URL shortening interface.
 * Mirrors the functionality of the web frontend's index.html.
 *
 * NOTE: This composable does NOT create its own Scaffold/TopAppBar.
 * The outer Scaffold in UrlShortenerNavHost owns the app chrome.
 * We only consume innerPadding to avoid the content being hidden
 * behind the bottom navigation bar.
 */
@Composable
fun HomeScreen(
    innerPadding: PaddingValues,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Map error codes to user-friendly strings
    val errorMessage = when (uiState.errorMessage) {
        "EMPTY_URL"   -> stringResource(R.string.error_empty_url)
        "INVALID_URL" -> stringResource(R.string.error_invalid_url)
        "NO_NETWORK"  -> stringResource(R.string.error_network)
        null          -> null
        else          -> uiState.errorMessage
    }
    val rateLimitMessage = if (uiState.isRateLimited) stringResource(R.string.error_rate_limit) else null

    // Show snackbar for errors and rate-limit messages
    LaunchedEffect(errorMessage, rateLimitMessage) {
        val msg = rateLimitMessage ?: errorMessage
        if (msg != null) snackbarHostState.showSnackbar(msg)
    }

    val cardShape = RoundedCornerShape(8.dp)
    val outlineColor = MaterialTheme.colorScheme.outline

    // Box lets the SnackbarHost float at the bottom over the scrollable column
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Uppercase hero heading — letterpress stamped ──────────────────
            Text(
                text = "PASTE A LONG LINK.\nGET A SHORT ONE.",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            // Muted subtitle — greyboard tone
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )

            // ── URL input field — debossed well ───────────────────────────────
            OutlinedTextField(
                value = uiState.urlInput,
                onValueChange = viewModel::onUrlChanged,
                label = { Text(stringResource(R.string.paste_url_hint)) },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = false,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        viewModel.shortenUrl()
                    }
                ),
                shape = cardShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor    = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor  = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContainerColor   = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor       = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    unfocusedBorderColor     = MaterialTheme.colorScheme.outline,
                    disabledBorderColor      = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedLabelColor        = MaterialTheme.colorScheme.onSurface,
                    unfocusedLabelColor      = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedLeadingIconColor  = MaterialTheme.colorScheme.onSurface,
                    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedTextColor         = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor       = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .debossedContainer(cornerRadius = 8.dp, backgroundColor = MaterialTheme.colorScheme.surfaceVariant),
                enabled = !uiState.isLoading
            )

            Spacer(Modifier.height(16.dp))

            // ── Shorten button — embossed, full-width, tactile press ──────────
            val buttonInteraction = remember { MutableInteractionSource() }
            val isPressed by buttonInteraction.collectIsPressedAsState()

            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.shortenUrl()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .embossedCard(
                        cornerRadius = 8.dp,
                        backgroundColor = if (isPressed)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                        else
                            MaterialTheme.colorScheme.primary
                    ),
                shape = cardShape,
                interactionSource = buttonInteraction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor   = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    disabledContentColor   = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                ),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.size(8.dp))
                    if (uiState.isWakingUp) {
                        Text(
                            "Waking up server…",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            "Shortening…",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                } else {
                    Text(
                        stringResource(R.string.shorten_button),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Dashed crease divider — tactile score/fold line ───────────────
            DashedHorizontalDivider(color = outlineColor)

            Spacer(Modifier.height(24.dp))

            // ── Result card — shown with animation when a short URL is ready ──
            AnimatedVisibility(
                visible = uiState.resultShortUrl != null,
                enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                    animationSpec = tween(300),
                    initialOffsetY = { it / 3 }
                ),
                exit = fadeOut(animationSpec = tween(200))
            ) {
                uiState.resultShortUrl?.let { shortUrl ->
                    ResultCard(
                        shortUrl = shortUrl,
                        onCopy = {
                            copyToClipboard(context, shortUrl)
                            Toast.makeText(context, context.getString(R.string.copied_toast), Toast.LENGTH_SHORT).show()
                        },
                        onOpen = {
                            openInCustomTab(context, shortUrl)
                        }
                    )
                }
            }
        } // end Column

        // Snackbar anchored to the bottom of the Box
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    } // end Box
}

/**
 * Draws a dashed horizontal crease using [Canvas] + [PathEffect.dashPathEffect].
 * Mimics the scored/perforated lines found on kraft cardboard packaging.
 */
@Composable
private fun DashedHorizontalDivider(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
    ) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = 1.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
        )
    }
}

@Composable
private fun ResultCard(
    shortUrl: String,
    onCopy: () -> Unit,
    onOpen: () -> Unit
) {
    val cardShape = RoundedCornerShape(8.dp)

    // Embossed result card — raised from the paper surface
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .embossedCard(cornerRadius = 8.dp, backgroundColor = KraftSurface)
            .padding(20.dp)
    ) {
        // Stamped label — uppercase, wide tracking
        Text(
            text = "YOUR SHORT URL",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = shortUrl,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(16.dp))

        // Dashed inner crease before actions
        DashedHorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(14.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Copy — secondary ghost button (debossed feel, lighter background)
            val copyInteraction = remember { MutableInteractionSource() }
            OutlinedButton(
                onClick = onCopy,
                shape = cardShape,
                interactionSource = copyInteraction,
                modifier = Modifier
                    .weight(1f)
                    .embossedCard(
                        cornerRadius = 6.dp,
                        backgroundColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor   = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                border = null,
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Icon(
                    Icons.Filled.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    stringResource(R.string.copy_button),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            // Open — primary embossed button
            val openInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = onOpen,
                shape = cardShape,
                interactionSource = openInteraction,
                modifier = Modifier
                    .weight(1f)
                    .embossedCard(
                        cornerRadius = 6.dp,
                        backgroundColor = MaterialTheme.colorScheme.primary
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor   = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Icon(
                    Icons.Filled.OpenInBrowser,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    stringResource(R.string.open_button),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

/** Copy text to the system clipboard */
private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("short_url", text))
}

/** Open a URL in a Chrome Custom Tab */
private fun openInCustomTab(context: Context, url: String) {
    CustomTabsIntent.Builder()
        .setShowTitle(true)
        .build()
        .launchUrl(context, url.toUri())
}
