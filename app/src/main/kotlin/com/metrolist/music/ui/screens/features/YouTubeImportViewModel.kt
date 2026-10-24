/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.features

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.pages.PlaylistPage
import com.metrolist.innertube.utils.completed
import com.metrolist.music.constants.YoutubeImportHintSeenKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.importer.YouTubePlaylistLinkParser
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.time.LocalDateTime
import javax.inject.Inject

enum class YtiError { INVALID_LINK, NOT_PLAYLIST, PRIVATE, NETWORK }

enum class YtiDuplicate { REPLACE, ADD, BOTH }

@Immutable
data class YtiPreview(
    val id: String,
    val title: String,
    val author: String?,
    val thumbnail: String?,
    val songCount: Int?,
    val countText: String?,
)

@Immutable
data class YtiConflict(val title: String, val newName: String)

@Immutable
data class YtiResult(val playlistId: String, val name: String, val count: Int)

sealed interface YtiPhase {
    data object Input : YtiPhase

    data object Fetching : YtiPhase

    data class Preview(val info: YtiPreview) : YtiPhase

    /** [saving] is true once all pages are loaded and the songs are being written to the database. */
    data class Importing(
        val index: Int,
        val count: Int,
        val name: String,
        val loaded: Int,
        val total: Int?,
        val saving: Boolean,
    ) : YtiPhase

    data class Done(val results: List<YtiResult>, val failed: Int) : YtiPhase
}

enum class YtiAccountState { IDLE, LOADING, READY, FAILED }

private class YtiException(val error: YtiError) : Exception()

private class LoadedPlaylist(val title: String, val songs: List<SongItem>)

private class ImportRequest(val id: String, val title: String, val first: PlaylistPage?)

@HiltViewModel
class YouTubeImportViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) : ViewModel() {
    var link by mutableStateOf("")

    var phase by mutableStateOf<YtiPhase>(YtiPhase.Input)
        private set
    var error by mutableStateOf<YtiError?>(null)
        private set
    var conflict by mutableStateOf<YtiConflict?>(null)
        private set
    var hintVisible by mutableStateOf(false)
        private set

    var accountState by mutableStateOf(YtiAccountState.IDLE)
        private set
    var accountPlaylists by mutableStateOf<List<PlaylistItem>>(emptyList())
        private set
    var selected by mutableStateOf<Set<String>>(emptySet())
        private set

    private var work: Job? = null
    private var previewPage: PlaylistPage? = null
    private var currentPreview: YtiPreview? = null
    private var retryAction: (() -> Unit)? = null

    init {
        viewModelScope.launch {
            hintVisible = context.dataStore.data.first()[YoutubeImportHintSeenKey] != true
        }
    }

    fun dismissHint() {
        hintVisible = false
        viewModelScope.launch { context.dataStore.edit { it[YoutubeImportHintSeenKey] = true } }
    }

    fun onLinkChange(value: String) {
        link = value
        if (error == YtiError.INVALID_LINK || error == YtiError.NOT_PLAYLIST) error = null
    }

    // region account playlists

    fun loadAccountPlaylists(force: Boolean = false) {
        if (!force && accountState != YtiAccountState.IDLE && accountState != YtiAccountState.FAILED) return
        accountState = YtiAccountState.LOADING
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { YouTube.library("FEmusic_liked_playlists").completed() }
            result
                .onSuccess { page ->
                    accountPlaylists =
                        page.items
                            .filterIsInstance<PlaylistItem>()
                            .filter { !it.isPodcast && YouTubePlaylistLinkParser.normalizeId(it.id) != null }
                            .distinctBy { it.id }
                    selected = selected.filter { id -> accountPlaylists.any { it.id == id } }.toSet()
                    accountState = YtiAccountState.READY
                }.onFailure {
                    if (it is CancellationException) throw it
                    Timber.tag(TAG).w(it, "Loading account playlists failed")
                    accountState = YtiAccountState.FAILED
                }
        }
    }

    fun toggleSelected(id: String) {
        selected = if (id in selected) selected - id else selected + id
    }

    fun selectAll(all: Boolean) {
        selected = if (all) accountPlaylists.map { it.id }.toSet() else emptySet()
    }

    fun importSelected() {
        if (phase != YtiPhase.Input) return
        val requests =
            accountPlaylists
                .filter { it.id in selected }
                .map { ImportRequest(YouTubePlaylistLinkParser.normalizeId(it.id) ?: it.id, it.title, null) }
        if (requests.isEmpty()) return
        error = null
        runImport(requests, YtiDuplicate.BOTH)
    }

    // endregion

    // region link flow

    fun previewLink() {
        error = null
        val id =
            when (val parsed = YouTubePlaylistLinkParser.parse(link)) {
                is YouTubePlaylistLinkParser.Result.Playlist -> parsed.id
                YouTubePlaylistLinkParser.Result.NotAPlaylist -> return fail(YtiError.NOT_PLAYLIST)
                YouTubePlaylistLinkParser.Result.Invalid -> return fail(YtiError.INVALID_LINK)
            }
        retryAction = { previewLink() }
        previewPage = null
        currentPreview = null
        work?.cancel()
        work =
            viewModelScope.launch {
                phase = YtiPhase.Fetching
                try {
                    val page = withContext(Dispatchers.IO) { fetchFirstPage(id) }
                    previewPage = page
                    val item = page.playlist
                    val countText = item.songCountText
                    val info =
                        YtiPreview(
                            id = id,
                            title = item.title,
                            author = item.author?.name,
                            thumbnail = item.thumbnail,
                            songCount = parseCount(countText),
                            countText = countText,
                        )
                    currentPreview = info
                    phase = YtiPhase.Preview(info)
                } catch (e: YtiException) {
                    phase = YtiPhase.Input
                    fail(e.error)
                }
            }
    }

    fun startImport() {
        val info = (phase as? YtiPhase.Preview)?.info ?: return
        val title = info.title.ifBlank { UNTITLED }
        viewModelScope.launch {
            val existing = withContext(Dispatchers.IO) { localPlaylistsNamed(title).firstOrNull() }
            if (existing == null) {
                runImport(listOf(request(info)), YtiDuplicate.BOTH)
            } else {
                conflict = YtiConflict(existing.name, withContext(Dispatchers.IO) { uniqueName(title) })
            }
        }
    }

    fun resolveConflict(policy: YtiDuplicate) {
        val info = (phase as? YtiPhase.Preview)?.info ?: return
        conflict = null
        runImport(listOf(request(info)), policy)
    }

    fun dismissConflict() {
        conflict = null
    }

    private fun request(info: YtiPreview) = ImportRequest(info.id, info.title, previewPage)

    // endregion

    fun cancel() {
        work?.cancel()
        phase = currentPreview?.let { YtiPhase.Preview(it) } ?: YtiPhase.Input
    }

    fun retry() {
        val action = retryAction ?: return
        error = null
        action()
    }

    fun dismissError() {
        error = null
    }

    fun startOver() {
        work?.cancel()
        previewPage = null
        currentPreview = null
        link = ""
        error = null
        conflict = null
        retryAction = null
        phase = YtiPhase.Input
    }

    private fun fail(kind: YtiError) {
        error = kind
    }

    private fun runImport(requests: List<ImportRequest>, policy: YtiDuplicate) {
        retryAction = { runImport(requests, policy) }
        work?.cancel()
        work =
            viewModelScope.launch {
                val results = mutableListOf<YtiResult>()
                var failed = 0
                var lastError: YtiError? = null
                try {
                    requests.forEachIndexed { index, request ->
                        val label = request.title.ifBlank { UNTITLED }
                        phase = YtiPhase.Importing(index + 1, requests.size, label, 0, null, false)
                        try {
                            val loaded =
                                withContext(Dispatchers.IO) {
                                    loadAll(request.id, request.first) { count, total ->
                                        phase = YtiPhase.Importing(index + 1, requests.size, label, count, total, false)
                                    }
                                }
                            phase = YtiPhase.Importing(index + 1, requests.size, label, loaded.songs.size, loaded.songs.size, true)
                            results += withContext(Dispatchers.IO) { save(loaded.title.ifBlank { label }, loaded.songs, policy) }
                        } catch (e: YtiException) {
                            failed++
                            lastError = e.error
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.tag(TAG).w(e, "Saving imported playlist failed")
                    failed = requests.size - results.size
                    lastError = lastError ?: YtiError.NETWORK
                }
                if (results.isEmpty()) {
                    // Nothing imported: go back to where the user was, with a retryable error.
                    phase = currentPreview?.takeIf { requests.size == 1 && previewPage != null }?.let { YtiPhase.Preview(it) } ?: YtiPhase.Input
                    fail(lastError ?: YtiError.NETWORK)
                } else {
                    phase = YtiPhase.Done(results, failed)
                }
            }
    }

    // region fetching

    private suspend fun fetchFirstPage(id: String): PlaylistPage {
        val page = YouTube.playlist(id).getOrElse { throw classify(it) }
        if (page.songs.isEmpty() && page.songsContinuation == null) throw YtiException(YtiError.PRIVATE)
        return page
    }

    /** Loads every page of the playlist, following continuation tokens, keeping order and dropping repeats. */
    private suspend fun loadAll(
        id: String,
        first: PlaylistPage?,
        onProgress: (loaded: Int, total: Int?) -> Unit,
    ): LoadedPlaylist {
        val page = first ?: fetchFirstPage(id)
        val total = parseCount(page.playlist.songCountText)
        val songs = LinkedHashMap<String, SongItem>()
        page.songs.forEach { songs.putIfAbsent(it.id, it) }
        onProgress(songs.size, total)
        var continuation = page.songsContinuation
        val seen = mutableSetOf<String>()
        while (continuation != null) {
            currentCoroutineContext().ensureActive()
            if (!seen.add(continuation) || seen.size > MAX_PAGES) break
            val next = YouTube.playlistContinuation(continuation).getOrElse { throw classify(it, afterFirstPage = true) }
            next.songs.forEach { songs.putIfAbsent(it.id, it) }
            continuation = next.continuation
            onProgress(songs.size, total)
        }
        return LoadedPlaylist(page.playlist.title, songs.values.toList())
    }

    private fun classify(error: Throwable, afterFirstPage: Boolean = false): Exception {
        if (error is CancellationException) return error
        Timber.tag(TAG).w(error, "YouTube playlist request failed")
        return YtiException(if (error is IOException || afterFirstPage) YtiError.NETWORK else YtiError.PRIVATE)
    }

    private fun parseCount(text: String?): Int? =
        text
            ?.let { Regex("""\d[\d.,\s]*""").find(it)?.value }
            ?.filter { it.isDigit() }
            ?.toIntOrNull()
            ?.takeIf { it > 0 }

    // endregion

    // region saving

    private suspend fun localPlaylistsNamed(title: String): List<PlaylistEntity> =
        database.playlistEntitiesByNameAsc().filter {
            it.bookmarkedAt != null && it.browseId == null && it.name.trim().equals(title.trim(), ignoreCase = true)
        }

    private suspend fun uniqueName(title: String): String {
        val taken = database.playlistEntitiesByNameAsc().map { it.name.trim().lowercase() }.toSet()
        val base = title.trim()
        var n = 2
        while ("$base ($n)".lowercase() in taken) n++
        return "$base ($n)"
    }

    private suspend fun save(title: String, songs: List<SongItem>, policy: YtiDuplicate): YtiResult {
        val existing = localPlaylistsNamed(title).firstOrNull()
        val metadata = songs.map { it.toMediaMetadata() }
        if (existing != null && policy != YtiDuplicate.BOTH) {
            val playlist = database.playlist(existing.id).firstOrNull() ?: throw IllegalStateException("Playlist missing")
            val toAdd =
                if (policy == YtiDuplicate.REPLACE) {
                    database.withTransaction {
                        clearPlaylist(existing.id)
                        metadata.forEach { insert(it) }
                    }
                    metadata
                } else {
                    val present = database.playlistSongs(existing.id).first().map { it.song.id }.toSet()
                    val fresh = metadata.filter { it.id !in present }
                    database.withTransaction { fresh.forEach { insert(it) } }
                    fresh
                }
            database.addSongsToPlaylist(playlist, toAdd.map { it.id to null })
            return YtiResult(existing.id, existing.name, toAdd.size)
        }

        val name = if (existing != null) uniqueName(title) else title.trim().ifEmpty { UNTITLED }
        val entity = PlaylistEntity(name = name, bookmarkedAt = LocalDateTime.now())
        database.withTransaction {
            insert(entity)
            metadata.forEach { insert(it) }
        }
        val playlist = database.playlist(entity.id).firstOrNull() ?: throw IllegalStateException("Playlist missing")
        database.addSongsToPlaylist(playlist, metadata.map { it.id to null })
        return YtiResult(entity.id, name, metadata.size)
    }

    // endregion

    private companion object {
        const val TAG = "YouTubeImport"
        const val UNTITLED = "YouTube Music playlist"
        const val MAX_PAGES = 300
    }
}
