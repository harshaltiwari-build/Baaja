/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.filterExplicit
import com.metrolist.innertube.pages.NewReleasesPage
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewReleaseViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    private val database: MusicDatabase,
) : ViewModel() {
    private val _newReleaseAlbums = MutableStateFlow<List<AlbumItem>>(emptyList())
    val newReleaseAlbums = _newReleaseAlbums.asStateFlow()

    /** The featured playlists of YouTube Music's New releases page, when it loaded. */
    private val _featured = MutableStateFlow<List<PlaylistItem>>(emptyList())
    val featured = _featured.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _hasError = MutableStateFlow(false)
    val hasError = _hasError.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun load() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            _hasError.value = false
            val hideExplicit = context.dataStore.get(HideExplicitKey, true)
            // YouTube Music's own New releases page first, in YouTube's order, like the YouTube Music app.
            newReleasesContent(YouTube.newReleasesPage().getOrNull(), hideExplicit)?.let { content ->
                _featured.value = content.featured
                _newReleaseAlbums.value = content.albums
                _isLoading.value = false
                return@launch
            }
            // The dedicated page sometimes comes back empty (especially when signed in), so fall back to the
            // Explore feed and then to an album search; New releases should never be blank.
            var failure: Throwable? = null
            val loaded: Result<List<AlbumItem>> =
                YouTube.newReleaseAlbums().let { first ->
                    first.exceptionOrNull()?.let { failure = it }
                    if (first.getOrNull().orEmpty().isNotEmpty()) {
                        first
                    } else {
                        val fromExplore = YouTube.explore().getOrNull()?.newReleaseAlbums.orEmpty()
                        if (fromExplore.isNotEmpty()) {
                            Result.success(fromExplore)
                        } else {
                            val searched =
                                YouTube
                                    .search("new albums 2026", YouTube.SearchFilter.FILTER_ALBUM)
                                    .getOrNull()
                                    ?.items
                                    ?.filterIsInstance<AlbumItem>()
                                    .orEmpty()
                            if (searched.isNotEmpty()) Result.success(searched) else failure?.let { Result.failure(it) } ?: first
                        }
                    }
                }
            loaded
                .onSuccess { albums ->
                    val artists: MutableMap<Int, String> = mutableMapOf()
                    val favouriteArtists: MutableMap<Int, String> = mutableMapOf()
                    database.allArtistsByPlayTime().first().let { list ->
                        var favIndex = 0
                        for ((artistsIndex, artist) in list.withIndex()) {
                            artists[artistsIndex] = artist.id
                            if (artist.artist.bookmarkedAt != null) {
                                favouriteArtists[favIndex] = artist.id
                                favIndex++
                            }
                        }
                    }
                    _newReleaseAlbums.value =
                        albums
                            .sortedBy { album ->
                                val artistIds = album.artists.orEmpty().mapNotNull { it.id }
                                val firstArtistKey =
                                    artistIds.firstNotNullOfOrNull { artistId ->
                                        if (artistId in favouriteArtists.values) {
                                            favouriteArtists.entries.firstOrNull { it.value == artistId }?.key
                                        } else {
                                            artists.entries.firstOrNull { it.value == artistId }?.key
                                        }
                                    } ?: Int.MAX_VALUE
                                firstArtistKey
                            }.filterExplicit(context.dataStore.get(HideExplicitKey, true))
                    if (_newReleaseAlbums.value.isEmpty()) _hasError.value = true
                }.onFailure {
                    _hasError.value = true
                    reportException(it)
                }
            _isLoading.value = false
        }
    }
}

internal data class NewReleasesContent(
    val featured: List<PlaylistItem>,
    val albums: List<AlbumItem>,
)

/**
 * What New releases shows from YouTube Music's page, keeping YouTube's order, or null when the page has no
 * albums so the older sources are used instead. Baaja is music only, so the page's music videos are left out.
 */
internal fun newReleasesContent(
    page: NewReleasesPage?,
    hideExplicit: Boolean,
): NewReleasesContent? {
    page ?: return null
    val albums = page.albums.distinctBy { it.id }.filterExplicit(hideExplicit)
    if (albums.isEmpty()) return null
    return NewReleasesContent(
        featured = page.featured.distinctBy { it.id },
        albums = albums,
    )
}
