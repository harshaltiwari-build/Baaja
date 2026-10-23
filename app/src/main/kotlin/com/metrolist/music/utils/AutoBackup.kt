/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.content.Context
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.metrolist.music.R
import com.metrolist.music.constants.AutoBackupFolderKey
import com.metrolist.music.constants.AutoBackupFrequency
import com.metrolist.music.db.MusicDatabase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/** Schedules (or cancels) periodic backups into the folder the user picked. */
object AutoBackup {
    private const val WORK_NAME = "auto_backup"
    const val KEEP_LATEST = 5

    fun schedule(context: Context, frequency: AutoBackupFrequency) {
        val workManager = WorkManager.getInstance(context)
        val days =
            when (frequency) {
                AutoBackupFrequency.OFF -> {
                    workManager.cancelUniqueWork(WORK_NAME)
                    return
                }
                AutoBackupFrequency.DAILY -> 1L
                AutoBackupFrequency.WEEKLY -> 7L
            }
        val request =
            PeriodicWorkRequestBuilder<AutoBackupWorker>(days, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()
        workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun filePrefix(context: Context) = "${context.getString(R.string.app_name)}_auto_"
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AutoBackupEntryPoint {
    fun database(): MusicDatabase
}

class AutoBackupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val folderUri = applicationContext.dataStore.get(AutoBackupFolderKey)?.toUri() ?: return Result.failure()
        val folder = DocumentFile.fromTreeUri(applicationContext, folderUri)
        if (folder == null || !folder.canWrite()) {
            Timber.tag(TAG).w("Backup folder is missing or no longer writable")
            return Result.failure()
        }

        val prefix = AutoBackup.filePrefix(applicationContext)
        val name = prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".backup"
        val file = folder.createFile("application/octet-stream", name) ?: return Result.retry()
        val database = EntryPointAccessors.fromApplication(applicationContext, AutoBackupEntryPoint::class.java).database()

        return runCatching {
            applicationContext.contentResolver.openOutputStream(file.uri)?.use {
                BackupWriter.write(applicationContext, database, it)
            } ?: error("Couldn't open ${file.uri}")

            folder.listFiles()
                .filter { it.name?.startsWith(prefix) == true }
                .sortedByDescending { it.lastModified() }
                .drop(AutoBackup.KEEP_LATEST)
                .forEach { it.delete() }
            Result.success()
        }.getOrElse {
            Timber.tag(TAG).w(it, "Automatic backup failed")
            file.delete()
            Result.retry()
        }
    }

    private companion object {
        const val TAG = "AutoBackup"
    }
}
