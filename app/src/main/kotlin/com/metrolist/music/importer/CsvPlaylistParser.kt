/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.importer

import java.util.Locale

/**
 * Reads playlist exports such as Exportify's CSV ("Track Name", "Artist Name(s)", "Album Name",
 * "Duration (ms)"…). Also copes with semicolon/tab separated files and simpler "Title,Artist" sheets.
 */
object CsvPlaylistParser {
    class MissingColumnException : IllegalArgumentException("No track title column found")

    private val titleHeaders =
        listOf("track name", "track title", "song name", "song title", "title", "name", "track", "song")
    private val artistHeaders =
        listOf("artist name(s)", "artist names", "artist name", "artist(s)", "artists", "artist", "album artist name(s)")
    private val albumHeaders = listOf("album name", "album title", "album")
    private val durationMsHeaders = listOf("duration (ms)", "track duration (ms)", "duration_ms", "duration ms")
    private val durationHeaders = listOf("duration", "length", "time")

    fun parse(text: String): ImportedPlaylist {
        val rows = parseRows(text).filter { row -> row.any { it.isNotBlank() } }
        if (rows.isEmpty()) return ImportedPlaylist(name = null, tracks = emptyList())
        val header = rows.first().map { normalizeHeader(it) }

        val titleCol = findColumn(header, titleHeaders)
        if (titleCol == null) {
            // No recognised header: accept a plain "title, artist" sheet without a header row.
            if (rows.first().size > 2) throw MissingColumnException()
            return ImportedPlaylist(
                name = null,
                tracks =
                    rows.mapNotNull { row ->
                        val title = row.first().trim().takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                        ImportTrack(title = title, artists = splitArtists(row.getOrNull(1).orEmpty()))
                    },
            )
        }
        val artistCol = findColumn(header, artistHeaders)
        val albumCol = findColumn(header, albumHeaders)
        val durationMsCol = findColumn(header, durationMsHeaders)
        val durationCol = if (durationMsCol == null) findColumn(header, durationHeaders) else null

        var skipped = 0
        val tracks =
            rows.drop(1).mapNotNull { row ->
                val title = row.getOrNull(titleCol)?.trim().orEmpty()
                if (title.isEmpty()) {
                    skipped++
                    return@mapNotNull null
                }
                ImportTrack(
                    title = title,
                    artists = artistCol?.let { splitArtists(row.getOrNull(it).orEmpty()) }.orEmpty(),
                    album = albumCol?.let { row.getOrNull(it)?.trim() }?.takeIf { it.isNotEmpty() },
                    durationMs =
                        durationMsCol?.let { row.getOrNull(it)?.trim()?.toDoubleOrNull()?.toLong() }
                            ?: durationCol?.let { parseClockDuration(row.getOrNull(it).orEmpty()) },
                )
            }
        return ImportedPlaylist(name = null, tracks = tracks, skipped = skipped)
    }

    /** RFC 4180 style parsing: quoted fields, doubled quotes, separators and line breaks inside quotes. */
    fun parseRows(text: String): List<List<String>> {
        val source = text.removePrefix("﻿")
        val delimiter = detectDelimiter(source)
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < source.length) {
            val c = source[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < source.length && source[i + 1] == '"') {
                        field.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    field.append(c)
                }
            } else {
                when (c) {
                    '"' -> if (field.isBlank()) { field.setLength(0); inQuotes = true } else field.append(c)
                    delimiter -> {
                        row += field.toString()
                        field.setLength(0)
                    }
                    '\r' -> Unit
                    '\n' -> {
                        row += field.toString()
                        field.setLength(0)
                        rows += row
                        row = ArrayList()
                    }
                    else -> field.append(c)
                }
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row += field.toString()
            rows += row
        }
        return rows
    }

    /** Exportify joins several artists with commas inside the quoted field; some tools use semicolons. */
    fun splitArtists(value: String): List<String> =
        value
            .split(',', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    /** "3:45" or "1:02:03" to milliseconds. */
    fun parseClockDuration(value: String): Long? {
        val parts = value.trim().split(':')
        if (parts.size !in 2..3) return null
        val numbers = parts.map { it.toIntOrNull() ?: return null }
        val seconds = numbers.fold(0L) { acc, n -> acc * 60 + n }
        return seconds * 1000
    }

    private fun detectDelimiter(text: String): Char {
        val firstLine = text.lineSequence().firstOrNull().orEmpty()
        var inQuotes = false
        val counts = mutableMapOf(',' to 0, ';' to 0, '\t' to 0)
        firstLine.forEach { c ->
            if (c == '"') inQuotes = !inQuotes
            if (!inQuotes && c in counts) counts[c] = counts.getValue(c) + 1
        }
        return counts.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.key ?: ','
    }

    private fun normalizeHeader(value: String): String = value.trim().trim('"').lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")

    private fun findColumn(header: List<String>, names: List<String>): Int? {
        for (name in names) {
            val index = header.indexOf(name)
            if (index >= 0) return index
        }
        return null
    }
}
