/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.features

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.constants.LocalMusicSortDescendingKey
import com.metrolist.music.constants.LocalMusicSortKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.local.LocalAlbum
import com.metrolist.music.local.LocalArtist
import com.metrolist.music.local.LocalAudio
import com.metrolist.music.local.LocalLibrary
import com.metrolist.music.local.LocalMusicPlayback
import com.metrolist.music.local.LocalMusicScanner
import com.metrolist.music.local.LocalSort
import com.metrolist.music.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LocalMusicTab { SONGS, ALBUMS, ARTISTS }

@HiltViewModel
class LocalMusicViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) : ViewModel() {
    private val _hasPermission = MutableStateFlow(LocalMusicScanner.hasPermission(context))
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val allSongs = MutableStateFlow<List<LocalAudio>>(emptyList())
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    private val _hasScanned = MutableStateFlow(false)
    val hasScanned: StateFlow<Boolean> = _hasScanned.asStateFlow()

    val query = MutableStateFlow("")
    val tab = MutableStateFlow(LocalMusicTab.SONGS)

    /** Key of the album / artist currently opened, or null for the overview. */
    val openGroupKey = MutableStateFlow<String?>(null)

    private val _sort = MutableStateFlow(LocalSort.TITLE)
    val sort: StateFlow<LocalSort> = _sort.asStateFlow()
    private val _descending = MutableStateFlow(false)
    val descending: StateFlow<Boolean> = _descending.asStateFlow()

    val totalCount: StateFlow<Int> =
        allSongs.map { it.size }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val filtered =
        combine(allSongs, query) { songs, q -> LocalLibrary.filter(songs, q) }
            .flowOn(Dispatchers.Default)

    val songs: StateFlow<List<LocalAudio>> =
        combine(filtered, _sort, _descending) { songs, sort, desc -> LocalLibrary.sort(songs, sort, desc) }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val albums: StateFlow<List<LocalAlbum>> =
        filtered.map { LocalLibrary.albums(it) }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val artists: StateFlow<List<LocalArtist>> =
        filtered.map { LocalLibrary.artists(it) }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The opened album or artist, looked up in the unfiltered library so searching doesn't empty it. */
    val openGroup: StateFlow<OpenGroup?> =
        combine(allSongs, openGroupKey) { songs, key ->
            when {
                key == null -> null
                key.startsWith("album:") ->
                    LocalLibrary.albums(songs).firstOrNull { it.key == key }?.let {
                        OpenGroup(key, it.title, it.artist, it.artworkUri, it.songs, isArtist = false)
                    }
                else ->
                    LocalLibrary.artists(songs).firstOrNull { it.key == key }?.let {
                        OpenGroup(key, it.name, null, it.artworkUri, it.songs, isArtist = true, albumCount = it.albumCount)
                    }
            }
        }.flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    data class OpenGroup(
        val key: String,
        val title: String,
        val subtitle: String?,
        val artworkUri: String?,
        val songs: List<LocalAudio>,
        val isArtist: Boolean,
        val albumCount: Int = 0,
    )

    private var scanJob: Job? = null
    private val observer =
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                scheduleRescan(debounceMs = 1_500)
            }
        }
    private var observerRegistered = false

    init {
        viewModelScope.launch {
            val prefs = context.dataStore.data.first()
            prefs[LocalMusicSortKey]?.let { saved ->
                LocalSort.entries.firstOrNull { it.name == saved }?.let { _sort.value = it }
            }
            _descending.value = prefs[LocalMusicSortDescendingKey] ?: false
        }
        refreshPermission()
    }

    /** Call when the screen resumes or the permission dialog returns. */
    fun refreshPermission() {
        val granted = LocalMusicScanner.hasPermission(context)
        val changed = granted != _hasPermission.value
        _hasPermission.value = granted
        if (granted) {
            registerObserver()
            if (changed || !_hasScanned.value) scheduleRescan(debounceMs = 0)
        } else {
            allSongs.value = emptyList()
        }
    }

    fun rescan() = scheduleRescan(debounceMs = 0)

    private fun scheduleRescan(debounceMs: Long) {
        if (!_hasPermission.value) return
        scanJob?.cancel()
        scanJob =
            viewModelScope.launch {
                if (debounceMs > 0) delay(debounceMs)
                _isScanning.value = true
                try {
                    allSongs.value = LocalMusicScanner.scan(context)
                    _hasScanned.value = true
                } finally {
                    _isScanning.value = false
                }
            }
    }

    private fun registerObserver() {
        if (observerRegistered) return
        runCatching {
            context.contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                observer,
            )
            observerRegistered = true
        }
    }

    fun setSort(sort: LocalSort) {
        if (_sort.value == sort) return
        _sort.value = sort
        viewModelScope.launch { context.dataStore.edit { it[LocalMusicSortKey] = sort.name } }
    }

    fun toggleDescending() {
        val value = !_descending.value
        _descending.value = value
        viewModelScope.launch { context.dataStore.edit { it[LocalMusicSortDescendingKey] = value } }
    }

    /** Adds the songs to the database (for history and stats) without delaying playback. */
    fun register(songs: List<LocalAudio>, unknownArtist: String) {
        LocalMusicPlayback.registerInBackground(database, songs, unknownArtist)
    }

    override fun onCleared() {
        if (observerRegistered) runCatching { context.contentResolver.unregisterContentObserver(observer) }
        super.onCleared()
    }
}
