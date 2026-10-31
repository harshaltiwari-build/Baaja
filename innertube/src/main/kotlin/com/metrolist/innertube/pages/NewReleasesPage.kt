package com.metrolist.innertube.pages

import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem

/** YouTube Music's New releases page: featured playlists, "Albums & singles" and "Music videos", in its order. */
data class NewReleasesPage(
    val featured: List<PlaylistItem>,
    val albums: List<AlbumItem>,
    val videos: List<SongItem>,
)
