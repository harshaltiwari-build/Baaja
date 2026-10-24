/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Downloads the release APK into the app cache and hands it to the system installer.
 * Only this repository's GitHub release files are accepted, the size must match the release listing,
 * and the file must be signed with the same key as the installed app before the installer opens.
 */
class UpdateInstaller(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    enum class Phase { Idle, Downloading, ReadyToInstall, NeedsPermission, Failed }

    var phase by mutableStateOf(Phase.Idle)
        private set
    var progress by mutableStateOf(0f)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    private var job: Job? = null

    fun start(release: ReleaseInfo) {
        if (phase == Phase.Downloading) return
        val asset = Updater.getAssetForCurrentVariant(release)
        if (asset == null) {
            fail("No download for this build")
            return
        }
        if (!asset.downloadUrl.startsWith(ALLOWED_PREFIX)) {
            fail("Unexpected download address")
            return
        }
        phase = Phase.Downloading
        progress = 0f
        errorMessage = null
        job =
            scope.launch {
                val result = withContext(Dispatchers.IO) { runCatching { download(asset) } }
                result
                    .onSuccess { file ->
                        if (!isSignedLikeThisApp(file)) {
                            file.delete()
                            fail("The downloaded file is not signed by Baaja")
                        } else {
                            install(file)
                        }
                    }.onFailure { fail(it.message ?: "Download failed") }
            }
    }

    fun cancel() {
        job?.cancel()
        phase = Phase.Idle
    }

    /** Called again after the user grants "install unknown apps" to Baaja. */
    fun retryInstall() {
        val file = apkFile()
        if (file.exists()) install(file) else phase = Phase.Idle
    }

    private fun fail(message: String) {
        errorMessage = message
        phase = Phase.Failed
    }

    private fun apkFile(): File = File(File(context.cacheDir, "updates").apply { mkdirs() }, "Baaja-update.apk")

    private fun download(asset: ReleaseAsset): File {
        val target = apkFile()
        target.delete()
        val connection = URL(asset.downloadUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.instanceFollowRedirects = true
        try {
            check(connection.responseCode == HttpURLConnection.HTTP_OK) { "Server answered ${connection.responseCode}" }
            val total = if (asset.size > 0) asset.size else connection.contentLengthLong
            var done = 0L
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        done += read
                        if (total > 0) progress = (done.toFloat() / total).coerceIn(0f, 1f)
                    }
                }
            }
            check(asset.size <= 0 || done == asset.size) { "The file arrived incomplete" }
        } finally {
            connection.disconnect()
        }
        return target
    }

    @Suppress("DEPRECATION")
    private fun signerDigests(info: android.content.pm.PackageInfo?): Set<String> {
        val signatures =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info?.signingInfo?.apkContentsSigners
            } else {
                info?.signatures
            }
        return signatures
            .orEmpty()
            .map { signature -> MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02x".format(it) } }
            .toSet()
    }

    @Suppress("DEPRECATION")
    private fun isSignedLikeThisApp(file: File): Boolean =
        runCatching {
            val flags =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    PackageManager.GET_SIGNING_CERTIFICATES
                } else {
                    PackageManager.GET_SIGNATURES
                }
            val downloaded = context.packageManager.getPackageArchiveInfo(file.absolutePath, flags)
            val installed = context.packageManager.getPackageInfo(context.packageName, flags)
            val mine = signerDigests(installed)
            mine.isNotEmpty() && signerDigests(downloaded).any { it in mine }
        }.getOrDefault(false)

    private fun install(file: File) {
        if (!context.packageManager.canRequestPackageInstalls()) {
            phase = Phase.NeedsPermission
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", file)
        val intent =
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        phase = Phase.ReadyToInstall
        context.startActivity(intent)
    }

    private companion object {
        const val ALLOWED_PREFIX = "https://github.com/harshaltiwari47-dot/Baaja/releases/download/"
    }
}
