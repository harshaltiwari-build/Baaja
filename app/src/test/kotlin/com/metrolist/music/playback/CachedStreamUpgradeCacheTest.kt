package com.metrolist.music.playback

import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files

/**
 * The upgrade rule against Media3's real song cache, the way MusicService's resolver uses it: a
 * data-saving copy is dropped once, and the better copy fetched afterwards is kept on later plays.
 */
@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class CachedStreamUpgradeCacheTest {
    private lateinit var dir: File
    private lateinit var cache: SimpleCache
    private val upgrader = CachedStreamUpgrader()

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("baaja-cache").toFile()
        cache = SimpleCache(dir, NoOpCacheEvictor(), StandaloneDatabaseProvider(ApplicationProvider.getApplicationContext()))
    }

    @After
    fun tearDown() {
        cache.release()
        dir.deleteRecursively()
    }

    /** Fills the cache for [key] the way playback does: through a CacheDataSource. */
    private fun write(key: String, size: Int) {
        val source = CacheDataSource(cache, ByteArrayDataSource(ByteArray(size)))
        CacheWriter(source, DataSpec.Builder().setUri("data://$key").setKey(key).build(), null, null).cache()
    }

    /** What the resolver does when a song starts playing. */
    private fun play(key: String, storedItag: Int) {
        if (upgrader.shouldUpgrade(key, position = 0, storedItag = storedItag, prefersLowQuality = false)) {
            cache.removeResource(key)
        }
    }

    @Test
    fun `a low quality copy is dropped once and the better copy then stays`() {
        write("song", 1000)
        play("song", storedItag = 139)
        assertFalse("data-saving copy dropped", cache.isCached("song", 0, 1000))

        write("song", 4000) // the better stream fetched after the drop
        repeat(5) { play("song", storedItag = 139) } // even if the format row still says 139
        assertTrue("better copy kept on later plays", cache.isCached("song", 0, 4000))
    }

    @Test
    fun `other songs in the cache are untouched`() {
        write("low", 1000)
        write("good", 1000)
        play("low", storedItag = 139)
        play("good", storedItag = 251)
        assertFalse(cache.isCached("low", 0, 1000))
        assertTrue(cache.isCached("good", 0, 1000))
    }
}
