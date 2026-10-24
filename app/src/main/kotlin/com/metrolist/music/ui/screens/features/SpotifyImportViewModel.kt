/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.features

import java.time.LocalDateTime
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.constants.SpotifyClientIdKey
import com.metrolist.music.constants.SpotifyClientSecretKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.importer.CsvPlaylistParser
import com.metrolist.music.importer.ImportTrack
import com.metrolist.music.importer.ImportedPlaylist
import com.metrolist.music.importer.SpotifyApi
import com.metrolist.music.importer.SpotifyLinkParser
import com.metrolist.music.importer.YouTubeTrackMatcher
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

enum class ImportMode { LINK, CSV }

enum class ImportError {
    INVALID_LINK, SHORT_LINK, NOT_PLAYLIST, NO_KEYS, BAD_KEYS, NOT_FOUND, RATE_LIMITED, NETWORK, SERVER,
    CSV_COLUMNS, CSV_READ, EMPTY,
}

enum class MatchStatus { WAITING, SEARCHING, FOUND, NOT_FOUND, FAILED }

@Immutable
data class ImportRow(
    val index: Int,
    val track: ImportTrack,
    val status: MatchStatus = MatchStatus.WAITING,
    val match: SongItem? = null,
    val selected: Boolean = false,
)

sealed interface ImportPhase {
    data object Input : ImportPhase

    data class Reading(val loaded: Int, val total: Int) : ImportPhase

    data object Matching : ImportPhase

    data object Review : ImportPhase

    data object Saving : ImportPhase

    data class Done(val playlistId: String, val name: String, val count: Int) : ImportPhase
}

@HiltViewModel
class SpotifyImportViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) : ViewModel() {
    private val api = SpotifyApi()

    var mode by mutableStateOf(ImportMode.LINK)
    var link by mutableStateOf("")
    var clientId by mutableStateOf("")
    var clientSecret by mutableStateOf("")
    var keysSaved by mutableStateOf(false)
        private set
    var editingKeys by mutableStateOf(false)

    var phase by mutableStateOf<ImportPhase>(ImportPhase.Input)
        private set
    var error by mutableStateOf<ImportError?>(null)
        private set
    var serverCode by mutableIntStateOf(0)
        private set
    var playlistName by mutableStateOf("")
    var skipped by mutableIntStateOf(0)
        private set

    val rows = mutableStateListOf<ImportRow>()
    var checked by mutableIntStateOf(0)
        private set
    var found by mutableIntStateOf(0)
        private set
    var notFound by mutableIntStateOf(0)
        private set
    var failed by mutableIntStateOf(0)
        private set

    private var work: Job? = null

    init {
        viewModelScope.launch {
            val prefs = context.dataStore.data.first()
            clientId = prefs[SpotifyClientIdKey].orEmpty()
            clientSecret = prefs[SpotifyClientSecretKey].orEmpty()
            keysSaved = clientId.isNotBlank() && clientSecret.isNotBlank()
            editingKeys = !keysSaved
        }
    }

    fun saveKeys() {
        val id = clientId.trim()
        val secret = clientSecret.trim()
        if (id.isEmpty() || secret.isEmpty()) return
        clientId = id
        clientSecret = secret
        viewModelScope.launch {
            context.dataStore.edit {
                it[SpotifyClientIdKey] = id
                it[SpotifyClientSecretKey] = secret
            }
            keysSaved = true
            editingKeys = false
            if (error == ImportError.NO_KEYS || error == ImportError.BAD_KEYS) error = null
        }
    }

    fun clearKeys() {
        clientId = ""
        clientSecret = ""
        keysSaved = false
        editingKeys = true
        viewModelScope.launch {
            context.dataStore.edit {
                it.remove(SpotifyClientIdKey)
                it.remove(SpotifyClientSecretKey)
            }
        }
    }

    fun dismissError() {
        error = null
    }

    fun readSpotifyPlaylist(defaultName: String) {
        error = null
        val playlistId =
            when (val parsed = SpotifyLinkParser.parse(link)) {
                is SpotifyLinkParser.Result.Playlist -> parsed.id
                SpotifyLinkParser.Result.ShortLink -> return fail(ImportError.SHORT_LINK)
                SpotifyLinkParser.Result.NotAPlaylist -> return fail(ImportError.NOT_PLAYLIST)
                SpotifyLinkParser.Result.Invalid -> return fail(ImportError.INVALID_LINK)
            }
        if (!keysSaved || clientId.isBlank() || clientSecret.isBlank()) {
            editingKeys = true
            return fail(ImportError.NO_KEYS)
        }
        work?.cancel()
        work =
            viewModelScope.launch {
                phase = ImportPhase.Reading(0, 0)
                try {
                    val playlist =
                        api.fetchPlaylist(clientId, clientSecret, playlistId) { loaded, total ->
                            viewModelScope.launch { if (phase is ImportPhase.Reading) phase = ImportPhase.Reading(loaded, total) }
                        }
                    startMatching(playlist, defaultName)
                } catch (e: SpotifyApi.SpotifyException) {
                    phase = ImportPhase.Input
                    serverCode = e.httpCode ?: 0
                    fail(
                        when (e.failure) {
                            SpotifyApi.Failure.BAD_KEYS -> ImportError.BAD_KEYS.also { editingKeys = true }
                            SpotifyApi.Failure.NOT_FOUND -> ImportError.NOT_FOUND
                            SpotifyApi.Failure.RATE_LIMITED -> ImportError.RATE_LIMITED
                            SpotifyApi.Failure.NETWORK -> ImportError.NETWORK
                            SpotifyApi.Failure.SERVER -> ImportError.SERVER
                        },
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Never log the request itself: it carries the user's keys.
                    Timber.tag(TAG).w("Spotify playlist read failed: %s", e.javaClass.simpleName)
                    phase = ImportPhase.Input
                    fail(ImportError.SERVER)
                }
            }
    }

    fun readCsv(uri: Uri, defaultName: String) {
        error = null
        work?.cancel()
        work =
            viewModelScope.launch {
                phase = ImportPhase.Reading(0, 0)
                val result =
                    withContext(Dispatchers.IO) {
                        runCatching {
                            val text =
                                context.contentResolver.openInputStream(uri)?.use { stream ->
                                    stream.readBytes(MAX_CSV_BYTES).toString(Charsets.UTF_8)
                                } ?: throw IllegalStateException("No stream")
                            CsvPlaylistParser.parse(text).copy(name = displayName(uri))
                        }
                    }
                result
                    .onSuccess { startMatching(it, defaultName) }
                    .onFailure {
                        phase = ImportPhase.Input
                        fail(if (it is CsvPlaylistParser.MissingColumnException) ImportError.CSV_COLUMNS else ImportError.CSV_READ)
                    }
            }
    }

    private suspend fun startMatching(playlist: ImportedPlaylist, defaultName: String) {
        if (playlist.tracks.isEmpty()) {
            phase = ImportPhase.Input
            fail(ImportError.EMPTY)
            return
        }
        playlistName = playlist.name?.takeIf { it.isNotBlank() } ?: defaultName
        skipped = playlist.skipped
        rows.clear()
        rows.addAll(playlist.tracks.mapIndexed { index, track -> ImportRow(index, track) })
        checked = 0
        found = 0
        notFound = 0
        failed = 0
        phase = ImportPhase.Matching
        matchRows(rows.indices.toList())
    }

    private suspend fun matchRows(indices: List<Int>) {
        val semaphore = Semaphore(MATCH_CONCURRENCY)
        try {
            withContext(Dispatchers.IO) {
                indices.map { index ->
                    launch {
                        semaphore.withPermit {
                            val track = rows[index].track
                            withContext(Dispatchers.Main) { rows[index] = rows[index].copy(status = MatchStatus.SEARCHING) }
                            val outcome = YouTubeTrackMatcher.match(track)
                            withContext(Dispatchers.Main) { applyOutcome(index, outcome) }
                        }
                    }
                }.joinAll()
            }
        } finally {
            withContext(NonCancellable + Dispatchers.Main) {
                // Anything still pending after Stop counts as failed so it can be retried.
                rows.indices.forEach { i ->
                    val row = rows[i]
                    if (row.status == MatchStatus.WAITING || row.status == MatchStatus.SEARCHING) {
                        rows[i] = row.copy(status = MatchStatus.FAILED)
                        failed++
                        checked++
                    }
                }
                if (phase == ImportPhase.Matching) phase = ImportPhase.Review
            }
        }
    }

    private fun applyOutcome(index: Int, outcome: YouTubeTrackMatcher.Outcome) {
        val row = rows[index]
        if (row.status != MatchStatus.SEARCHING) return
        rows[index] =
            when (outcome) {
                is YouTubeTrackMatcher.Outcome.Found -> {
                    found++
                    row.copy(status = MatchStatus.FOUND, match = outcome.song, selected = true)
                }
                YouTubeTrackMatcher.Outcome.NotFound -> {
                    notFound++
                    row.copy(status = MatchStatus.NOT_FOUND)
                }
                YouTubeTrackMatcher.Outcome.Failed -> {
                    failed++
                    row.copy(status = MatchStatus.FAILED)
                }
            }
        checked++
    }

    fun stopMatching() {
        work?.cancel()
    }

    fun retryFailed() {
        val indices = rows.filter { it.status == MatchStatus.FAILED }.map { it.index }
        if (indices.isEmpty()) return
        indices.forEach { rows[it] = rows[it].copy(status = MatchStatus.WAITING) }
        failed -= indices.size
        checked -= indices.size
        phase = ImportPhase.Matching
        work = viewModelScope.launch { matchRows(indices) }
    }

    fun toggle(index: Int) {
        val row = rows.getOrNull(index) ?: return
        if (row.match == null) return
        rows[index] = row.copy(selected = !row.selected)
    }

    fun selectAll(selected: Boolean) {
        rows.indices.forEach { i ->
            val row = rows[i]
            if (row.match != null && row.selected != selected) rows[i] = row.copy(selected = selected)
        }
    }

    val selectedCount: Int
        get() = rows.count { it.selected && it.match != null }

    fun save(defaultName: String) {
        val songs = rows.filter { it.selected }.mapNotNull { it.match }.distinctBy { it.id }
        if (songs.isEmpty() || phase != ImportPhase.Review) return
        val name = playlistName.trim().ifEmpty { defaultName }
        phase = ImportPhase.Saving
        viewModelScope.launch {
            try {
                val entity = PlaylistEntity(name = name, bookmarkedAt = LocalDateTime.now(), isEditable = true)
                withContext(Dispatchers.IO) {
                    database.withTransaction {
                        insert(entity)
                        songs.forEach { insert(it.toMediaMetadata()) }
                    }
                    val playlist = database.playlist(entity.id).firstOrNull() ?: throw IllegalStateException("Playlist missing")
                    database.addSongsToPlaylist(playlist, songs.map { it.id to null })
                }
                phase = ImportPhase.Done(entity.id, name, songs.size)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Saving imported playlist failed")
                phase = ImportPhase.Review
            }
        }
    }

    fun startOver() {
        work?.cancel()
        rows.clear()
        link = ""
        error = null
        phase = ImportPhase.Input
    }

    private fun fail(kind: ImportError) {
        error = kind
    }

    private fun displayName(uri: Uri): String? =
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0)?.substringBeforeLast('.') else null
            }
        }.getOrNull()?.takeIf { it.isNotBlank() }

    private fun java.io.InputStream.readBytes(limit: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        while (out.size() < limit) {
            val read = read(buffer)
            if (read < 0) break
            out.write(buffer, 0, minOf(read, limit - out.size()))
        }
        return out.toByteArray()
    }

    private companion object {
        const val TAG = "SpotifyImport"
        const val MATCH_CONCURRENCY = 4
        const val MAX_CSV_BYTES = 5 * 1024 * 1024
    }
}
