/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.db.entities.FormatEntity
import com.metrolist.music.ui.theme.BaajaArtwork
import com.metrolist.music.ui.theme.BaajaCard
import com.metrolist.music.ui.theme.BaajaPillButton
import com.metrolist.music.ui.theme.baajaInk
import com.metrolist.music.ui.theme.baajaSurface
import com.metrolist.music.utils.SearchRoutes
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun BehindTheSongScreen(
    navController: NavController,
    viewModel: BehindTheSongViewModel = hiltViewModel(),
) {
    val info by viewModel.info.collectAsStateWithLifecycle()
    val notFound by viewModel.notFound.collectAsStateWithLifecycle()
    val playCount by viewModel.playCount.collectAsStateWithLifecycle()
    val format by viewModel.format.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    val insets = LocalPlayerAwareWindowInsets.current
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(insets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal).asPaddingValues())
            .padding(start = 20.dp, end = 20.dp, top = 12.dp)
            .padding(bottom = insets.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding() + 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                Modifier
                    .padding(end = 2.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = navController::navigateUp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.arrow_back), stringResource(R.string.back_button_desc), modifier = Modifier.size(26.dp))
            }
            Text(stringResource(R.string.baaja_bts_title), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        }

        val song = info
        if (song == null) {
            if (notFound) {
                Text(stringResource(R.string.baaja_bts_not_found), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
            } else {
                Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            return@Column
        }

        // Song header
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            BaajaArtwork(song.thumbnailUrl, Modifier.size(84.dp), shape = RoundedCornerShape(20.dp), shadowOffset = 3.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(song.title, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 28.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                val artistLine = listOfNotNull(song.artistNames.takeIf { it.isNotBlank() }, song.year?.toString()).joinToString(" · ")
                val firstArtistId = song.artists.firstOrNull { it.first != null }?.first
                Text(
                    artistLine,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        if (firstArtistId != null) {
                            Modifier.clickable(role = Role.Button) { navController.navigate("artist/$firstArtistId") }
                        } else {
                            Modifier
                        },
                )
            }
        }

        // Facts the app already has
        FactChips(
            buildList {
                song.albumName?.takeIf { it.isNotBlank() }?.let { add(stringResource(R.string.baaja_bts_album) to it) }
                song.year?.let { add(stringResource(R.string.baaja_bts_year) to it.toString()) }
                if (song.durationSeconds > 0) add(stringResource(R.string.baaja_bts_length) to formatClock(song.durationSeconds))
                add(stringResource(R.string.baaja_bts_plays) to playCount.toString())
                format?.let { add(stringResource(R.string.baaja_bts_audio) to describeFormat(it)) }
            },
        )

        ListeningSection(stats, song.totalPlayTimeMs)
    }
}

private data class ChipColors(val container: Color, val content: Color)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FactChips(facts: List<Pair<String, String>>) {
    val scheme = MaterialTheme.colorScheme
    val palette =
        listOf(
            ChipColors(scheme.primaryContainer, scheme.onPrimaryContainer),
            ChipColors(scheme.tertiaryContainer, scheme.onTertiaryContainer),
            ChipColors(scheme.secondaryContainer, scheme.onSecondaryContainer),
            ChipColors(scheme.secondary.copy(alpha = 0.2f).compositeOver(scheme.surface), scheme.onSurface),
        )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        facts.forEachIndexed { index, (label, value) ->
            val colors = palette[index % palette.size]
            Row(
                Modifier
                    .heightIn(min = 36.dp)
                    .clip(CircleShape)
                    .background(colors.container)
                    .border(2.dp, baajaInk, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(label, fontSize = 14.sp, color = colors.content.copy(alpha = 0.72f))
                Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.content, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ListeningSection(
    stats: ListeningStats?,
    totalPlayTimeMs: Long,
) {
    Column {
        SectionTitle(stringResource(R.string.baaja_bts_your_listening))
        val formatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()) }
        if (stats == null || stats.sessions == 0 || stats.firstPlayed == null) {
            Text(
                stringResource(R.string.baaja_bts_never_played),
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        } else {
            InfoRow(stringResource(R.string.baaja_bts_first_played), stats.firstPlayed.format(formatter))
            stats.lastPlayed?.let { InfoRow(stringResource(R.string.baaja_bts_last_played), it.format(formatter)) }
            if (totalPlayTimeMs > 0) InfoRow(stringResource(R.string.baaja_bts_listened), formatListened(totalPlayTimeMs))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
) {
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 52.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 12.dp))
        }
        HorizontalDivider(thickness = 1.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

private fun formatClock(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

private fun formatListened(ms: Long): String {
    val minutes = ms / 60_000
    val hours = minutes / 60
    return if (hours > 0) "$hours h ${minutes % 60} min" else "${minutes.coerceAtLeast(1)} min"
}

/** e.g. "opus · 160 kbps" from the stored stream format. */
private fun describeFormat(format: FormatEntity): String {
    val codec =
        format.codecs.substringBefore('.').ifBlank { format.mimeType.substringAfter('/').substringBefore(';') }
    val kbps = format.bitrate / 1000
    return if (kbps > 0) "$codec · $kbps kbps" else codec
}
