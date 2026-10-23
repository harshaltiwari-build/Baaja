/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.lyrics.lyricsTextLooksSynced
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.component.BaajaLyricsStyle
import com.metrolist.music.ui.component.LocalBaajaLyricsStyle
import com.metrolist.music.ui.component.LyricsShareCardRequests
import com.metrolist.music.ui.theme.BaajaArtwork
import com.metrolist.music.ui.theme.baajaInk
import com.metrolist.music.ui.theme.baajaSurface

/** Warm cream page colour from the lyrics mockup, derived from the theme so dark mode stays dark. */
@Composable
internal fun baajaLyricsBackground(): Color =
    lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.secondaryContainer, 0.26f)

/**
 * Full-screen lyrics page from the Lyrics mockup: header with the song, the lyrics with the sung
 * line on an outlined card, and a bottom control card (seek, source, play, share card).
 *
 * On blur/gradient player backgrounds the album background shows through instead of the cream page.
 */
@Composable
internal fun BaajaLyricsScreen(
    mediaMetadata: MediaMetadata,
    imageBackground: Boolean,
    colors: BaajaPlayerColors,
    positionProvider: () -> Long,
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    onClose: () -> Unit,
    onOpenLyricsMenu: () -> Unit,
    seekRow: @Composable () -> Unit,
    playButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val currentLyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)
    val lyricsForSong = currentLyrics?.takeIf { it.id == mediaMetadata.id }
    val isSynced = remember(lyricsForSong?.lyrics) { lyricsTextLooksSynced(lyricsForSong?.lyrics) }
    val provider = lyricsForSong?.provider?.takeIf { it.isNotBlank() && it != "Unknown" }

    val pageColor = baajaLyricsBackground()
    val ink = baajaInk
    val cardColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface
    val lyricsStyle =
        remember(imageBackground, ink, cardColor, textColor) {
            if (imageBackground) null else BaajaLyricsStyle(card = cardColor, ink = ink, text = textColor)
        }
    val headerColors = if (imageBackground) colors else colors.onCard()
    val cardColors = colors.onCard()

    val headerContent: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 10.dp, top = 8.dp, bottom = 6.dp),
        ) {
            BaajaRoundButton(
                icon = R.drawable.expand_more,
                contentDescription = stringResource(R.string.baaja_player_close_lyrics),
                onClick = onClose,
                colors = headerColors,
                outlined = false,
                size = 44.dp,
            )
            BaajaArtwork(
                model = mediaMetadata.thumbnailUrl,
                shape = RoundedCornerShape(12.dp),
                shadowOffset = 0.dp,
                modifier = Modifier.size(46.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = mediaMetadata.title,
                    color = headerColors.content,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = mediaMetadata.artists.joinToString { it.name },
                    color = headerColors.mutedContent,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (isSynced) {
                BaajaLabelChip(
                    text = stringResource(R.string.baaja_player_lyrics_synced),
                    colors = cardColors,
                    icon = R.drawable.check,
                    height = 32.dp,
                )
            }
            BaajaRoundButton(
                icon = R.drawable.fullscreen,
                contentDescription = stringResource(R.string.baaja_player_toggle_fullscreen),
                onClick = onToggleFullScreen,
                colors = headerColors,
                outlined = false,
                size = 44.dp,
            )
        }
    }

    val lyricsContent: @Composable (Modifier) -> Unit = { mod ->
        Box(modifier = mod, contentAlignment = Alignment.Center) {
            CompositionLocalProvider(LocalBaajaLyricsStyle provides lyricsStyle) {
                InlineLyricsView(
                    mediaMetadata = mediaMetadata,
                    showLyrics = true,
                    positionProvider = positionProvider,
                    horizontalPadding = 6.dp,
                )
            }
        }
    }

    val controlCard: @Composable (Modifier) -> Unit = { mod ->
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier =
                mod
                    .fillMaxWidth()
                    .baajaSurface(cardColors.ink, cardColors.surface, RoundedCornerShape(26.dp), shadowOffset = 4.dp, borderWidth = 2.5.dp)
                    .padding(16.dp),
        ) {
            seekRow()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    BaajaPlayerPill(
                        text =
                            provider?.let { stringResource(R.string.baaja_player_lyrics_source, it) }
                                ?: stringResource(R.string.baaja_player_lyrics_options),
                        onClick = onOpenLyricsMenu,
                        colors = cardColors,
                        height = 44.dp,
                        shadowOffset = 0.dp,
                    )
                }
                Box(Modifier.padding(horizontal = 8.dp)) { playButton() }
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    BaajaPlayerPill(
                        text = stringResource(R.string.baaja_player_share_card),
                        icon = R.drawable.share,
                        onClick = LyricsShareCardRequests::request,
                        colors = cardColors,
                        height = 44.dp,
                        shadowOffset = 0.dp,
                    )
                }
            }
        }
    }

    val pageModifier =
        modifier
            .fillMaxSize()
            .then(if (imageBackground) Modifier else Modifier.background(pageColor))

    run {
        Column(
            modifier = pageModifier.windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        ) {
            headerContent()
            lyricsContent(Modifier.weight(1f).fillMaxWidth())
            AnimatedVisibility(
                visible = !isFullScreen,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                controlCard(
                    Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
                )
            }
        }
    }
}
