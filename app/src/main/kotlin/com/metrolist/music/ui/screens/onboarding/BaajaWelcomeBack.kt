/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.metrolist.music.LocalDatabase
import com.metrolist.music.R
import com.metrolist.music.constants.AccountNameKey
import com.metrolist.music.constants.BaajaAccountImageKey
import com.metrolist.music.ui.component.BaajaTileColors
import com.metrolist.music.ui.theme.BaajaPillButton
import com.metrolist.music.ui.theme.baajaInk
import com.metrolist.music.ui.theme.baajaOutline
import com.metrolist.music.ui.theme.baajaSurface
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map

/** Shown once after a Google sign-in: who is connected and what is being brought over. */
@Composable
fun BaajaWelcomeBack(
    onContinue: () -> Unit,
    onAddLanguages: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val database = LocalDatabase.current
    val (name) = rememberPreference(AccountNameKey, "")
    val (imageUrl) = rememberPreference(BaajaAccountImageKey, "")
    val liked by remember(database) { database.likedSongsCount() }.collectAsStateWithLifecycle(0)
    val playlists by remember(database) { database.playlistsByCreateDateAsc().map { it.size } }.collectAsStateWithLifecycle(0)
    val subscriptions by remember(database) { database.artistsBookmarkedByCreateDateAsc().map { it.size } }.collectAsStateWithLifecycle(0)
    // Syncing runs in the background; after a few seconds an empty row simply means there was nothing to bring over.
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(7_000)
        settled = true
    }
    val ink = baajaInk
    val first = name.trim().substringBefore(' ').ifEmpty { stringResource(R.string.baaja_welcome_back_fallback_name) }

    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 36.dp, bottom = 150.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.size(116.dp)) {
                Box(
                    Modifier
                        .size(112.dp)
                        .baajaOutline(ink, CircleShape, borderWidth = 3.dp, shadowOffset = 4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFE066)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(first.take(1).uppercase(), fontSize = 46.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF17151F))
                    if (imageUrl.isNotBlank()) {
                        AsyncImage(imageUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                }
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.5.dp, ink, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.foundation.Image(painterResource(R.drawable.google_g), null, Modifier.size(20.dp))
                }
            }
            Text(
                stringResource(R.string.baaja_welcome_back_title, first),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.baaja_welcome_back_sub),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            SyncRow(R.drawable.favorite, BaajaTileColors.liked(), stringResource(R.string.baaja_welcome_back_liked), pluralStringResource(R.plurals.n_song, liked, liked), liked > 0 || settled)
            SyncRow(R.drawable.library_music, BaajaTileColors.lilac(), stringResource(R.string.baaja_welcome_back_playlists), playlists.toString(), playlists > 0 || settled)
            SyncRow(R.drawable.artist, BaajaTileColors.mint(), stringResource(R.string.baaja_welcome_back_subs), subscriptions.toString(), subscriptions > 0 || settled)
            Text(
                stringResource(R.string.baaja_welcome_back_note),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .baajaSurface(ink, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp), shadowOffset = 0.dp)
                        .padding(14.dp),
            )
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BaajaPillButton(
                text = stringResource(R.string.baaja_welcome_back_go),
                onClick = onContinue,
                height = 60.dp,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = onAddLanguages) {
                Text(stringResource(R.string.baaja_welcome_back_languages), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SyncRow(
    icon: Int,
    tint: Color,
    label: String,
    detail: String,
    done: Boolean,
) {
    val ink = baajaInk
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .baajaSurface(ink, MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp), shadowOffset = 0.dp, borderWidth = 2.5.dp)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(tint).border(2.dp, ink, CircleShape), contentAlignment = Alignment.Center) {
            androidx.compose.material3.Icon(painterResource(icon), null, Modifier.size(20.dp), tint = ink)
        }
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Text(detail, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (done) {
            Text("✓  " + stringResource(R.string.baaja_welcome_back_synced), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        } else {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.5.dp)
            Text(stringResource(R.string.baaja_welcome_back_syncing), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}
