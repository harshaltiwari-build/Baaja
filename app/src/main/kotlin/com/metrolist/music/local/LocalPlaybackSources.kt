/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.local

import android.content.Context
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorsFactory

private fun Uri.isLocalAudio(): Boolean = scheme == "content" || scheme == "file"

/**
 * Sends `content://` / `file://` items straight to the platform reader and everything else to
 * [remote] (the YouTube resolver with its caches). Local files therefore never reach InnerTube
 * stream resolution and are never copied into the song cache.
 */
@UnstableApi
class LocalAwareDataSourceFactory(
    context: Context,
    private val remote: DataSource.Factory,
) : DataSource.Factory {
    private val local = DefaultDataSource.Factory(context.applicationContext)

    override fun createDataSource(): DataSource = LocalAwareDataSource(local, remote)
}

@UnstableApi
private class LocalAwareDataSource(
    private val localFactory: DataSource.Factory,
    private val remoteFactory: DataSource.Factory,
) : DataSource {
    private val listeners = ArrayList<TransferListener>()
    private var localSource: DataSource? = null
    private var remoteSource: DataSource? = null
    private var active: DataSource? = null

    override fun addTransferListener(transferListener: TransferListener) {
        listeners += transferListener
        localSource?.addTransferListener(transferListener)
        remoteSource?.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val source =
            if (dataSpec.uri.isLocalAudio()) {
                localSource ?: localFactory.createDataSource().also { created ->
                    listeners.forEach(created::addTransferListener)
                    localSource = created
                }
            } else {
                remoteSource ?: remoteFactory.createDataSource().also { created ->
                    listeners.forEach(created::addTransferListener)
                    remoteSource = created
                }
            }
        active = source
        return source.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        checkNotNull(active) { "read() before open()" }.read(buffer, offset, length)

    override fun getUri(): Uri? = active?.uri

    override fun getResponseHeaders(): Map<String, List<String>> = active?.responseHeaders ?: emptyMap()

    override fun close() {
        val source = active
        active = null
        source?.close()
    }
}

/**
 * The streaming extractors Baaja uses for YouTube audio only understand WebM/MP4. Files on the
 * phone can be MP3, FLAC, OGG, WAV…, so local URIs get the full default extractor set.
 */
@UnstableApi
class LocalAwareExtractorsFactory(
    private val streaming: ExtractorsFactory,
) : ExtractorsFactory {
    private val full = DefaultExtractorsFactory()

    override fun createExtractors(): Array<Extractor> = streaming.createExtractors()

    override fun createExtractors(
        uri: Uri,
        responseHeaders: Map<String, List<String>>,
    ): Array<Extractor> =
        if (uri.isLocalAudio()) {
            full.createExtractors(uri, responseHeaders)
        } else {
            streaming.createExtractors(uri, responseHeaders)
        }
}
