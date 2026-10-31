/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.menu

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.res.Configuration
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.metrolist.music.ui.component.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.metrolist.music.ui.component.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadService
import com.metrolist.music.LocalNavController
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.ui.screens.assistant.behindTheSongRoute
import com.metrolist.music.constants.ListItemHeight
import com.metrolist.music.listentogether.ConnectionState
import com.metrolist.music.listentogether.ListenTogetherEvent
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.playback.ExoDownloadService
import com.metrolist.music.db.entities.Song
import com.metrolist.music.db.entities.SpeedDialItem
import com.metrolist.music.ui.component.BottomSheetState
import com.metrolist.music.ui.component.ListDialog
import com.metrolist.music.ui.component.Material3MenuGroup
import com.metrolist.music.ui.component.Material3MenuItemData
import com.metrolist.music.ui.component.NewAction
import com.metrolist.music.ui.component.NewActionGrid
import com.metrolist.music.ui.component.VolumeSlider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.round

@Composable
fun PlayerMenu(
    mediaMetadata: MediaMetadata?,
    playerBottomSheetState: BottomSheetState,
    isQueueTrigger: Boolean? = false,
    onDismiss: () -> Unit,
) {
    mediaMetadata ?: return
    val navController = LocalNavController.current
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val playerVolume = playerConnection.service.playerVolume.collectAsStateWithLifecycle()

    // Cast state for volume control - safely access castConnectionHandler to prevent crashes
    val castHandler =
        remember(playerConnection) {
            try {
                playerConnection.service.castConnectionHandler
            } catch (e: Exception) {
                null
            }
        }
    val isCasting by castHandler?.isCasting?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }
    val castVolume by castHandler?.castVolume?.collectAsStateWithLifecycle() ?: remember { mutableFloatStateOf(1f) }
    val castDeviceName by castHandler?.castDeviceName?.collectAsStateWithLifecycle() ?: remember { mutableStateOf<String?>(null) }

    val librarySong by database.song(mediaMetadata.id).collectAsStateWithLifecycle(initialValue = null)
    val notInterestedItem = rememberNotInterestedItem(mediaMetadata.id, onDismiss)
    val coroutineScope = rememberCoroutineScope()
    val downloadUtil = LocalDownloadUtil.current

    val download by downloadUtil
        .getDownload(mediaMetadata.id)
        .collectAsStateWithLifecycle(initialValue = null)

    val isPinned by database.speedDialDao.isPinned(mediaMetadata.id).collectAsStateWithLifecycle(initialValue = false)

    val artists =
        remember(mediaMetadata.artists) {
            mediaMetadata.artists.filter { it.id != null }
        }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val listenTogetherManager = LocalListenTogetherManager.current
    val listenTogetherRoleState = listenTogetherManager?.role?.collectAsStateWithLifecycle(initialValue = com.metrolist.music.listentogether.RoomRole.NONE)
    val isListenTogetherGuest = listenTogetherRoleState?.value == com.metrolist.music.listentogether.RoomRole.GUEST
    val pendingSuggestions by listenTogetherManager?.pendingSuggestions?.collectAsStateWithLifecycle(initialValue = emptyList())
        ?: remember { mutableStateOf(emptyList()) }

    val systemEqLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = {
            database.withTransaction {
                insert(mediaMetadata)
            }
            listOf(mediaMetadata.id)
        },
        onGetSongIds = { listOf(mediaMetadata.id) },
        onDismiss = {
            showChoosePlaylistDialog = false
        },
    )

    var showSelectArtistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showSelectArtistDialog) {
        ListDialog(
            onDismiss = { showSelectArtistDialog = false },
        ) {
            items(artists) { artist ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier =
                        Modifier
                            .fillParentMaxWidth()
                            .height(ListItemHeight)
                            .clickable {
                                navController.navigate("artist/${artist.id}")
                                showSelectArtistDialog = false
                                playerBottomSheetState.collapseSoft()
                                onDismiss()
                            }.padding(horizontal = 24.dp),
                ) {
                    Text(
                        text = artist.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    var showPitchTempoDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showPitchTempoDialog) {
        TempoPitchDialog(
            onDismiss = { showPitchTempoDialog = false },
        )
    }

    if (isQueueTrigger != true) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 24.dp, bottom = 6.dp),
        ) {
            // Show Cast indicator when casting
            if (isCasting && castDeviceName != null) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.cast),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.casting_to, castDeviceName ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                VolumeSlider(
                    value = if (isCasting) castVolume else playerVolume.value,
                    onValueChange = { volume ->
                        if (isCasting) {
                            castHandler?.setVolume(volume)
                        } else {
                            playerConnection.service.playerVolume.value = volume
                        }
                    },
                    modifier = Modifier.weight(1f),
                    accentColor = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    HorizontalDivider()

    Spacer(modifier = Modifier.height(12.dp))

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    LazyColumn(
        contentPadding =
            PaddingValues(
                start = 0.dp,
                top = 0.dp,
                end = 0.dp,
                bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
            ),
    ) {
        item {
            val startingRadioText = stringResource(R.string.starting_radio)
            NewActionGrid(
                actions =
                    listOfNotNull(
                        if (!isListenTogetherGuest) {
                            NewAction(
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.radio),
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                text = stringResource(R.string.start_radio),
                                onClick = {
                                    Toast.makeText(context, startingRadioText, Toast.LENGTH_SHORT).show()
                                    playerConnection.startRadioSeamlessly()
                                    onDismiss()
                                },
                            )
                        } else {
                            null
                        },
                        NewAction(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.playlist_add),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            text = stringResource(R.string.add_to_playlist),
                            onClick = { showChoosePlaylistDialog = true },
                        ),
                        NewAction(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.link),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            text = stringResource(R.string.copy_link),
                            onClick = {
                                val clipboard =
                                    context.getSystemService(
                                        android.content.Context.CLIPBOARD_SERVICE,
                                    ) as android.content.ClipboardManager
                                val clip =
                                    android.content.ClipData.newPlainText(
                                        "Song Link",
                                        "https://music.youtube.com/watch?v=${mediaMetadata.id}",
                                    )
                                clipboard.setPrimaryClip(clip)
                                android.widget.Toast
                                    .makeText(context, R.string.link_copied, android.widget.Toast.LENGTH_SHORT)
                                    .show()
                                onDismiss()
                            },
                        ),
                    ),
                columns = if (isListenTogetherGuest) 2 else 3,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
            )
        }

        item {
            // Check if this is a podcast episode (album ID doesn't start with MPREb_)
            val isPodcast = mediaMetadata.album?.let { !it.id.startsWith("MPREb_") } ?: false

            Material3MenuGroup(
                items =
                    buildList {
                        // Don't show "View Artist" for podcasts - only show "View Podcast"
                        if (artists.isNotEmpty() && !isPodcast) {
                            add(
                                Material3MenuItemData(
                                    title = { Text(text = stringResource(R.string.view_artist)) },
                                    description = {
                                        Text(
                                            text = mediaMetadata.artists.joinToString { it.name },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.artist),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    },
                                    onClick = {
                                        if (mediaMetadata.artists.size == 1) {
                                            navController.navigate("artist/${mediaMetadata.artists[0].id}")
                                            playerBottomSheetState.collapseSoft()
                                            onDismiss()
                                        } else {
                                            showSelectArtistDialog = true
                                        }
                                    },
                                ),
                            )
                        }
                        if (mediaMetadata.album != null) {
                            add(
                                Material3MenuItemData(
                                    title = { Text(text = stringResource(if (isPodcast) R.string.view_podcast else R.string.view_album)) },
                                    description = {
                                        Text(
                                            text = mediaMetadata.album.title,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    icon = {
                                        Icon(
                                            painter = painterResource(if (isPodcast) R.drawable.mic else R.drawable.album),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    },
                                    onClick = {
                                        if (isPodcast) {
                                            navController.navigate("online_podcast/${mediaMetadata.album.id}")
                                        } else {
                                            navController.navigate("album/${mediaMetadata.album.id}")
                                        }
                                        playerBottomSheetState.collapseSoft()
                                        onDismiss()
                                    },
                                ),
                            )
                        }
                        // Add to Library option
                        val isInLibrary = librarySong?.song?.inLibrary != null
                        add(
                            Material3MenuItemData(
                                title = {
                                    Text(
                                        text =
                                            stringResource(
                                                if (isInLibrary) {
                                                    R.string.remove_from_library
                                                } else {
                                                    R.string.add_to_library
                                                },
                                            ),
                                    )
                                },
                                icon = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                if (isInLibrary) {
                                                    R.drawable.library_add_check
                                                } else {
                                                    R.drawable.library_add
                                                },
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                    )
                                },
                                onClick = {
                                    playerConnection.toggleLibrary()
                                    onDismiss()
                                },
                            ),
                        )
                        add(
                            Material3MenuItemData(
                                title = {
                                    Text(
                                        text = if (isPinned) stringResource(R.string.unpin_from_speed_dial) else stringResource(R.string.pin_to_speed_dial),
                                    )
                                },
                                icon = {
                                    Icon(
                                        painter = painterResource(if (isPinned) R.drawable.remove else R.drawable.add),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                    )
                                },
                                onClick = {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        if (isPinned) {
                                            database.speedDialDao.delete(mediaMetadata.id)
                                        } else {
                                            database.speedDialDao.insert(SpeedDialItem.fromYTItem(mediaMetadata.toYTItem()))
                                        }
                                    }
                                    onDismiss()
                                },
                            ),
                        )
                    },
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        item {
            Material3MenuGroup(
                items =
                    listOf(
                        when (download?.state) {
                            Download.STATE_COMPLETED -> {
                                Material3MenuItemData(
                                    title = {
                                        Text(
                                            text = stringResource(R.string.remove_download),
                                        )
                                    },
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.offline),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    },
                                    onClick = {
                                        DownloadService.sendRemoveDownload(
                                            context,
                                            ExoDownloadService::class.java,
                                            mediaMetadata.id,
                                            false,
                                        )
                                    },
                                )
                            }

                            Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                                Material3MenuItemData(
                                    title = { Text(text = stringResource(R.string.downloading)) },
                                    icon = {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp,
                                        )
                                    },
                                    onClick = {
                                        DownloadService.sendRemoveDownload(
                                            context,
                                            ExoDownloadService::class.java,
                                            mediaMetadata.id,
                                            false,
                                        )
                                    },
                                )
                            }

                            else -> {
                                Material3MenuItemData(
                                    title = { Text(text = stringResource(R.string.action_download)) },
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.download),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    },
                                    onClick = {
                                        downloadUtil.download(mediaMetadata)
                                    },
                                )
                            }
                        },
                    ),
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        if (isListenTogetherGuest) item {
            Material3MenuGroup(
                items =
                    buildList {
                        if (isListenTogetherGuest) {
                            add(
                                Material3MenuItemData(
                                    title = { Text(text = stringResource(R.string.resync)) },
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.replay),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    },
                                    onClick = {
                                        listenTogetherManager.requestSync()
                                        onDismiss()
                                    },
                                ),
                            )
                        }
                    },
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        item {
            Material3MenuGroup(
                items =
                    buildList {
                        if (librarySong?.song?.isLocal != true) notInterestedItem?.let(::add)
                        add(
                            Material3MenuItemData(
                                title = { Text(text = stringResource(R.string.baaja_player_behind_the_song)) },
                                description = { Text(text = stringResource(R.string.baaja_player_behind_the_song_desc)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.info),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                    )
                                },
                                onClick = {
                                    navController.navigate(behindTheSongRoute(mediaMetadata.id))
                                    playerBottomSheetState.collapseSoft()
                                    onDismiss()
                                },
                            ),
                        )

                        if (isQueueTrigger != true) {
                            add(
                                Material3MenuItemData(
                                    title = { Text(text = stringResource(R.string.equalizer)) },
                                    description = { Text(text = stringResource(R.string.equalizer_desc)) },
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.equalizer),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    },
                                    onClick = {
                                        navController.navigate("equalizer")
                                        onDismiss()
                                    },
                                ),
                            )
                            add(
                                Material3MenuItemData(
                                    title = { Text(text = stringResource(R.string.advanced)) },
                                    description = { Text(text = stringResource(R.string.advanced_desc)) },
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.tune),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    },
                                    onClick = {
                                        showPitchTempoDialog = true
                                    },
                                ),
                            )
                        }
                    },
            )
        }
    }
}

@Composable
fun TempoPitchDialog(onDismiss: () -> Unit) {
    val playerConnection = LocalPlayerConnection.current ?: return
    var tempo by remember {
        mutableFloatStateOf(playerConnection.player.playbackParameters.speed)
    }
    var transposeValue by remember {
        mutableIntStateOf(round(12 * log2(playerConnection.player.playbackParameters.pitch)).toInt())
    }
    val updatePlaybackParameters = {
        playerConnection.player.playbackParameters =
            PlaybackParameters(tempo, 2f.pow(transposeValue.toFloat() / 12))
    }
    val listenTogetherManager = com.metrolist.music.LocalListenTogetherManager.current
    val isInRoom = listenTogetherManager?.isInRoom ?: false

    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.tempo_and_pitch))
        },
        dismissButton = {
            TextButton(
                onClick = {
                    tempo = 1f
                    transposeValue = 0
                    updatePlaybackParameters()
                },
            ) {
                Text(stringResource(R.string.reset))
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        text = {
            Column {
                if (!isInRoom) {
                    ValueAdjuster(
                        icon = R.drawable.speed,
                        currentValue = tempo,
                        values = (0..35).map { round((0.25f + it * 0.05f) * 100) / 100 },
                        onValueUpdate = {
                            tempo = it
                            updatePlaybackParameters()
                        },
                        valueText = { "x$it" },
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                ValueAdjuster(
                    icon = R.drawable.discover_tune,
                    currentValue = transposeValue,
                    values = (-12..12).toList(),
                    onValueUpdate = {
                        transposeValue = it
                        updatePlaybackParameters()
                    },
                    valueText = { "${if (it > 0) "+" else ""}$it" },
                )
            }
        },
    )
}

@Composable
fun <T> ValueAdjuster(
    @DrawableRes icon: Int,
    currentValue: T,
    values: List<T>,
    onValueUpdate: (T) -> Unit,
    valueText: (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
        )

        IconButton(
            enabled = currentValue != values.first(),
            onClick = {
                onValueUpdate(values[values.indexOf(currentValue) - 1])
            },
        ) {
            Icon(
                painter = painterResource(R.drawable.remove),
                contentDescription = null,
            )
        }

        Text(
            text = valueText(currentValue),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(80.dp),
        )

        IconButton(
            enabled = currentValue != values.last(),
            onClick = {
                onValueUpdate(values[values.indexOf(currentValue) + 1])
            },
        ) {
            Icon(
                painter = painterResource(R.drawable.add),
                contentDescription = null,
            )
        }
    }
}
