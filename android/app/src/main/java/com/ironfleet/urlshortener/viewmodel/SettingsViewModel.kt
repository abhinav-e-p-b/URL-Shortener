package com.ironfleet.urlshortener.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ironfleet.urlshortener.repository.UrlShortenerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Settings screen.
 * Manages the API base URL field and persists changes via DataStore.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: UrlShortenerRepository
) : ViewModel() {

    /** Currently saved API base URL observed from DataStore */
    val savedBaseUrl: StateFlow<String> = repository.baseUrlFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ""
    )

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    /** Persist a new base URL; emits [saveSuccess] = true so the UI shows feedback */
    fun saveBaseUrl(url: String) {
        viewModelScope.launch {
            repository.saveBaseUrl(url)
            _saveSuccess.value = true
        }
    }

    /** Reset the save-success flag after the snackbar has been shown */
    fun consumeSaveSuccess() {
        _saveSuccess.value = false
    }
}
