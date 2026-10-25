/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.items
import coil3.compose.AsyncImage
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.R
import com.metrolist.music.ui.component.BaajaTileColors
import com.metrolist.music.ui.screens.onboarding.BaajaArtist
import com.metrolist.music.ui.theme.BaajaPillButton
import com.metrolist.music.ui.theme.baajaInk
import com.metrolist.music.ui.theme.baajaSurface

/** "Made for you" lead card: a cover collage of the user's artists and one Play button. */
@Composable
fun BaajaMixHero(
    artists: List<BaajaArtist>,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = baajaInk
    val shape = RoundedCornerShape(26.dp)
    val covers = artists.mapNotNull { it.thumbnail }.take(4)
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = BaajaHomeGutter)
            .baajaSurface(ink, BaajaTileColors.lilac(), shape, shadowOffset = 3.dp, borderWidth = 2.5.dp)
            .clip(shape)
            .clickable(role = Role.Button, onClick = onPlay)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            repeat(2) { r ->
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    repeat(2) { c ->
                        val url = covers.getOrNull(r * 2 + c)
                        Box(Modifier.weight(1f).fillMaxHeight().background(BaajaTileColors.sky())) {
                            if (url != null) {
                                AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                            }
                        }
                    }
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.baaja_home_made_for_you).uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.baaja_home_your_mix),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
            )
            Text(
                artists.take(3).joinToString { it.name },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BaajaPillButton(
                text = stringResource(R.string.baaja_home_play),
                onClick = onPlay,
                icon = R.drawable.play,
                height = 44.dp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/** One artist's mix: a pastel tile with the artist's photo and "<Name> Mix". */
@Composable
fun BaajaArtistMixTile(
    artist: BaajaArtist,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    tagline: String = stringResource(R.string.baaja_home_mix_tagline),
) {
    val ink = baajaInk
    Column(
        modifier
            .heightIn(min = 112.dp)
            .baajaSurface(ink, tint, RoundedCornerShape(22.dp), shadowOffset = 3.dp, borderWidth = 2.5.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(baajaPastelFor(artist.name)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    artist.name.trim().take(1).uppercase(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = androidx.compose.ui.graphics.Color(0xFF17151F),
                )
                if (artist.thumbnail != null) {
                    AsyncImage(artist.thumbnail, null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                }
            }
            if (badge != null) BaajaNewBadge(badge)
        }
        Column {
            Text(
                stringResource(R.string.baaja_home_mix_name, artist.name),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                tagline,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

/** A simple song row for the "Because you like …" lists. */
@Composable
fun BaajaTasteSongRow(
    song: SongItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = baajaInk
    Row(
        modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = BaajaHomeGutter, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = song.thumbnail,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .size(52.dp)
                    .baajaSurface(ink, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp), shadowOffset = 0.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(song.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                song.artists.joinToString { it.name },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Square cover card for the horizontally scrolling "Because you like …" rows. */
@Composable
fun BaajaTasteSongCard(
    song: SongItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = baajaInk
    Column(
        modifier
            .width(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AsyncImage(
            model = song.thumbnail,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .size(140.dp)
                    .baajaSurface(ink, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp), shadowOffset = 3.dp, borderWidth = 2.5.dp),
        )
        Text(song.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(
            song.artists.joinToString { it.name },
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun BaajaNewBadge(text: String) {
    Text(
        text,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        color = androidx.compose.ui.graphics.Color(0xFF17151F),
        modifier =
            Modifier
                .baajaSurface(baajaInk, androidx.compose.ui.graphics.Color(0xFFFFE066), CircleShape, shadowOffset = 0.dp)
                .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/** One cover card of a taste shelf: a song or an album. */
data class TasteCard(
    val key: String,
    val title: String,
    val subtitle: String,
    val thumbnail: String?,
    val badge: String? = null,
)

@Composable
fun BaajaCoverCard(
    card: TasteCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = baajaInk
    Column(
        modifier
            .width(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box {
            AsyncImage(
                model = card.thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(140.dp)
                        .baajaSurface(ink, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp), shadowOffset = 3.dp, borderWidth = 2.5.dp),
            )
            if (card.badge != null) {
                Box(Modifier.padding(8.dp)) { BaajaNewBadge(card.badge) }
            }
        }
        Text(card.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(
            card.subtitle,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** What Home has learned so far: picks only, picks plus a little history, or history in charge. */
enum class TasteStage { PICKS, MIXED, HISTORY }

class TasteShelf(
    val title: String,
    val label: String?,
    val cards: List<TasteCard>,
    val onClick: (Int) -> Unit,
)

class TasteMix(
    val artist: BaajaArtist,
    val badge: String?,
    val tagline: String,
    val onClick: () -> Unit,
)

/** Emits the personalised block in the order of the approved designs for each [TasteStage]. */
fun androidx.compose.foundation.lazy.LazyListScope.tasteHome(
    stage: TasteStage,
    heroArtists: List<BaajaArtist>,
    onPlayMix: () -> Unit,
    mixes: List<TasteMix>,
    mixesLabel: String?,
    historyShelf: TasteShelf?,
    releasesShelf: TasteShelf?,
    becauseHistory: TasteShelf?,
    becausePicks: List<TasteShelf>,
    discoverShelf: TasteShelf?,
    tints: List<Color>,
    extraShelves: List<TasteShelf> = emptyList(),
) {
    fun shelf(key: String, shelf: TasteShelf) {
        item(key = "${key}_title") { BaajaHomeSectionTitle(title = shelf.title, label = shelf.label) }
        item(key = "${key}_row") {
            androidx.compose.foundation.lazy.LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = BaajaHomeGutter, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(shelf.cards, key = { _, c -> c.key }) { index, card ->
                    BaajaCoverCard(card, onClick = { shelf.onClick(index) })
                }
            }
        }
    }

    fun hero() {
        item(key = "taste_hero") { BaajaMixHero(artists = heroArtists, onPlay = onPlayMix) }
    }

    fun mixesBlock() {
        if (mixes.isEmpty()) return
        item(key = "taste_mixes_title") {
            BaajaHomeSectionTitle(title = androidx.compose.ui.res.stringResource(R.string.baaja_home_artist_mixes), label = mixesLabel)
        }
        mixes.take(4).chunked(2).forEachIndexed { rowIndex, pair ->
            item(key = "taste_mix_row_$rowIndex") {
                // Equal heights so a tile with a "New" badge or a longer name doesn't sit taller than its neighbour.
                Row(
                    Modifier
                        .padding(start = BaajaHomeGutter, end = BaajaHomeGutter, bottom = 12.dp)
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    pair.forEachIndexed { col, mix ->
                        BaajaArtistMixTile(
                            artist = mix.artist,
                            tint = tints[(rowIndex * 2 + col) % tints.size],
                            onClick = mix.onClick,
                            badge = mix.badge,
                            tagline = mix.tagline,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                    if (pair.size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                }
            }
        }
    }

    when (stage) {
        TasteStage.PICKS -> {
            hero()
            mixesBlock()
            becausePicks.take(2).forEachIndexed { i, s -> shelf("because_pick_$i", s) }
        }
        TasteStage.MIXED -> {
            historyShelf?.let { shelf("history", it) }
            hero()
            becauseHistory?.let { shelf("because_history", it) }
            extraShelves.forEachIndexed { i, e -> shelf("extra_$i", e) }
            mixesBlock()
            becausePicks.take(1).forEachIndexed { i, s -> shelf("because_pick_$i", s) }
        }
        TasteStage.HISTORY -> {
            historyShelf?.let { shelf("history", it) }
            hero()
            releasesShelf?.let { shelf("releases", it) }
            becauseHistory?.let { shelf("because_history", it) }
            extraShelves.forEachIndexed { i, e -> shelf("extra_$i", e) }
            mixesBlock()
            discoverShelf?.let { shelf("discover", it) }
        }
    }
}

/** A community playlist as a compact card: a 2x2 cover collage, the title and who made it. */
@Composable
fun BaajaCommunityCard(
    title: String,
    author: String,
    covers: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = baajaInk
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .width(156.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Column(
            Modifier
                .size(156.dp)
                .baajaSurface(ink, MaterialTheme.colorScheme.surfaceVariant, shape, shadowOffset = 3.dp, borderWidth = 2.5.dp),
        ) {
            repeat(2) { r ->
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    repeat(2) { c ->
                        val url = covers.getOrNull(r * 2 + c)
                        Box(Modifier.weight(1f).fillMaxSize().background(BaajaTileColors.lilac())) {
                            if (url != null) {
                                AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                            }
                        }
                    }
                }
            }
        }
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (author.isNotBlank()) {
            Text(
                author,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * The artists this listener cares about most, shared by Home, Explore and Library. Signed in, it is the
 * account's own YouTube Music history, followed artists and liked songs; picks and Baaja plays fill the gaps.
 */
@Composable
fun rememberTasteArtists(limit: Int = 10): List<BaajaArtist> {
    val database = com.metrolist.music.LocalDatabase.current
    val (cookie) = com.metrolist.music.utils.rememberPreference(com.metrolist.music.constants.InnerTubeCookieKey, "")
    val (pickedRaw) = com.metrolist.music.utils.rememberPreference(com.metrolist.music.constants.BaajaPickedArtistsKey, "")
    val isLoggedIn = androidx.compose.runtime.remember(cookie) { "SAPISID" in com.metrolist.innertube.utils.parseCookieString(cookie) }
    val liked by androidx.compose.runtime.remember(database) { database.likedSongsByCreateDateAsc() }
        .collectAsStateWithLifecycle(emptyList())
    val bookmarked by androidx.compose.runtime.remember(database) { database.artistsBookmarkedByCreateDateAsc() }
        .collectAsStateWithLifecycle(emptyList())
    val played by androidx.compose.runtime.remember(database) {
        database.mostPlayedArtists(java.time.LocalDateTime.now().minusWeeks(8), limit = 8)
    }.collectAsStateWithLifecycle(emptyList())
    var history by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<List<SongItem>>(emptyList()) }
    androidx.compose.runtime.LaunchedEffect(isLoggedIn) {
        history =
            if (isLoggedIn) {
                com.metrolist.innertube.YouTube.musicHistory().getOrNull()?.sections.orEmpty().flatMap { it.songs }.take(60)
            } else {
                emptyList()
            }
    }
    // Artists found through history have no photo yet; fetch it from the artist page once and remember it.
    var photos by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Map<String, String>>(emptyMap()) }
    val ranked = androidx.compose.runtime.remember(isLoggedIn, liked, bookmarked, played, history, pickedRaw) {
        val scores = LinkedHashMap<String, Pair<BaajaArtist, Int>>()

        fun add(id: String?, name: String, thumb: String?, weight: Int) {
            if (id.isNullOrBlank() || name.isBlank()) return
            val current = scores[id]
            scores[id] = (current?.first?.let { if (it.thumbnail == null && thumb != null) it.copy(thumbnail = thumb) else it }
                ?: BaajaArtist(id, name, thumb)) to ((current?.second ?: 0) + weight)
        }
        if (isLoggedIn) {
            history.forEach { song -> song.artists.forEach { add(it.id, it.name, null, 2) } }
            bookmarked.forEach { add(it.id, it.title, it.thumbnailUrl, 5) }
            liked.forEachIndexed { index, song ->
                // A like is an explicit signal, and a recent one says more about current taste.
                song.artists.forEach { add(it.id, it.name, it.thumbnailUrl, if (index >= liked.size - 30) 4 else 2) }
            }
        }
        played.forEach { add(it.id, it.title, it.thumbnailUrl, 3) }
        com.metrolist.music.ui.screens.onboarding.decodeArtists(pickedRaw).forEach { add(it.id, it.name, it.thumbnail, 2) }
        scores.values.sortedByDescending { it.second }.map { it.first }.take(limit)
    }
    androidx.compose.runtime.LaunchedEffect(ranked.map { it.id }) {
        ranked.filter { it.thumbnail.isNullOrBlank() && it.id !in photos }.forEach { artist ->
            com.metrolist.innertube.YouTube.artist(artist.id).getOrNull()?.artist?.thumbnail?.let { photo ->
                photos = photos + (artist.id to photo)
            }
        }
    }
    return androidx.compose.runtime.remember(ranked, photos) {
        ranked.map { if (it.thumbnail.isNullOrBlank()) it.copy(thumbnail = photos[it.id]) else it }
    }
}

/** A scrolling row of the listener's top artists; tapping one opens that artist. */
@Composable
fun BaajaYourArtistsRow(
    artists: List<BaajaArtist>,
    onArtist: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (artists.isEmpty()) return
    Column(modifier) {
        BaajaHomeSectionTitle(title = stringResource(R.string.baaja_home_your_artists))
        androidx.compose.foundation.lazy.LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = BaajaHomeGutter - 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(artists.size, key = { "taste_artist_${artists[it].id}" }) { index ->
                val artist = artists[index]
                BaajaArtistAvatar(
                    name = artist.name,
                    thumbnailUrl = artist.thumbnail,
                    onClick = { onArtist(artist.id) },
                )
            }
        }
    }
}
