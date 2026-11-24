package com.metrolist.music.recommend

import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.Song
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.random.Random

/**
 * Mixes built only from the listener's own history, refreshed once a day:
 * On Repeat, Rediscover, a mix for this time of day, and up to three top-artist mixes
 * (the artist's most played songs interleaved with songs related to them).
 */
object DailyMixes {
    enum class Kind { ON_REPEAT, REDISCOVER, TIME_OF_DAY, ARTIST }

    enum class DayPart(
        val startHour: Int,
        val endHour: Int,
    ) {
        MORNING(5, 11),
        AFTERNOON(12, 16),
        EVENING(17, 21),
        NIGHT(22, 23),
        ;

        companion object {
            fun of(hour: Int): DayPart =
                entries.firstOrNull { hour in it.startHour..it.endHour } ?: NIGHT
        }
    }

    data class Mix(
        val key: String,
        val kind: Kind,
        val songs: List<Song>,
        val artistName: String? = null,
        val dayPart: DayPart? = null,
    ) {
        val thumbnail: String? get() = songs.firstOrNull()?.song?.thumbnailUrl
    }

    private const val MIN_SONGS = 8
    private const val MIX_SIZE = 30

    @Volatile
    private var cached: Pair<String, List<Mix>>? = null

    /** Call off the main thread. Results are kept for the rest of the day and day part. */
    suspend fun build(database: MusicDatabase): List<Mix> {
        val now = LocalDateTime.now()
        val today = LocalDate.now()
        val dayPart = DayPart.of(now.hour)
        val cacheKey = "$today-$dayPart"
        cached?.takeIf { it.first == cacheKey }?.let { return it.second }

        val random = Random(today.toEpochDay())
        val mixes = mutableListOf<Mix>()

        val onRepeat = database.mostPlayedSongs(now.minusDays(30), limit = MIX_SIZE).first()
        if (onRepeat.size >= MIN_SONGS) mixes += Mix("on_repeat", Kind.ON_REPEAT, dayShuffle(onRepeat, random))

        val wallClockNow = now.atZone(ZoneOffset.UTC).toInstant().toEpochMilli()
        val since = now.minusDays(60).atZone(ZoneOffset.UTC).toInstant().toEpochMilli()
        val partSongs = database.mostPlayedSongsInHours(since, dayPart.startHour, dayPart.endHour, MIX_SIZE)
        if (partSongs.size >= MIN_SONGS) {
            mixes += Mix("time_${dayPart.name}", Kind.TIME_OF_DAY, partSongs.shuffled(random), dayPart = dayPart)
        }

        val artists = database.mostPlayedArtists(now.minusDays(60), limit = 3).first()
        for (artist in artists) {
            val own =
                database
                    .mostPlayedSongsByArtist(artist.id, now.minusDays(365), now)
                    .first()
                    .take(12)
            if (own.size < 3) continue
            val related =
                own
                    .take(6)
                    .flatMap { database.relatedSongs(it.id) }
                    .filter { song -> song.artists.none { it.id == artist.id } }
                    .distinctBy { it.id }
                    .shuffled(random)
                    .take(MIX_SIZE - own.size)
            val songs = interleave(own.shuffled(random), related).distinctBy { it.id }
            if (songs.size >= MIN_SONGS) {
                mixes += Mix("artist_${artist.id}", Kind.ARTIST, songs, artistName = artist.artist.name)
            }
        }

        val rediscover = database.forgottenFavorites(wallClockNow).first()
        if (rediscover.size >= MIN_SONGS) {
            mixes += Mix("rediscover", Kind.REDISCOVER, rediscover.shuffled(random).take(MIX_SIZE))
        }

        cached = cacheKey to mixes
        return mixes
    }

    /** Shuffles within thirds so the most played songs still lead, but the order changes each day. */
    private fun dayShuffle(
        songs: List<Song>,
        random: Random,
    ): List<Song> {
        val third = (songs.size + 2) / 3
        return songs.chunked(third).flatMap { it.shuffled(random) }
    }

    private fun interleave(
        a: List<Song>,
        b: List<Song>,
    ): List<Song> {
        val result = ArrayList<Song>(a.size + b.size)
        val max = maxOf(a.size, b.size)
        for (i in 0 until max) {
            a.getOrNull(i)?.let(result::add)
            b.getOrNull(i)?.let(result::add)
        }
        return result
    }
}
