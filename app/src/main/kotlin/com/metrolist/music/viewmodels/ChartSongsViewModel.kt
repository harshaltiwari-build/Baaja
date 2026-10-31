package com.metrolist.music.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/** One of YouTube's charts as audio songs only (see [audioChart]), kept for half an hour. */
@HiltViewModel
class ChartSongsViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val playlistId: String = checkNotNull(savedStateHandle["playlistId"])

        private val _songs = MutableStateFlow<List<SongItem>?>(null)
        val songs = _songs.asStateFlow()
        private val _hasError = MutableStateFlow(false)
        val hasError = _hasError.asStateFlow()
        private var job: Job? = null

        init {
            load()
        }

        fun load() {
            if (job?.isActive == true) return
            _hasError.value = false
            cache[playlistId]?.takeIf { System.currentTimeMillis() - it.first < FRESH_MS }?.let {
                _songs.value = it.second
                return
            }
            _songs.value = null
            job =
                viewModelScope.launch {
                    val entries = YouTube.playlist(playlistId).onFailure(::reportException).getOrNull()?.songs.orEmpty().take(MAX_ENTRIES)
                    val songs =
                        audioChart(entries) { query ->
                            YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items?.filterIsInstance<SongItem>().orEmpty().take(5)
                        }
                    if (songs.isEmpty()) {
                        _hasError.value = true
                    } else {
                        cache[playlistId] = System.currentTimeMillis() to songs
                    }
                    _songs.value = songs
                }
        }

        private companion object {
            const val FRESH_MS = 30 * 60 * 1000L
            const val MAX_ENTRIES = 100
            val cache = ConcurrentHashMap<String, Pair<Long, List<SongItem>>>()
        }
    }
