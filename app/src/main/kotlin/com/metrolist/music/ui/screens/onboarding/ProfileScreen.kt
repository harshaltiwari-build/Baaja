/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.innertube.YouTube
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.UseLoginForBrowse
import com.metrolist.music.constants.YtmSyncKey
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.theme.BaajaArtwork
import com.metrolist.music.ui.theme.BaajaCard
import com.metrolist.music.ui.theme.BaajaPillButton
import com.metrolist.music.ui.theme.BaajaInk
import com.metrolist.music.ui.theme.baajaInk
import com.metrolist.music.ui.theme.baajaSurface
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.utils.reportException
import com.metrolist.music.viewmodels.AccountSettingsViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDateTime

private val AllTimeStart: LocalDateTime = LocalDateTime.of(2000, 1, 1, 0, 0)

/** Profile hub: who you are, a listening summary and the ways into stats, history, settings and your account. */
@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val scope = rememberCoroutineScope()
    val account = rememberBaajaAccount()
    val profile = rememberBaajaProfile()
    var showEdit by rememberSaveable { mutableStateOf(false) }
    val accountSettingsViewModel: AccountSettingsViewModel = hiltViewModel()
    val (_, onInnerTubeCookieChange) = rememberPreference(InnerTubeCookieKey, "")
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
    var showTokenEditor by rememberSaveable { mutableStateOf(false) }
    val (useLoginForBrowse, onUseLoginForBrowseChange) = rememberPreference(UseLoginForBrowse, true)
    val (ytmSync, onYtmSyncChange) = rememberPreference(YtmSyncKey, true)

    val now = remember { LocalDateTime.now() }
    val totalPlayTime by remember(database) { database.getTotalPlayTimeInRange(AllTimeStart, now) }
        .collectAsStateWithLifecycle(null)
    val plays by remember(database) { database.eventCount() }.collectAsStateWithLifecycle(0)
    val artistCount by remember(database) { database.getUniqueArtistCountInRange(AllTimeStart, now) }
        .collectAsStateWithLifecycle(0)
    val topArtists by remember(database) {
        database.mostPlayedArtists(AllTimeStart, limit = 12).map { artists ->
            artists.filter { it.artist.isYouTubeArtist }.take(4)
        }
    }.collectAsStateWithLifecycle(emptyList())
    val firstEvent by remember(database) { database.firstEvent() }.collectAsStateWithLifecycle(null)
    // Top artists from history may have no saved photo; fetch it from their artist page.
    var artistPhotos by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    LaunchedEffect(topArtists.map { it.id }) {
        topArtists.filter { it.artist.thumbnailUrl.isNullOrBlank() }.forEach { artist ->
            com.metrolist.innertube.YouTube.artist(artist.id).getOrNull()?.artist?.thumbnail?.let { photo ->
                artistPhotos = artistPhotos + (artist.id to photo)
            }
        }
    }

    val numberFormat = remember { NumberFormat.getIntegerInstance() }
    val displayName = profile.name
    val subtitle =
        buildList {
            if (profile.bio.isNotBlank()) {
                add(profile.bio)
            } else if (account.isLoggedIn) {
                (account.channelHandle.ifBlank { account.email }).takeIf { it.isNotBlank() }?.let(::add)
            } else {
                add(stringResource(R.string.baaja_onb_profile_guest_subtitle))
            }
            firstEvent?.event?.timestamp?.year?.let { add(stringResource(R.string.baaja_onb_profile_since, it)) }
        }.joinToString(" · ")

    if (showTokenEditor) AdvancedLoginDialog(onDismiss = { showTokenEditor = false })

    if (showEdit) EditProfileSheet(profile = profile, onDismiss = { showEdit = false })

    if (showLogoutDialog) {
        DefaultDialog(
            onDismiss = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.logout_dialog_title)) },
            content = {
                Text(
                    text = stringResource(R.string.logout_dialog_message),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                accountSettingsViewModel.logoutAndClearLibraryData(context)
                            } catch (e: Exception) {
                                reportException(e)
                            }
                            onInnerTubeCookieChange("")
                            showLogoutDialog = false
                        }
                    },
                ) { Text(stringResource(R.string.logout_clear)) }
                TextButton(
                    onClick = {
                        scope.launch {
                            accountSettingsViewModel.logoutKeepData(context, onInnerTubeCookieChange)
                            showLogoutDialog = false
                        }
                    },
                ) { Text(stringResource(R.string.logout_keep)) }
            },
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            ).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // The shared insets also reserve room for an app bar this screen doesn't have, so only clear the status bar.
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button) { navController.navigateUp() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = stringResource(R.string.back_button_desc),
                    modifier = Modifier.size(26.dp),
                )
            }
            Text(
                stringResource(R.string.baaja_onb_profile_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(start = 6.dp).semantics { heading() },
            )
        }

        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BaajaAvatar(name = displayName, imageUrl = profile.avatarModel, size = 110.dp)
            Text(
                displayName,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        BaajaPillButton(
            text = stringResource(R.string.baaja_onb_edit_profile),
            onClick = { showEdit = true },
            primary = false,
            icon = R.drawable.edit,
            height = 48.dp,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val minutes = (totalPlayTime ?: 0L) / 60_000L
            ProfileStatTile(numberFormat.format(minutes), stringResource(R.string.baaja_onb_profile_minutes), baajaPastel(0), Modifier.weight(1f))
            ProfileStatTile(numberFormat.format(plays), stringResource(R.string.baaja_onb_profile_plays), baajaPastel(1), Modifier.weight(1f))
            ProfileStatTile(numberFormat.format(artistCount), stringResource(R.string.baaja_onb_profile_artists), baajaPastel(5), Modifier.weight(1f))
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileLinkCard(stringResource(R.string.baaja_onb_profile_listening_stats), R.drawable.stats, baajaPastel(2), Modifier.weight(1f)) {
                    navController.navigate("stats")
                }
                ProfileLinkCard(stringResource(R.string.history), R.drawable.history, baajaPastel(1), Modifier.weight(1f)) {
                    navController.navigate("history")
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.baaja_onb_profile_top_artists),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            if (topArtists.isEmpty()) {
                Text(
                    stringResource(R.string.baaja_onb_profile_no_artists),
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    topArtists.forEach { artist ->
                        Column(
                            Modifier
                                .width(78.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(role = Role.Button) { navController.navigate("artist/${artist.id}") }
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            BaajaArtwork(
                                model = artist.artist.thumbnailUrl ?: artistPhotos[artist.id],
                                shape = CircleShape,
                                shadowOffset = 0.dp,
                                modifier = Modifier.size(66.dp),
                            )
                            Text(
                                artist.artist.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.baaja_onb_profile_account),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            if (account.isLoggedIn) {
                BaajaCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        ProfileAccountRow(R.drawable.library_music, stringResource(R.string.baaja_onb_profile_youtube_library), baajaPastel(4)) {
                            navController.navigate("account")
                        }
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 1.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        ProfileAccountRow(R.drawable.account, stringResource(R.string.switch_youtube_channel), baajaPastel(6)) {
                            navController.navigate("switch_channel")
                        }
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 1.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 1.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        ProfileSwitchRow(R.drawable.cached, stringResource(R.string.yt_sync), baajaPastel(1), ytmSync, onCheckedChange = onYtmSyncChange)
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 1.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        ProfileSwitchRow(R.drawable.cached, stringResource(R.string.more_content), baajaPastel(2), useLoginForBrowse) {
                            YouTube.useLoginForBrowse = it
                            onUseLoginForBrowseChange(it)
                        }
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 1.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        ProfileAccountRow(R.drawable.token, stringResource(R.string.advanced_login), baajaPastel(3)) {
                            showTokenEditor = true
                        }
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 1.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        ProfileAccountRow(R.drawable.logout, stringResource(R.string.action_logout), baajaPastel(5)) {
                            showLogoutDialog = true
                        }
                    }
                }
            } else {
                BaajaCard(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    onClick = { navController.navigate("login") },
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BaajaIconBadge(R.drawable.login, Color.White, size = 46.dp)
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.baaja_onb_profile_sign_in_title),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Text(
                                stringResource(R.string.baaja_onb_profile_sign_in_body),
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                        Icon(
                            painterResource(R.drawable.navigate_next),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
                BaajaCard(modifier = Modifier.fillMaxWidth()) {
                    ProfileAccountRow(R.drawable.token, stringResource(R.string.advanced_login), baajaPastel(3)) {
                        showTokenEditor = true
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ProfileStatTile(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .baajaSurface(baajaInk, color, RoundedCornerShape(18.dp), shadowOffset = 3.dp)
            .padding(horizontal = 6.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = BaajaInk, maxLines = 1)
        Text(label, fontSize = 14.sp, color = BaajaInk, maxLines = 1)
    }
}

@Composable
private fun ProfileLinkCard(
    label: String,
    @DrawableRes icon: Int,
    badge: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    BaajaCard(modifier = modifier.heightIn(min = 104.dp), shape = RoundedCornerShape(22.dp), onClick = onClick) {
        Column(
            Modifier.fillMaxWidth().heightIn(min = 104.dp).padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            BaajaIconBadge(icon, badge, size = 36.dp)
            Spacer(Modifier.height(10.dp))
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 19.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ProfileAccountRow(
    @DrawableRes icon: Int,
    label: String,
    badge: Color,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BaajaIconBadge(icon, badge)
        Text(label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Icon(painterResource(R.drawable.navigate_next), contentDescription = null, modifier = Modifier.size(22.dp))
    }
}
