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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier
                .size(104.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            repeat(2) { r ->
                Row(Modifier.weight(1f)) {
                    repeat(2) { c ->
                        val url = covers.getOrNull(r * 2 + c)
                        Box(Modifier.weight(1f).fillMaxWidth().height(52.dp).background(BaajaTileColors.sky())) {
                            if (url != null) {
                                AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                            }
                        }
                    }
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            BaajaPillButton(text = stringResource(R.string.baaja_home_play), onClick = onPlay, icon = R.drawable.play, height = 44.dp)
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
            .height(112.dp)
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
                Row(
                    Modifier.padding(start = BaajaHomeGutter, end = BaajaHomeGutter, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    pair.forEachIndexed { col, mix ->
                        BaajaArtistMixTile(
                            artist = mix.artist,
                            tint = tints[(rowIndex * 2 + col) % tints.size],
                            onClick = mix.onClick,
                            badge = mix.badge,
                            tagline = mix.tagline,
                            modifier = Modifier.weight(1f),
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
            mixesBlock()
            becausePicks.take(1).forEachIndexed { i, s -> shelf("because_pick_$i", s) }
        }
        TasteStage.HISTORY -> {
            historyShelf?.let { shelf("history", it) }
            hero()
            releasesShelf?.let { shelf("releases", it) }
            becauseHistory?.let { shelf("because_history", it) }
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
