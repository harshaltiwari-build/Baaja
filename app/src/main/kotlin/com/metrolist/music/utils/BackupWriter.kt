/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.content.Context
import com.metrolist.music.db.InternalDatabase
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.extensions.div
import com.metrolist.music.extensions.zipOutputStream
import com.metrolist.music.viewmodels.BackupRestoreViewModel.Companion.SETTINGS_FILENAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.util.zip.ZipEntry

/** Writes a full backup (settings, artist aliases and the library database) as a zip into [output]. */
object BackupWriter {
    fun write(context: Context, database: MusicDatabase, output: OutputStream) {
        output.buffered().zipOutputStream().use { zip ->
            (context.filesDir / "datastore" / SETTINGS_FILENAME).inputStream().buffered().use { input ->
                zip.putNextEntry(ZipEntry(SETTINGS_FILENAME))
                input.copyTo(zip)
            }
            zip.putNextEntry(ZipEntry(ArtistNameAliases.BACKUP_FILENAME))
            zip.write(ArtistNameAliases.serialize().encodeToByteArray())

            runBlocking(Dispatchers.IO) { database.checkpoint() }
            val dbPath = database.openHelper.writableDatabase.path ?: return@use
            for ((file, entry) in listOf(
                File(dbPath) to InternalDatabase.DB_NAME,
                File("$dbPath-wal") to "${InternalDatabase.DB_NAME}-wal",
                File("$dbPath-shm") to "${InternalDatabase.DB_NAME}-shm",
            )) {
                if (!file.exists()) continue
                FileInputStream(file).use { input ->
                    zip.putNextEntry(ZipEntry(entry))
                    input.copyTo(zip)
                }
            }
        }
    }
}
