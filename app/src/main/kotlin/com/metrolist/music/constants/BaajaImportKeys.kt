/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/** The user's own Spotify developer app keys, used only for the playlist import. */
val SpotifyClientIdKey = stringPreferencesKey("baaja_spotify_client_id")
val SpotifyClientSecretKey = stringPreferencesKey("baaja_spotify_client_secret")

/** Local music screen: last chosen sort order. */
val LocalMusicSortKey = stringPreferencesKey("baaja_local_music_sort")
val LocalMusicSortDescendingKey = booleanPreferencesKey("baaja_local_music_sort_desc")
