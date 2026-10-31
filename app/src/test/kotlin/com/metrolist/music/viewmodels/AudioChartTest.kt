package com.metrolist.music.viewmodels

import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.SongItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * Charts as audio songs only, like YouTube Music's "Top 100 Songs India": each music video in YouTube's chart
 * is swapped for its official audio song. Cases are real entries from the India charts on 2026-11-01.
 */
class AudioChartTest {
    private fun omv(title: String, vararg artists: String, type: String = "MUSIC_VIDEO_TYPE_OMV") =
        SongItem(id = "v_$title", title = title, artists = artists.map { Artist(it, null) }, thumbnail = "", musicVideoType = type)

    private fun atv(title: String, vararg artists: String) =
        SongItem(id = "a_$title", title = title, artists = artists.map { Artist(it, null) }, thumbnail = "", musicVideoType = "MUSIC_VIDEO_TYPE_ATV")

    // ---- unit: cleaning video titles ----

    @Test
    fun `video title noise is removed`() {
        assertEquals("mallepoola pallaki", normalizedSongTitle("MALLEPOOLA PALLAKI (Song) | Ravi Teja | GV Prakash | Dappu Srinu"))
        assertEquals("kaali hoodie", normalizedSongTitle("KAALI HOODIE (MUSIC VIDEO) SIDHU MOOSE WALA FT BOHEMIA | THE KIDD"))
        assertEquals("ala bolelo", normalizedSongTitle("Ala Bolelo - Lyric Video | Jailer 2 | Superstar Rajinikanth"))
        assertEquals("casa tupka anthemo feat priyanshi srivastava", normalizedSongTitle("Casa Tupka Anthemo (feat. Priyanshi Srivastava)"))
        assertEquals("kalyani remix", normalizedSongTitle("KALYANI (Remix) (Official Video)"))
    }

    // ---- unit: picking the audio song ----

    @Test
    fun `same title and artist is matched`() {
        val pick = audioSongFor(omv("KALYANI (Remix)", "ARJN", "KDS"), listOf(atv("KALYANI (Remix)", "ARJN", "KDS", "FIFTY4"), atv("KALYANI", "ARJN", "RONN")))
        assertEquals("KALYANI (Remix)", pick?.title)
    }

    @Test
    fun `the song's longer official title is matched`() {
        val pick =
            audioSongFor(
                omv("Yeshanagula", "Jangi Reddy", "Anirudh Ravichander"),
                listOf(atv("Yeshanagula (From \"The Paradise\") (Telugu)", "Anirudh Ravichander", "Jangi Reddy"), atv("Yesha Nagula Kattameda", "Veeresha Lingam")),
            )
        assertEquals("Yeshanagula (From \"The Paradise\") (Telugu)", pick?.title)
    }

    @Test
    fun `artist names written differently still match`() {
        val pick = audioSongFor(omv("MALLEPOOLA PALLAKI (Song) | Ravi Teja", "G. V. Prakash Kumar"), listOf(atv("Mallepoola Pallaki", "G.V. Prakash Kumar")))
        assertEquals("Mallepoola Pallaki", pick?.title)
    }

    @Test
    fun `a fan or tv channel upload matches the top song with the same title`() {
        // YouTube Music itself shows "Ala Bolelo (From "Jailer 2")" for this Sun TV lyric video.
        val pick =
            audioSongFor(
                omv("Ala Bolelo - Lyric Video | Jailer 2", "Sun TV", type = "MUSIC_VIDEO_TYPE_UGC"),
                listOf(atv("Ala Bolelo (From \"Jailer 2\")", "Anirudh Ravichander", "Arunraja Kamaraj"), atv("Ala Bolelo", "Anirudh Ravichander", "Deepthi Suresh")),
            )
        assertEquals("Ala Bolelo (From \"Jailer 2\")", pick?.title)
    }

    @Test
    fun `a cover by someone else is never picked`() {
        // Attack: "Parvati (Replied Version)" by another singer shares the start of the title.
        assertNull(audioSongFor(omv("Parvati", "Sadhu Tiwari"), listOf(atv("Parvati (Replied Version)", "Aditya Kaushal"), atv("Maine Tere Hi Bharose", "Suneel Lodhi"))))
        assertNull(audioSongFor(omv("Ravana Tandav", "Vishal Dadlani"), listOf(atv("Ravana Tandav", "Desi Soldier"))))
    }

    @Test
    fun `videos in the search results are never picked`() {
        assertNull(audioSongFor(omv("Barsaat", "Banjaare"), listOf(omv("Barsaat", "Banjaare"))))
    }

    @Test
    fun `an entry that is already an audio song is kept as it is`() {
        val song = atv("Barsaat", "Banjaare", "Roni")
        assertSame(song, audioSongFor(song, emptyList()))
    }

    @Test
    fun `very short titles need an exact title`() {
        assertNull(audioSongFor(omv("Ek", "Someone"), listOf(atv("Ek Tarfa", "Someone"))))
    }

    // ---- integration: building a whole chart ----

    @Test
    fun `a chart keeps its order, drops entries without an audio song, and never shows a video`() =
        runBlocking {
            val chart = listOf(omv("Barsaat", "Banjaare"), omv("Unknown Track", "Nobody"), omv("Parvati", "Sadhu Tiwari"))
            val songs =
                audioChart(chart, parallelism = 2) { query ->
                    when {
                        query.startsWith("Barsaat") -> listOf(atv("Barsaat", "Banjaare", "Roni"))
                        query.startsWith("Parvati") -> listOf(atv("Parvati", "Sadhu Tiwari"))
                        else -> listOf(omv("Unknown Track", "Nobody"))
                    }
                }
            assertEquals(listOf("Barsaat", "Parvati"), songs.map { it.title })
            assertEquals(setOf("MUSIC_VIDEO_TYPE_ATV"), songs.map { it.musicVideoType }.toSet())
        }

    @Test
    fun `a failing search skips that entry only`() =
        runBlocking {
            val chart = listOf(omv("Barsaat", "Banjaare"), omv("Parvati", "Sadhu Tiwari"))
            val songs = audioChart(chart, parallelism = 2) { query -> if (query.startsWith("Barsaat")) error("offline") else listOf(atv("Parvati", "Sadhu Tiwari")) }
            assertEquals(listOf("Parvati"), songs.map { it.title })
        }

    @Test
    fun `no more than the allowed searches run at once and the same song appears once`() =
        runBlocking {
            val running = AtomicInteger()
            val peak = AtomicInteger()
            val chart = (1..30).map { omv("Barsaat", "Banjaare") }
            val songs =
                audioChart(chart, parallelism = 4) {
                    peak.accumulateAndGet(running.incrementAndGet(), ::maxOf)
                    kotlinx.coroutines.delay(10)
                    running.decrementAndGet()
                    listOf(atv("Barsaat", "Banjaare"))
                }
            assert(peak.get() <= 4) { "peak ${peak.get()}" }
            assertEquals(1, songs.size)
        }
}
