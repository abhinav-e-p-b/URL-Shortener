package com.ironfleet.urlshortener.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ironfleet.urlshortener.repository.ShortenResult
import com.ironfleet.urlshortener.repository.UrlShortenerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI state for the Home screen */
data class HomeUiState(
    val urlInput: String = "",
    val isLoading: Boolean = false,
    val resultShortUrl: String? = null,
    val resultShortCode: String? = null,
    val errorMessage: String? = null,
    val isRateLimited: Boolean = false
)

/**
 * ViewModel for the Home screen.
 * Exposes a [StateFlow<HomeUiState>] that the Composable collects.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: UrlShortenerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Called on every keystroke in the URL input field */
    fun onUrlChanged(newUrl: String) {
        _uiState.value = _uiState.value.copy(
            urlInput = newUrl,
            // Clear previous result/error when the user starts typing again
            resultShortUrl = null,
            resultShortCode = null,
            errorMessage = null,
            isRateLimited = false
        )
    }

    /** Called when the user taps "Shorten" */
    fun shortenUrl() {
        val url = _uiState.value.urlInput.trim()

        // Basic client-side validation
        if (url.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "EMPTY_URL")
            return
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            _uiState.value = _uiState.value.copy(errorMessage = "INVALID_URL")
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            isRateLimited = false,
            resultShortUrl = null,
            resultShortCode = null
        )

        viewModelScope.launch {
            val result = repository.shorten(url)
            _uiState.value = when (result) {
                is ShortenResult.Success -> _uiState.value.copy(
                    isLoading = false,
                    resultShortUrl = result.shortUrl,
                    resultShortCode = result.shortCode
                )
                is ShortenResult.RateLimited -> _uiState.value.copy(
                    isLoading = false,
                    isRateLimited = true
                )
                is ShortenResult.Error -> _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.message
                )
            }
        }
    }

    /** Called after the user dismisses an error or copies the result */
    fun clearResult() {
        _uiState.value = HomeUiState(urlInput = _uiState.value.urlInput)
    }
}
