/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.features

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.ui.theme.BaajaCard
import com.metrolist.music.ui.theme.BaajaChip
import com.metrolist.music.ui.theme.BaajaPillButton
import com.metrolist.music.ui.theme.baajaInk
import com.metrolist.music.ui.theme.baajaSurface
import kotlinx.coroutines.launch

private const val SPOTIFY_DASHBOARD_URL = "https://developer.spotify.com/dashboard"

@Composable
fun SpotifyImportScreen(
    navController: NavController,
    viewModel: SpotifyImportViewModel = hiltViewModel(),
) {
    val phase = viewModel.phase
    val defaultName = stringResource(R.string.baaja_imp_default_name)
    val insets = LocalPlayerAwareWindowInsets.current

    val csvPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) viewModel.readCsv(uri, defaultName)
        }

    val busy = phase is ImportPhase.Matching || phase is ImportPhase.Reading
    BackHandler(enabled = phase !is ImportPhase.Input) {
        if (busy) viewModel.stopMatching() else viewModel.startOver()
    }

    val showReview = phase is ImportPhase.Matching || phase is ImportPhase.Review || phase is ImportPhase.Saving

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding =
                insets
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                    .asPaddingValues()
                    .let { PaddingValues(bottom = it.calculateBottomPadding() + if (showReview) 96.dp else 24.dp) },
            modifier =
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        ) {
            item(key = "header") {
                FeatureHeader(
                    title = stringResource(R.string.baaja_imp_spotify_title),
                    subtitle = stringResource(R.string.baaja_imp_spotify_subtitle),
                    onBack = { navController.navigateUp() },
                )
            }

            when (phase) {
                ImportPhase.Input -> {
                    item(key = "modes") {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            BaajaChip(
                                label = stringResource(R.string.baaja_imp_mode_link),
                                selected = viewModel.mode == ImportMode.LINK,
                                onClick = {
                                    viewModel.mode = ImportMode.LINK
                                    viewModel.dismissError()
                                },
                                icon = R.drawable.link,
                            )
                            BaajaChip(
                                label = stringResource(R.string.baaja_imp_mode_csv),
                                selected = viewModel.mode == ImportMode.CSV,
                                onClick = {
                                    viewModel.mode = ImportMode.CSV
                                    viewModel.dismissError()
                                },
                                icon = R.drawable.list,
                            )
                        }
                    }
                    viewModel.error?.let { err ->
                        item(key = "error") { ErrorCard(err, viewModel.serverCode, viewModel.serverDetail) }
                    }
                    if (viewModel.mode == ImportMode.LINK) {
                        item(key = "link") { LinkCard(viewModel, defaultName) }
                        item(key = "keys") { KeysCard(viewModel) }
                    } else {
                        item(key = "csv") {
                            CsvCard(
                                onPick = {
                                    csvPicker.launch(
                                        arrayOf("text/csv", "text/comma-separated-values", "text/plain", "application/csv", "application/vnd.ms-excel", "text/*"),
                                    )
                                },
                            )
                        }
                    }
                }

                is ImportPhase.Reading -> {
                    item(key = "reading") {
                        BaajaCard(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    stringResource(R.string.baaja_imp_reading),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                if (phase.total > 0) {
                                    Text(
                                        stringResource(R.string.baaja_imp_reading_count, phase.loaded, phase.total),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                    FeatureProgressBar(phase.loaded.toFloat() / phase.total)
                                } else {
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surface,
                                    )
                                }
                            }
                        }
                    }
                }

                ImportPhase.Matching, ImportPhase.Review, ImportPhase.Saving -> {
                    item(key = "progress") { MatchProgressCard(viewModel, phase) }
                    if (phase != ImportPhase.Matching) {
                        item(key = "review") { ReviewControls(viewModel) }
                    }
                    items(viewModel.rows, key = { "row_${it.index}" }, contentType = { "import_row" }) { row ->
                        ImportRowItem(
                            row = row,
                            enabled = phase == ImportPhase.Review,
                            onToggle = { viewModel.toggle(row.index) },
                        )
                    }
                }

                is ImportPhase.Done -> {
                    item(key = "done") {
                        DoneCard(
                            phase = phase,
                            onOpen = { navController.navigate("local_playlist/${phase.playlistId}") },
                            onAgain = viewModel::startOver,
                        )
                    }
                }
            }
        }

        if (showReview) {
            val count = viewModel.selectedCount
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(insets.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                BaajaPillButton(
                    text = stringResource(R.string.baaja_imp_save, count),
                    onClick = { viewModel.save(defaultName) },
                    enabled = phase == ImportPhase.Review && count > 0,
                    icon = R.drawable.playlist_add,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun LinkCard(viewModel: SpotifyImportViewModel, defaultName: String) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    BaajaCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.baaja_imp_link_label), fontSize = 17.sp, fontWeight = FontWeight.Bold)
            FeatureTextField(
                value = viewModel.link,
                onValueChange = { viewModel.link = it },
                placeholder = stringResource(R.string.baaja_imp_link_hint),
                leadingIcon = R.drawable.link,
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Go,
                onImeAction = { viewModel.readSpotifyPlaylist(defaultName) },
                trailing = {
                    Text(
                        stringResource(R.string.baaja_imp_paste),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier =
                            Modifier
                                .clip(CircleShape)
                                .clickable(role = Role.Button) {
                                    scope.launch {
                                        val clip = clipboard.getClipEntry()?.clipData
                                        val text = if (clip != null && clip.itemCount > 0) clip.getItemAt(0).coerceToText(context)?.toString() else null
                                        if (!text.isNullOrBlank()) viewModel.link = text.trim()
                                    }
                                }.padding(horizontal = 14.dp, vertical = 12.dp),
                    )
                },
            )
            BaajaPillButton(
                text = stringResource(R.string.baaja_imp_read_playlist),
                onClick = { viewModel.readSpotifyPlaylist(defaultName) },
                enabled = viewModel.link.isNotBlank(),
                icon = R.drawable.download,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun KeysCard(viewModel: SpotifyImportViewModel) {
    val uriHandler = LocalUriHandler.current
    var showSecret by remember { mutableStateOf(false) }
    BaajaCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowOffset = 3.dp,
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier
                        .size(40.dp)
                        .baajaSurface(
                            baajaInk,
                            if (viewModel.keysSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                            RoundedCornerShape(12.dp),
                            shadowOffset = 0.dp,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(if (viewModel.keysSaved) R.drawable.check else R.drawable.key),
                        null,
                        tint = if (viewModel.keysSaved) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.baaja_imp_keys_title), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(if (viewModel.keysSaved) R.string.baaja_imp_keys_saved else R.string.baaja_imp_keys_missing),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (viewModel.keysSaved && !viewModel.editingKeys) {
                    BaajaChip(
                        label = stringResource(R.string.baaja_imp_keys_edit),
                        selected = false,
                        onClick = { viewModel.editingKeys = true },
                    )
                }
            }

            if (viewModel.editingKeys) {
                Text(stringResource(R.string.baaja_imp_keys_steps), fontSize = 14.sp, lineHeight = 20.sp)
                BaajaPillButton(
                    text = stringResource(R.string.baaja_imp_keys_open_dashboard),
                    onClick = { uriHandler.openUri(SPOTIFY_DASHBOARD_URL) },
                    primary = false,
                    icon = R.drawable.language,
                    height = 48.dp,
                    modifier = Modifier.fillMaxWidth(),
                )
                FeatureTextField(
                    value = viewModel.clientId,
                    onValueChange = { viewModel.clientId = it },
                    placeholder = stringResource(R.string.baaja_imp_client_id),
                    leadingIcon = R.drawable.key_vertical,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Next,
                    shape = RoundedCornerShape(16.dp),
                )
                FeatureTextField(
                    value = viewModel.clientSecret,
                    onValueChange = { viewModel.clientSecret = it },
                    placeholder = stringResource(R.string.baaja_imp_client_secret),
                    leadingIcon = R.drawable.lock,
                    secret = !showSecret,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    onImeAction = viewModel::saveKeys,
                    shape = RoundedCornerShape(16.dp),
                    trailing = {
                        val label = stringResource(if (showSecret) R.string.baaja_imp_hide_secret else R.string.baaja_imp_show_secret)
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable(role = Role.Button) { showSecret = !showSecret }
                                .semantics { contentDescription = label },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(painterResource(if (showSecret) R.drawable.lock_open else R.drawable.lock), null, modifier = Modifier.size(20.dp))
                        }
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    BaajaPillButton(
                        text = stringResource(R.string.baaja_imp_keys_save),
                        onClick = viewModel::saveKeys,
                        enabled = viewModel.clientId.isNotBlank() && viewModel.clientSecret.isNotBlank(),
                        height = 48.dp,
                        modifier = Modifier.weight(1f),
                    )
                    if (viewModel.keysSaved) {
                        BaajaPillButton(
                            text = stringResource(R.string.baaja_imp_keys_clear),
                            onClick = viewModel::clearKeys,
                            primary = false,
                            height = 48.dp,
                        )
                    }
                }
                Text(
                    stringResource(R.string.baaja_imp_keys_note),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CsvCard(onPick: () -> Unit) {
    BaajaCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.baaja_imp_csv_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                stringResource(R.string.baaja_imp_csv_body),
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            BaajaPillButton(
                text = stringResource(R.string.baaja_imp_csv_pick),
                onClick = onPick,
                icon = R.drawable.upload,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ErrorCard(error: ImportError, serverCode: Int, serverDetail: String?) {
    val base =
        when (error) {
            ImportError.INVALID_LINK -> stringResource(R.string.baaja_imp_err_invalid_link)
            ImportError.SHORT_LINK -> stringResource(R.string.baaja_imp_err_short_link)
            ImportError.NOT_PLAYLIST -> stringResource(R.string.baaja_imp_err_not_playlist)
            ImportError.NO_KEYS -> stringResource(R.string.baaja_imp_err_no_keys)
            ImportError.BAD_KEYS -> stringResource(R.string.baaja_imp_err_bad_keys)
            ImportError.NOT_FOUND -> stringResource(R.string.baaja_imp_err_not_found)
            ImportError.RATE_LIMITED -> stringResource(R.string.baaja_imp_err_rate)
            ImportError.NETWORK -> stringResource(R.string.baaja_imp_err_network)
            ImportError.SERVER -> stringResource(R.string.baaja_imp_err_server, serverCode)
            ImportError.CSV_COLUMNS -> stringResource(R.string.baaja_imp_err_csv_columns)
            ImportError.CSV_READ -> stringResource(R.string.baaja_imp_err_csv_read)
            ImportError.EMPTY -> stringResource(R.string.baaja_imp_err_empty)
        }
    val showDetail = serverDetail != null && (error == ImportError.BAD_KEYS || error == ImportError.NOT_FOUND || error == ImportError.SERVER)
    val message = if (showDetail) base + "\n\n" + stringResource(R.string.baaja_imp_err_spotify_says, serverDetail.orEmpty(), serverCode) else base
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .baajaSurface(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(18.dp), shadowOffset = 0.dp)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.error), null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(22.dp))
        Text(message, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MatchProgressCard(viewModel: SpotifyImportViewModel, phase: ImportPhase) {
    val total = viewModel.rows.size
    BaajaCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(if (phase == ImportPhase.Matching) R.string.baaja_imp_matching else R.string.baaja_imp_review_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        if (phase == ImportPhase.Matching) {
                            stringResource(R.string.baaja_imp_progress, viewModel.checked, total)
                        } else {
                            stringResource(R.string.baaja_imp_review_body)
                        },
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                if (phase == ImportPhase.Matching) {
                    BaajaChip(label = stringResource(R.string.baaja_imp_stop), selected = false, onClick = viewModel::stopMatching)
                } else if (phase == ImportPhase.Saving) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                }
            }
            FeatureProgressBar(if (total == 0) 0f else viewModel.checked.toFloat() / total)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CountPill(stringResource(R.string.baaja_imp_matched_count, viewModel.found), MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
                CountPill(stringResource(R.string.baaja_imp_not_found_count, viewModel.notFound), MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface)
                if (viewModel.failed > 0) {
                    CountPill(stringResource(R.string.baaja_imp_failed_count, viewModel.failed), MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary)
                }
            }
            if (viewModel.skipped > 0) {
                Text(
                    stringResource(R.string.baaja_imp_skipped, viewModel.skipped),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun CountPill(
    text: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
) {
    Text(
        text,
        color = content,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier =
            Modifier
                .baajaSurface(baajaInk, container, CircleShape, shadowOffset = 0.dp, borderWidth = 1.5.dp)
                .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun ReviewControls(viewModel: SpotifyImportViewModel) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.baaja_imp_playlist_name), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        FeatureTextField(
            value = viewModel.playlistName,
            onValueChange = { viewModel.playlistName = it },
            placeholder = stringResource(R.string.baaja_imp_default_name),
            leadingIcon = R.drawable.queue_music,
            shape = RoundedCornerShape(16.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BaajaChip(label = stringResource(R.string.baaja_imp_select_all), selected = false, onClick = { viewModel.selectAll(true) })
            BaajaChip(label = stringResource(R.string.baaja_imp_select_none), selected = false, onClick = { viewModel.selectAll(false) })
            if (viewModel.failed > 0) {
                BaajaChip(
                    label = stringResource(R.string.baaja_imp_retry_failed),
                    selected = false,
                    onClick = viewModel::retryFailed,
                    icon = R.drawable.refresh,
                )
            }
        }
    }
}

@Composable
private fun ImportRowItem(
    row: ImportRow,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    val match = row.match
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .then(
                if (match != null) {
                    Modifier.toggleable(
                        value = row.selected,
                        enabled = enabled,
                        role = Role.Checkbox,
                        onValueChange = { onToggle() },
                    )
                } else {
                    Modifier
                },
            ).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            when (row.status) {
                MatchStatus.FOUND -> FeatureCheckbox(checked = row.selected)
                MatchStatus.SEARCHING -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp)
                MatchStatus.WAITING -> Spacer(Modifier.size(20.dp))
                MatchStatus.NOT_FOUND, MatchStatus.FAILED ->
                    Icon(
                        painterResource(if (row.status == MatchStatus.FAILED) R.drawable.warning else R.drawable.search_off),
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
            }
        }
        FeatureArtwork(model = match?.thumbnail, size = 52.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                row.track.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (match == null && row.status != MatchStatus.SEARCHING && row.status != MatchStatus.WAITING) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            if (row.track.artists.isNotEmpty()) {
                Text(
                    row.track.artistLine,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val statusText =
                when (row.status) {
                    MatchStatus.FOUND ->
                        match?.let { song -> "↳ ${song.title} · ${song.artists.joinToString(", ") { it.name }}" }
                    MatchStatus.WAITING -> stringResource(R.string.baaja_imp_status_waiting)
                    MatchStatus.SEARCHING -> stringResource(R.string.baaja_imp_status_searching)
                    MatchStatus.NOT_FOUND -> stringResource(R.string.baaja_imp_status_not_found)
                    MatchStatus.FAILED -> stringResource(R.string.baaja_imp_status_failed)
                }
            if (statusText != null) {
                Text(
                    statusText,
                    fontSize = 13.sp,
                    fontWeight = if (row.status == MatchStatus.FOUND) FontWeight.SemiBold else FontWeight.Normal,
                    color =
                        when (row.status) {
                            MatchStatus.FOUND -> MaterialTheme.colorScheme.primary
                            MatchStatus.FAILED -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DoneCard(
    phase: ImportPhase.Done,
    onOpen: () -> Unit,
    onAgain: () -> Unit,
) {
    BaajaCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.size(56.dp).baajaSurface(baajaInk, MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp), shadowOffset = 3.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.check), null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(30.dp))
            }
            Text(
                stringResource(R.string.baaja_imp_saved, phase.name, phase.count),
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            BaajaPillButton(
                text = stringResource(R.string.baaja_imp_open_playlist),
                onClick = onOpen,
                icon = R.drawable.queue_music,
                modifier = Modifier.fillMaxWidth(),
            )
            BaajaPillButton(
                text = stringResource(R.string.baaja_imp_start_over),
                onClick = onAgain,
                primary = false,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(2.dp))
        }
    }
}
