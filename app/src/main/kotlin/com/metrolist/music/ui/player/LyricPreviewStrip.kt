/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.ShowLyricPreviewOnPlayerKey
import com.metrolist.music.db.entities.LyricsEntity
import com.metrolist.music.lyrics.LyricsEntry
import com.metrolist.music.lyrics.LyricsUtils
import com.metrolist.music.lyrics.lyricsTextLooksSynced
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.utils.rememberPreference
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Apple-Music-style lyric preview under the artwork: the synced line being sung and the next one
 * faded. Tapping it opens the full lyrics. Renders nothing when the song has no synced lyrics or
 * when [ShowLyricPreviewOnPlayerKey] is off.
 */
@Composable
internal fun LyricPreviewStrip(
    mediaMetadata: MediaMetadata?,
    positionProvider: () -> Long,
    textColor: Color,
    onOpenLyrics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showPreview by rememberPreference(ShowLyricPreviewOnPlayerKey, defaultValue = true)
    if (!showPreview) return

    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val database = LocalDatabase.current
    val currentLyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)
    val currentSong by playerConnection.currentSong.collectAsStateWithLifecycle(initialValue = null)

    // Fetch lyrics for the playing song if nobody has yet (the full lyrics view does the same).
    LaunchedEffect(mediaMetadata?.id, currentLyrics == null) {
        val metadata = mediaMetadata ?: return@LaunchedEffect
        if (currentLyrics != null) return@LaunchedEffect
        delay(800) // skip songs the user is flicking past
        withContext(Dispatchers.IO) {
            runCatching {
                if (database.lyrics(metadata.id).first() != null) return@runCatching
                val helper =
                    EntryPointAccessors
                        .fromApplication(context.applicationContext, com.metrolist.music.di.LyricsHelperEntryPoint::class.java)
                        .lyricsHelper()
                val fetched = helper.getLyrics(metadata)
                database.query { upsert(LyricsEntity(metadata.id, fetched.lyrics, fetched.provider)) }
            }
        }
    }

    val lyricsText =
        currentLyrics
            ?.takeIf { it.id == mediaMetadata?.id }
            ?.lyrics
            ?.takeIf { it != LyricsEntity.LYRICS_NOT_FOUND }

    val lines by produceState(initialValue = emptyList<LyricsEntry>(), lyricsText) {
        value =
            if (!lyricsTextLooksSynced(lyricsText)) {
                emptyList()
            } else {
                withContext(Dispatchers.Default) {
                    runCatching { LyricsUtils.parseLyrics(lyricsText!!) }
                        .getOrDefault(emptyList())
                        .filter { !it.isBackground && it.text.isNotBlank() }
                }
            }
    }

    val offset = currentSong?.song?.lyricsOffset ?: 0
    // Only recomposes when the line changes, not on every position tick.
    val lineIndex by remember(lines, offset) {
        derivedStateOf {
            if (lines.isEmpty()) -1 else LyricsUtils.findCurrentLineIndex(lines, positionProvider() + offset)
        }
    }

    AnimatedVisibility(
        visible = lines.isNotEmpty(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        val openLyrics = stringResource(R.string.baaja_player_open_lyrics)
        AnimatedContent(
            targetState = lineIndex,
            transitionSpec = {
                (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut())
            },
            label = "lyricPreview",
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(role = Role.Button, onClickLabel = openLyrics, onClick = onOpenLyrics)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
        ) { index ->
            val current = lines.getOrNull(index)?.text
            val next = lines.getOrNull(index + 1)?.text
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = current ?: "♪",
                    color = textColor,
                    fontSize = 19.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (next != null) {
                    Text(
                        text = next,
                        color = textColor.copy(alpha = 0.45f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
