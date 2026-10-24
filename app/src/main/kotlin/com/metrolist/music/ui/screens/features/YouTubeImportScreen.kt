/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.features

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
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
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.ui.theme.BaajaCard
import com.metrolist.music.ui.theme.BaajaChip
import com.metrolist.music.ui.theme.BaajaPillButton
import com.metrolist.music.ui.theme.baajaInk
import com.metrolist.music.ui.theme.baajaSurface
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.launch

@Composable
fun YouTubeImportScreen(
    navController: NavController,
    viewModel: YouTubeImportViewModel = hiltViewModel(),
) {
    val phase = viewModel.phase
    val insets = LocalPlayerAwareWindowInsets.current
    val (innerTubeCookie) = rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) { "SAPISID" in parseCookieString(innerTubeCookie) }
    LaunchedEffect(isLoggedIn) { if (isLoggedIn) viewModel.loadAccountPlaylists() }

    BackHandler(enabled = phase !is YtiPhase.Input) {
        if (phase is YtiPhase.Fetching || phase is YtiPhase.Importing) viewModel.cancel() else viewModel.startOver()
    }

    viewModel.conflict?.let { conflict ->
        AlertDialog(
            onDismissRequest = viewModel::dismissConflict,
            title = { Text(stringResource(R.string.baaja_yti_dup_title, conflict.title), fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.baaja_yti_dup_body))
                    BaajaPillButton(
                        text = stringResource(R.string.baaja_yti_dup_replace),
                        onClick = { viewModel.resolveConflict(YtiDuplicate.REPLACE) },
                        height = 48.dp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    BaajaPillButton(
                        text = stringResource(R.string.baaja_yti_dup_add),
                        onClick = { viewModel.resolveConflict(YtiDuplicate.ADD) },
                        primary = false,
                        height = 48.dp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    BaajaPillButton(
                        text = stringResource(R.string.baaja_yti_dup_both, conflict.newName),
                        onClick = { viewModel.resolveConflict(YtiDuplicate.BOTH) },
                        primary = false,
                        height = 48.dp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = viewModel::dismissConflict, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.baaja_yti_cancel))
                }
            },
        )
    }

    LazyColumn(
        contentPadding =
            insets
                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                .asPaddingValues()
                .let { PaddingValues(bottom = it.calculateBottomPadding() + 24.dp) },
        modifier =
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        item(key = "header") {
            FeatureHeader(
                title = stringResource(R.string.baaja_yti_title),
                subtitle = stringResource(R.string.baaja_yti_subtitle),
                onBack = { navController.navigateUp() },
            )
        }

        if (viewModel.hintVisible && phase !is YtiPhase.Done) {
            item(key = "hint") { HintCard(onDismiss = viewModel::dismissHint) }
        }
        viewModel.error?.let { err ->
            item(key = "error") {
                ErrorCard(err, canRetry = err == YtiError.PRIVATE || err == YtiError.NETWORK, onRetry = viewModel::retry)
            }
        }

        when (phase) {
            YtiPhase.Input -> {
                item(key = "link") { LinkCard(viewModel) }
                accountSection(viewModel, isLoggedIn)
            }

            YtiPhase.Fetching ->
                item(key = "fetching") {
                    ProgressCard(
                        title = stringResource(R.string.baaja_yti_loading_preview),
                        detail = null,
                        progress = null,
                        working = false,
                        onCancel = viewModel::cancel,
                    )
                }

            is YtiPhase.Preview ->
                item(key = "preview") {
                    PreviewCard(phase.info, onImport = viewModel::startImport, onBack = viewModel::startOver)
                }

            is YtiPhase.Importing ->
                item(key = "importing") {
                    val detail =
                        when {
                            phase.saving -> stringResource(R.string.baaja_yti_saving)
                            phase.total != null && phase.total > 0 ->
                                stringResource(R.string.baaja_yti_progress, phase.loaded, maxOf(phase.total, phase.loaded))
                            else -> stringResource(R.string.baaja_yti_progress_unknown, phase.loaded)
                        }
                    val title =
                        if (phase.count > 1) {
                            stringResource(R.string.baaja_yti_importing_item, phase.index, phase.count, phase.name)
                        } else {
                            stringResource(R.string.baaja_yti_importing)
                        }
                    ProgressCard(
                        title = title,
                        detail = detail,
                        progress =
                            if (phase.saving) {
                                1f
                            } else if (phase.total != null && phase.total > 0) {
                                phase.loaded.toFloat() / maxOf(phase.total, phase.loaded)
                            } else {
                                null
                            },
                        working = phase.saving,
                        onCancel = viewModel::cancel,
                    )
                }

            is YtiPhase.Done ->
                item(key = "done") {
                    DoneCard(
                        phase = phase,
                        onOpen = { id -> navController.navigate("local_playlist/$id") },
                        onAgain = viewModel::startOver,
                    )
                }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.accountSection(
    viewModel: YouTubeImportViewModel,
    isLoggedIn: Boolean,
) {
    if (!isLoggedIn) {
        item(key = "account_guest") {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.lock), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Text(
                    stringResource(R.string.baaja_yti_account_signin),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }
    item(key = "account_title") {
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 20.dp, top = 14.dp, bottom = 4.dp)) {
            Text(stringResource(R.string.baaja_yti_account_title), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
    when (viewModel.accountState) {
        YtiAccountState.IDLE, YtiAccountState.LOADING ->
            item(key = "account_loading") {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                    Text(stringResource(R.string.baaja_yti_account_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

        YtiAccountState.FAILED ->
            item(key = "account_failed") {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.baaja_yti_account_failed),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    BaajaChip(
                        label = stringResource(R.string.baaja_yti_retry),
                        selected = false,
                        onClick = { viewModel.loadAccountPlaylists(force = true) },
                        icon = R.drawable.refresh,
                    )
                }
            }

        YtiAccountState.READY -> {
            if (viewModel.accountPlaylists.isEmpty()) {
                item(key = "account_empty") {
                    Text(
                        stringResource(R.string.baaja_yti_account_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    )
                }
            } else {
                item(key = "account_controls") {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BaajaChip(label = stringResource(R.string.baaja_yti_select_all), selected = false, onClick = { viewModel.selectAll(true) })
                        BaajaChip(label = stringResource(R.string.baaja_yti_select_none), selected = false, onClick = { viewModel.selectAll(false) })
                    }
                }
                items(viewModel.accountPlaylists, key = { "acc_${it.id}" }, contentType = { "account_playlist" }) { playlist ->
                    val checked = playlist.id in viewModel.selected
                    val subtitle = playlist.songCountText ?: playlist.author?.name.orEmpty()
                    val description = stringResource(R.string.baaja_yti_select_item_desc, playlist.title, subtitle)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp)
                            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { viewModel.toggleSelected(playlist.id) })
                            .semantics { contentDescription = description }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        FeatureCheckbox(checked = checked)
                        FeatureArtwork(model = playlist.thumbnail, size = 52.dp, placeholder = R.drawable.queue_music)
                        Column(Modifier.weight(1f)) {
                            Text(playlist.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (subtitle.isNotBlank()) {
                                Text(
                                    subtitle,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
                item(key = "account_import") {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BaajaPillButton(
                            text = stringResource(R.string.baaja_yti_import_selected, viewModel.selected.size),
                            onClick = viewModel::importSelected,
                            enabled = viewModel.selected.isNotEmpty(),
                            icon = R.drawable.playlist_add,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            stringResource(R.string.baaja_yti_batch_note),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HintCard(onDismiss: () -> Unit) {
    BaajaCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowOffset = 3.dp,
    ) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.info), null, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(stringResource(R.string.baaja_yti_hint_title), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(stringResource(R.string.baaja_yti_hint_body), fontSize = 14.sp, lineHeight = 19.sp)
            }
            Text(
                stringResource(R.string.baaja_yti_hint_dismiss),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier =
                    Modifier
                        .heightIn(min = 48.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onDismiss)
                        .padding(horizontal = 12.dp)
                        .padding(vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun LinkCard(viewModel: YouTubeImportViewModel) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val pasteDesc = stringResource(R.string.baaja_yti_paste_desc)
    BaajaCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.baaja_yti_link_label), fontSize = 17.sp, fontWeight = FontWeight.Bold)
            FeatureTextField(
                value = viewModel.link,
                onValueChange = viewModel::onLinkChange,
                placeholder = stringResource(R.string.baaja_yti_link_hint),
                leadingIcon = R.drawable.link,
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Go,
                onImeAction = viewModel::previewLink,
                trailing = {
                    Text(
                        stringResource(R.string.baaja_yti_paste),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier =
                            Modifier
                                .heightIn(min = 48.dp)
                                .clip(CircleShape)
                                .clickable(role = Role.Button) {
                                    scope.launch {
                                        val clip = clipboard.getClipEntry()?.clipData
                                        val text =
                                            if (clip != null && clip.itemCount > 0) clip.getItemAt(0).coerceToText(context)?.toString() else null
                                        if (!text.isNullOrBlank()) viewModel.onLinkChange(text.trim())
                                    }
                                }.semantics { contentDescription = pasteDesc }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                    )
                },
            )
            BaajaPillButton(
                text = stringResource(R.string.baaja_yti_preview),
                onClick = viewModel::previewLink,
                enabled = viewModel.link.isNotBlank(),
                icon = R.drawable.download,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ErrorCard(
    error: YtiError,
    canRetry: Boolean,
    onRetry: () -> Unit,
) {
    val message =
        stringResource(
            when (error) {
                YtiError.INVALID_LINK -> R.string.baaja_yti_invalid_link
                YtiError.NOT_PLAYLIST -> R.string.baaja_yti_not_playlist
                YtiError.PRIVATE -> R.string.baaja_yti_private
                YtiError.NETWORK -> R.string.baaja_yti_network
            },
        )
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .baajaSurface(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(18.dp), shadowOffset = 0.dp)
            .padding(14.dp)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.error), null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(22.dp))
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
        if (canRetry) {
            BaajaPillButton(
                text = stringResource(R.string.baaja_yti_retry),
                onClick = onRetry,
                primary = false,
                icon = R.drawable.refresh,
                height = 48.dp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ProgressCard(
    title: String,
    detail: String?,
    progress: Float?,
    working: Boolean,
    onCancel: () -> Unit,
) {
    BaajaCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
            if (detail != null) {
                Text(detail, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            if (progress != null) {
                FeatureProgressBar(progress)
            } else {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surface,
                )
            }
            if (!working) {
                BaajaPillButton(
                    text = stringResource(R.string.baaja_yti_cancel),
                    onClick = onCancel,
                    primary = false,
                    height = 48.dp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun PreviewCard(
    info: YtiPreview,
    onImport: () -> Unit,
    onBack: () -> Unit,
) {
    BaajaCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                FeatureArtwork(model = info.thumbnail, size = 88.dp, placeholder = R.drawable.queue_music, shadowOffset = 3.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(info.title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    info.author?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            stringResource(R.string.baaja_yti_preview_by, it),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    val count = info.songCount?.toString() ?: info.countText
                    if (!count.isNullOrBlank()) {
                        Text(
                            if (info.songCount != null) stringResource(R.string.baaja_yti_preview_count, count) else count,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            BaajaPillButton(
                text = stringResource(R.string.baaja_yti_import),
                onClick = onImport,
                icon = R.drawable.playlist_add,
                modifier = Modifier.fillMaxWidth(),
            )
            BaajaPillButton(
                text = stringResource(R.string.baaja_yti_import_another),
                onClick = onBack,
                primary = false,
                height = 48.dp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DoneCard(
    phase: YtiPhase.Done,
    onOpen: (String) -> Unit,
    onAgain: () -> Unit,
) {
    val total = phase.results.sumOf { it.count }
    BaajaCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            androidx.compose.foundation.layout.Box(
                Modifier.size(56.dp).baajaSurface(baajaInk, MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp), shadowOffset = 3.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.check), null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(30.dp))
            }
            Text(
                if (phase.results.size == 1) {
                    stringResource(R.string.baaja_yti_done_one, phase.results[0].name, total)
                } else {
                    stringResource(R.string.baaja_yti_done_many, phase.results.size, total)
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            if (phase.failed > 0) {
                Text(
                    stringResource(R.string.baaja_yti_done_failed, phase.failed),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            if (phase.results.size == 1) {
                BaajaPillButton(
                    text = stringResource(R.string.baaja_yti_open_playlist),
                    onClick = { onOpen(phase.results[0].playlistId) },
                    icon = R.drawable.queue_music,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            BaajaPillButton(
                text = stringResource(R.string.baaja_yti_import_another),
                onClick = onAgain,
                primary = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
