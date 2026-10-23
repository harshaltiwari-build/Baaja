/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.local

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/** Reads music files from MediaStore. Needs [audioPermission] to be granted. */
object LocalMusicScanner {
    val audioPermission: String
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED

    suspend fun scan(context: Context): List<LocalAudio> =
        withContext(Dispatchers.IO) {
            if (!hasPermission(context)) return@withContext emptyList()
            val collection =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                } else {
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
            val projection =
                arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.ALBUM_ID,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.DATE_ADDED,
                    MediaStore.Audio.Media.YEAR,
                    MediaStore.Audio.Media.TRACK,
                )
            // IS_MUSIC skips ringtones, alarms, notification sounds and recordings; the duration
            // floor catches short clips that are still tagged as music.
            val selection =
                "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?"
            val args = arrayOf(LocalLibrary.MIN_DURATION_MS.toString())
            val songs = ArrayList<LocalAudio>()
            try {
                context.contentResolver
                    .query(collection, projection, selection, args, null)
                    ?.use { cursor ->
                        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                        val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                        val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                        val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                        val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                        val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                        val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                        val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                        val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(idCol)
                            val title =
                                LocalLibrary.cleanTag(cursor.getString(titleCol)).ifEmpty {
                                    cursor.getString(nameCol)?.substringBeforeLast('.').orEmpty()
                                }
                            songs +=
                                LocalAudio(
                                    mediaStoreId = id,
                                    uri = ContentUris.withAppendedId(collection, id).toString(),
                                    title = title.ifEmpty { id.toString() },
                                    artist = LocalLibrary.cleanTag(cursor.getString(artistCol)),
                                    album = LocalLibrary.cleanTag(cursor.getString(albumCol)),
                                    albumId = cursor.getLong(albumIdCol),
                                    durationMs = cursor.getLong(durationCol),
                                    dateAddedSec = cursor.getLong(addedCol),
                                    year = cursor.getInt(yearCol),
                                    // TRACK is disc * 1000 + track on most devices.
                                    trackNumber = cursor.getInt(trackCol),
                                )
                        }
                    }
            } catch (e: SecurityException) {
                Timber.tag(TAG).w(e, "Audio permission was revoked during the scan")
            } catch (e: IllegalArgumentException) {
                Timber.tag(TAG).w(e, "MediaStore rejected the audio query")
            }
            songs
        }

    private const val TAG = "LocalMusicScanner"
}
