/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.pages.ChartsPage
import com.metrolist.innertube.pages.HomePage
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

private const val CHARTS_TIMEOUT_MS = 20_000L

/**
 * Builds a charts-shaped page from the YouTube Music home feed, keeping only sections that
 * contain songs. Used when the real charts endpoint is empty (e.g. region without charts).
 */
internal fun HomePage.toTrendingChartsPage(): ChartsPage? {
    val sections =
        sections
            .mapNotNull { section ->
                val songs = section.items.filterIsInstance<SongItem>().distinctBy { it.id }
                if (songs.size < 2) {
                    null
                } else {
                    ChartsPage.ChartSection(
                        title = section.title,
                        items = songs,
                        chartType = ChartsPage.ChartType.TRENDING,
                    )
                }
            }.distinctBy { it.title }
            .take(4)
    return if (sections.isEmpty()) null else ChartsPage(sections = sections, continuation = null)
}

@HiltViewModel
class ChartsViewModel @Inject constructor() : ViewModel() {
    private val _chartsPage = MutableStateFlow<ChartsPage?>(null)
    val chartsPage = _chartsPage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    /** True when [chartsPage] holds the home-feed fallback instead of real charts. */
    private val _isFallback = MutableStateFlow(false)
    val isFallback = _isFallback.asStateFlow()

    private var loadJob: Job? = null

    /** Loads charts; if YouTube returns none (or fails) falls back to the trending home feed. */
    fun loadCharts() {
        if (loadJob?.isActive == true) return
        loadJob =
            viewModelScope.launch {
                _isLoading.value = true
                _error.value = null
                _isFallback.value = false

                var failure: Throwable? = null
                var page: ChartsPage? = null
                try {
                    withTimeout(CHARTS_TIMEOUT_MS) {
                        YouTube
                            .getChartsPage()
                            .onSuccess { page = it }
                            .onFailure { failure = it }
                        if (page?.sections.isNullOrEmpty()) {
                            YouTube
                                .home()
                                .onSuccess { home ->
                                    home.toTrendingChartsPage()?.let {
                                        page = it
                                        _isFallback.value = true
                                    }
                                }.onFailure { if (failure == null) failure = it }
                        }
                    }
                } catch (e: TimeoutCancellationException) {
                    failure = e
                }

                failure?.let { reportException(it) }
                if (page?.sections.isNullOrEmpty()) {
                    _chartsPage.value = null
                    _error.value = failure?.message ?: "empty"
                } else {
                    _chartsPage.value = page
                }
                _isLoading.value = false
            }
    }

    fun loadMore() {
        if (_isFallback.value || _isLoading.value) return
        viewModelScope.launch {
            _chartsPage.value?.continuation?.let { continuation ->
                _isLoading.value = true
                YouTube
                    .getChartsPage(continuation)
                    .onSuccess { newPage ->
                        _chartsPage.value =
                            _chartsPage.value?.copy(
                                sections = _chartsPage.value?.sections.orEmpty() + newPage.sections,
                                continuation = newPage.continuation,
                            )
                    }.onFailure { reportException(it) }
                _isLoading.value = false
            }
        }
    }
}
