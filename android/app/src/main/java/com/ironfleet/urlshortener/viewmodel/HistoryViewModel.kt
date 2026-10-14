package com.ironfleet.urlshortener.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ironfleet.urlshortener.data.local.UrlHistoryEntity
import com.ironfleet.urlshortener.repository.UrlShortenerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the History screen.
 * Converts the Room Flow into a StateFlow for the Composable to collect.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: UrlShortenerRepository
) : ViewModel() {

    /** Reactive list of all saved URLs, newest-first */
    val history: StateFlow<List<UrlHistoryEntity>> = repository
        .getHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /** Delete one item by its local Room ID */
    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            repository.deleteHistoryEntry(id)
        }
    }

    /** Clear all history */
    fun clearAll() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }
}
