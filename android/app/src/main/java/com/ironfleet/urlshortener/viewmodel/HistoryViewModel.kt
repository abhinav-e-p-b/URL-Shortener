package com.ironfleet.urlshortener.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ironfleet.urlshortener.repository.UrlShortenerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the History screen.
 * Converts the Room Flow into a StateFlow for the Composable to collect,
 * and coordinates background refresh of click counts from the server.
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

    /**
     * True while a click-count network refresh is in progress.
     * The UI shows a subtle loading indicator while this is active.
     */
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        // Wait for Room's first real emission before refreshing click counts.
        // history starts with initialValue = emptyList(), so calling
        // refreshClickCounts() directly in init{} would always be a no-op.
        viewModelScope.launch {
            history.first { it.isNotEmpty() } // suspends until DB data arrives
            refreshClickCounts()
        }
    }

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

    /**
     * Fetch the latest click counts from the server for all current history
     * entries and update the local Room cache. Safe to call repeatedly —
     * silently no-ops if the network is unavailable.
     */
    fun refreshClickCounts() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val codes = history.value.map { it.shortCode }
            if (codes.isNotEmpty()) {
                repository.refreshClickCounts(codes)
            }
            _isRefreshing.value = false
        }
    }
}
