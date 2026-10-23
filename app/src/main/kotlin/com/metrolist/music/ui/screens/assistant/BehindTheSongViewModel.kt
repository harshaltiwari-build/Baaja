/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.assistant

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.FormatEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject

/** What the header and facts need, from the local database or, failing that, YouTube Music. */
data class BehindTheSongInfo(
    val id: String,
    val title: String,
    val artists: List<Pair<String?, String>>,
    val thumbnailUrl: String?,
    val albumName: String?,
    val year: Int?,
    val durationSeconds: Int,
    val totalPlayTimeMs: Long,
) {
    val artistNames: String get() = artists.joinToString(", ") { it.second }
}

data class ListeningStats(
    val firstPlayed: LocalDateTime?,
    val lastPlayed: LocalDateTime?,
    val sessions: Int,
)

@HiltViewModel
class BehindTheSongViewModel
@Inject
constructor(
    private val database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val videoId: String = savedStateHandle.get<String>("videoId").orEmpty()

    private val _info = MutableStateFlow<BehindTheSongInfo?>(null)
    val info: StateFlow<BehindTheSongInfo?> = _info.asStateFlow()

    private val _notFound = MutableStateFlow(false)
    val notFound: StateFlow<Boolean> = _notFound.asStateFlow()

    val playCount: StateFlow<Int> =
        database
            .getLifetimePlayCount(videoId)
            .catch { emit(0) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val format: StateFlow<FormatEntity?> =
        database
            .format(videoId)
            .catch { emit(null) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _stats = MutableStateFlow<ListeningStats?>(null)
    val stats: StateFlow<ListeningStats?> = _stats.asStateFlow()

    init {
        viewModelScope.launch { loadInfo() }
        viewModelScope.launch(Dispatchers.IO) { _stats.value = queryStats() }
    }

    private suspend fun loadInfo() {
        if (videoId.isBlank()) {
            _notFound.value = true
            return
        }
        val local = database.song(videoId).first()
        if (local != null) {
            _info.value =
                BehindTheSongInfo(
                    id = local.id,
                    title = local.song.title,
                    artists = local.orderedArtists.map { (if (it.id.startsWith("UC")) it.id else null) to it.name },
                    thumbnailUrl = local.song.thumbnailUrl,
                    albumName = local.album?.title ?: local.song.albumName,
                    year = local.song.year ?: local.album?.year,
                    durationSeconds = local.song.duration,
                    totalPlayTimeMs = local.song.totalPlayTime,
                )
            return
        }
        val remote =
            withContext(Dispatchers.IO) {
                YouTube.queue(listOf(videoId)).getOrNull()?.firstOrNull()
            }
        if (remote == null) {
            _notFound.value = true
            return
        }
        _info.value =
            BehindTheSongInfo(
                id = remote.id,
                title = remote.title,
                artists = remote.artists.map { it.id to it.name },
                thumbnailUrl = remote.thumbnail,
                albumName = remote.album?.name,
                year = null,
                durationSeconds = remote.duration ?: -1,
                totalPlayTimeMs = 0,
            )
    }

    private fun queryStats(): ListeningStats? =
        runCatching {
            database.openHelper.readableDatabase
                .query("SELECT MIN(timestamp), MAX(timestamp), COUNT(*) FROM event WHERE songId = ?", arrayOf<Any?>(videoId))
                .use { cursor ->
                    if (!cursor.moveToFirst()) return@use null
                    fun at(index: Int) =
                        if (cursor.isNull(index)) null else LocalDateTime.ofInstant(Instant.ofEpochMilli(cursor.getLong(index)), ZoneOffset.UTC)
                    ListeningStats(at(0), at(1), cursor.getInt(2))
                }
        }.onFailure { Timber.w(it, "Listening stats query failed") }
            .getOrNull()
}
