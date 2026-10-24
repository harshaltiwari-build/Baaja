/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.player

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import com.metrolist.music.ui.component.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import com.metrolist.music.ui.component.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import com.metrolist.music.ui.theme.baajaInk
import com.metrolist.music.ui.theme.baajaSurface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import com.metrolist.music.LocalNavController
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.ListItemHeight
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.QueueEditLockKey
import com.metrolist.music.extensions.metadata
import com.metrolist.music.extensions.move
import com.metrolist.music.extensions.toggleRepeatMode
import com.metrolist.music.listentogether.RoomRole
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.component.ActionPromptDialog
import com.metrolist.music.ui.component.BottomSheet
import com.metrolist.music.ui.component.BottomSheetState
import com.metrolist.music.ui.component.LocalBottomSheetPageState
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.MediaMetadataListItem
import com.metrolist.music.ui.menu.PlayerMenu
import com.metrolist.music.ui.menu.QueueMenu
import com.metrolist.music.ui.menu.SelectionMediaMetadataMenu
import com.metrolist.music.ui.utils.ShowMediaInfo
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.utils.safeDataStoreEdit
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.math.roundToInt
import com.metrolist.music.constants.SleepTimerDefaultKey
import android.widget.Toast
import androidx.compose.runtime.derivedStateOf
import com.metrolist.music.constants.SleepTimerFadeOutKey
import com.metrolist.music.constants.SleepTimerStopAfterCurrentSongKey
import androidx.compose.runtime.derivedStateOf
import androidx.compose.material3.Button


@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Queue(
    state: BottomSheetState,
    playerBottomSheetState: BottomSheetState,
    modifier: Modifier = Modifier,
    background: Color,
    onBackgroundColor: Color,
    TextBackgroundColor: Color,
    textButtonColor: Color,
    iconButtonColor: Color,
    pureBlack: Boolean,
    showInlineLyrics: Boolean,
    playerBackground: PlayerBackgroundStyle = PlayerBackgroundStyle.DEFAULT,
    onToggleLyrics: () -> Unit = {},
) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboard.current
    val menuState = LocalMenuState.current
    val sleepTimerDefaultSetTemplate = stringResource(R.string.sleep_timer_default_set)
    val bottomSheetPageState = LocalBottomSheetPageState.current

    // Listen Together state (reactive)
    val listenTogetherManager = LocalListenTogetherManager.current
    val listenTogetherRoleState = listenTogetherManager?.role?.collectAsStateWithLifecycle(initialValue = com.metrolist.music.listentogether.RoomRole.NONE)
    val isListenTogetherGuest = listenTogetherRoleState?.value == RoomRole.GUEST

    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val repeatMode by playerConnection.repeatMode.collectAsStateWithLifecycle()

    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()

    val currentFormat by playerConnection.currentFormat.collectAsStateWithLifecycle(initialValue = null)

    val selectedSongs = remember { mutableStateListOf<MediaMetadata>() }
    val selectedItems = remember { mutableStateListOf<Timeline.Window>() }

    // Cast state - safely access castConnectionHandler to prevent crashes during service lifecycle changes
    val castHandler =
        remember(playerConnection) {
            try {
                playerConnection.service.castConnectionHandler
            } catch (e: Exception) {
                null
            }
        }
    val isCasting by castHandler?.isCasting?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }

    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection =
        rememberSaveable(
            saver =
                listSaver<MutableList<String>, String>(
                    save = { it.toList() },
                    restore = { it.toMutableStateList() },
                ),
        ) { mutableStateListOf() }
    val onExitSelectionMode = {
        inSelectMode = false
        selection.clear()
    }
    if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
    }

    var locked by rememberPreference(QueueEditLockKey, defaultValue = true)

    val baajaColors = rememberBaajaPlayerColors(onImageBackground = playerBackground != PlayerBackgroundStyle.DEFAULT)
    val queueInk = baajaInk

    val snackbarHostState = remember { SnackbarHostState() }
    var dismissJob: Job? by remember { mutableStateOf(null) }

    val coroutineScope = rememberCoroutineScope()
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    val sleepTimerDefault by rememberPreference(SleepTimerDefaultKey, 30f)
    var sleepTimerValue by remember { mutableFloatStateOf(sleepTimerDefault) }
    val isAtDefault by remember {
        derivedStateOf { sleepTimerValue.roundToInt() == sleepTimerDefault.roundToInt() }
    }
    LaunchedEffect(sleepTimerDefault) { sleepTimerValue = sleepTimerDefault }
    val sleepTimerStopAfterCurrentSong by rememberPreference(SleepTimerStopAfterCurrentSongKey, false)
    val sleepTimerFadeOut by rememberPreference(SleepTimerFadeOutKey, false)
    val sleepTimerEnabled = remember(
        playerConnection.service.sleepTimer?.triggerTime,
        playerConnection.service.sleepTimer?.pauseWhenSongEnd
    ) {
        playerConnection.service.sleepTimer?.isActive ?: false
    }
    var sleepTimerTimeLeft by remember { mutableLongStateOf(0L) }

    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft =
                    if (playerConnection.service.sleepTimer?.pauseWhenSongEnd == true) {
                        playerConnection.player.duration - playerConnection.player.currentPosition
                    } else {
                        (playerConnection.service.sleepTimer?.triggerTime ?: 0L) - System.currentTimeMillis()
                    }
                delay(1000L)
            }
        }
    }

    BottomSheet(
        state = state,
        modifier = modifier,
        background = {
            Box(Modifier.fillMaxSize().background(Color.Unspecified))
        },
        collapsedContent = {
            val sleepTimerLabel = stringResource(R.string.sleep_timer)
            val onSleepTimerClick: () -> Unit = {
                if (!isListenTogetherGuest) {
                    if (sleepTimerEnabled) {
                        playerConnection.service.sleepTimer?.clear()
                    } else {
                        showSleepTimerDialog = true
                    }
                }
            }
            // Mockup row: Lyrics · Sleep timer · Queue, plus the player menu.
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(
                            WindowInsets.systemBars.only(
                                WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal,
                            ),
                        ).padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                BaajaPlayerPill(
                    text = stringResource(R.string.lyrics),
                    icon = R.drawable.lyrics,
                    onClick = onToggleLyrics,
                    active = showInlineLyrics,
                    colors = baajaColors,
                )

                BaajaPlayerPill(
                    text = if (sleepTimerEnabled) makeTimeString(sleepTimerTimeLeft) else null,
                    icon = R.drawable.bedtime,
                    contentDescription = sleepTimerLabel,
                    onClick = onSleepTimerClick,
                    active = sleepTimerEnabled,
                    enabled = !isListenTogetherGuest,
                    colors = baajaColors,
                    modifier = if (sleepTimerEnabled) Modifier else Modifier.size(44.dp),
                )

                BaajaPlayerPill(
                    text = stringResource(R.string.queue),
                    icon = R.drawable.queue_music,
                    onClick = { state.expandSoft() },
                    colors = baajaColors,
                )

                BaajaPlayerPill(
                    text = null,
                    icon = R.drawable.more_vert,
                    contentDescription = stringResource(R.string.baaja_player_more),
                    onClick = {
                        menuState.show {
                            PlayerMenu(
                                mediaMetadata = mediaMetadata,
                                playerBottomSheetState = playerBottomSheetState,
                                onDismiss = menuState::dismiss,
                            )
                        }
                    },
                    colors = baajaColors,
                    modifier = Modifier.size(44.dp),
                )
            }

            if (showSleepTimerDialog) {
                ActionPromptDialog(
                    titleBar = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.sleep_timer),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                style = MaterialTheme.typography.headlineSmall,
                            )
                        }
                    },
                    onDismiss = { showSleepTimerDialog = false },
                    onConfirm = {
                        showSleepTimerDialog = false
                        playerConnection.service.sleepTimer?.start(
                            minute = sleepTimerValue.roundToInt(),
                            stopAfterCurrentSong = sleepTimerStopAfterCurrentSong,
                            fadeOut = sleepTimerFadeOut,
                        )
                    },
                    onCancel = {
                        showSleepTimerDialog = false
                    },
                    onReset = {
                        sleepTimerValue = sleepTimerDefault
                    },
                    content = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text =
                                    pluralStringResource(
                                        R.plurals.minute,
                                        sleepTimerValue.roundToInt(),
                                        sleepTimerValue.roundToInt(),
                                    ),
                                style = MaterialTheme.typography.bodyLarge,
                            )

                            Spacer(Modifier.height(16.dp))

                            Slider(
                                value = sleepTimerValue,
                                onValueChange = { sleepTimerValue = it },
                                valueRange = 5f..120f,
                                steps = (120 - 5) / 5 - 1,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Spacer(Modifier.height(8.dp))

                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                if (isAtDefault) {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                context.safeDataStoreEdit { settings ->
                                                    settings[SleepTimerDefaultKey] = sleepTimerValue
                                                }
                                            }
                                            Toast.makeText(
                                                context,
                                                String.format(sleepTimerDefaultSetTemplate, sleepTimerValue.roundToInt()),
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        },
                                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                        ),
                                    ) {
                                        Text(stringResource(R.string.set_as_default))
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                context.safeDataStoreEdit { settings ->
                                                    settings[SleepTimerDefaultKey] = sleepTimerValue
                                                }
                                            }
                                            Toast.makeText(
                                                context,
                                                String.format(sleepTimerDefaultSetTemplate, sleepTimerValue.roundToInt()),
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        },
                                    ) {
                                        Text(stringResource(R.string.set_as_default))
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        showSleepTimerDialog = false
                                        playerConnection.service.sleepTimer?.start(
                                            minute = -1,
                                        )
                                    },
                                ) {
                                    Text(stringResource(R.string.end_of_song))
                                }
                            }
                        }
                    },
                )
            }
        },
    ) {
        val queueTitle by playerConnection.queueTitle.collectAsStateWithLifecycle()
        val queueWindows by playerConnection.queueWindows.collectAsStateWithLifecycle()
        val automix by playerConnection.service.automixItems.collectAsStateWithLifecycle()
        val mutableQueueWindows = remember { mutableStateListOf<Timeline.Window>() }
        val queueLength =
            remember(queueWindows) {
                queueWindows.sumOf { it.mediaItem.metadata!!.duration }
            }

        val coroutineScope = rememberCoroutineScope()

        val headerItems = 1
        val lazyListState = rememberLazyListState()
        var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }

        val currentPlayingUid =
            remember(currentWindowIndex, queueWindows) {
                if (currentWindowIndex in queueWindows.indices) {
                    queueWindows[currentWindowIndex].uid
                } else {
                    null
                }
            }

        val reorderableState =
            rememberReorderableLazyListState(
                lazyListState = lazyListState,
                scrollThresholdPadding =
                    WindowInsets.systemBars
                        .add(
                            WindowInsets(
                                top = ListItemHeight,
                                bottom = ListItemHeight,
                            ),
                        ).asPaddingValues(),
            ) { from, to ->
                val currentDragInfo = dragInfo
                dragInfo =
                    if (currentDragInfo == null) {
                        from.index to to.index
                    } else {
                        currentDragInfo.first to to.index
                    }

                val safeFrom = (from.index - headerItems).coerceIn(0, mutableQueueWindows.lastIndex)
                val safeTo = (to.index - headerItems).coerceIn(0, mutableQueueWindows.lastIndex)

                mutableQueueWindows.move(safeFrom, safeTo)
            }

        LaunchedEffect(reorderableState.isAnyItemDragging) {
            if (!reorderableState.isAnyItemDragging) {
                dragInfo?.let { (from, to) ->
                    val safeFrom = (from - headerItems).coerceIn(0, queueWindows.lastIndex)
                    val safeTo = (to - headerItems).coerceIn(0, queueWindows.lastIndex)

                    if (!playerConnection.player.shuffleModeEnabled) {
                        playerConnection.player.moveMediaItem(safeFrom, safeTo)
                    } else {
                        playerConnection.player.setShuffleOrder(
                            DefaultShuffleOrder(
                                queueWindows
                                    .map { it.firstPeriodIndex }
                                    .toMutableList()
                                    .move(safeFrom, safeTo)
                                    .toIntArray(),
                                System.currentTimeMillis(),
                            ),
                        )
                    }
                    dragInfo = null
                }
            }
        }

        LaunchedEffect(queueWindows) {
            mutableQueueWindows.apply {
                clear()
                addAll(queueWindows)
            }
        }

        LaunchedEffect(mutableQueueWindows, currentWindowIndex) {
            if (currentWindowIndex != -1) {
                lazyListState.scrollToItem(currentWindowIndex)
            }
        }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(background),
        ) {
            LazyColumn(
                state = lazyListState,
                contentPadding =
                    WindowInsets.systemBars
                        .add(
                            WindowInsets(
                                top = ListItemHeight + 8.dp,
                                bottom = ListItemHeight + 8.dp,
                            ),
                        ).asPaddingValues(),
                modifier = Modifier.nestedScroll(state.preUpPostDownNestedScrollConnection),
            ) {
                item(key = "queue_top_spacer") {
                    Spacer(
                        modifier =
                            Modifier
                                .animateContentSize()
                                .height(if (inSelectMode) 48.dp else 0.dp),
                    )
                }

                itemsIndexed(
                    items = mutableQueueWindows,
                    key = { _, item -> item.uid.hashCode() },
                ) { index, window ->
                    ReorderableItem(
                        state = reorderableState,
                        key = window.uid.hashCode(),
                    ) {
                        val currentItem by rememberUpdatedState(window)
                        val isActive = window.uid == currentPlayingUid
                        val dismissBoxState =
                            rememberSwipeToDismissBoxState(
                                positionalThreshold = { totalDistance -> totalDistance },
                            )

                        var processedDismiss by remember { mutableStateOf(false) }
                        val removedSongMsg =
                            stringResource(R.string.removed_song_from_playlist, currentItem.mediaItem.metadata?.title ?: "")
                        val undoStr = stringResource(R.string.undo)
                        LaunchedEffect(dismissBoxState.currentValue) {
                            val dv = dismissBoxState.currentValue
                            if (!processedDismiss && !isListenTogetherGuest && (
                                    dv == SwipeToDismissBoxValue.StartToEnd ||
                                        dv == SwipeToDismissBoxValue.EndToStart
                                )
                            ) {
                                processedDismiss = true
                                playerConnection.player.removeMediaItem(currentItem.firstPeriodIndex)
                                dismissJob?.cancel()
                                dismissJob =
                                    coroutineScope.launch {
                                        val snackbarResult =
                                            snackbarHostState.showSnackbar(
                                                message = removedSongMsg,
                                                actionLabel = undoStr,
                                                duration = SnackbarDuration.Short,
                                            )
                                        if (snackbarResult == SnackbarResult.ActionPerformed) {
                                            playerConnection.player.addMediaItem(currentItem.mediaItem)
                                            playerConnection.player.moveMediaItem(
                                                mutableQueueWindows.size,
                                                currentItem.firstPeriodIndex,
                                            )
                                        }
                                    }
                            }
                            if (dv == SwipeToDismissBoxValue.Settled) {
                                processedDismiss = false
                            }
                        }

                        val onCheckedChange: (Boolean) -> Unit = {
                            if (it) {
                                selection.add(window.mediaItem.mediaId)
                            } else {
                                selection.remove(window.mediaItem.mediaId)
                            }
                        }

                        val content: @Composable () -> Unit = {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.animateItem(),
                            ) {
                                MediaMetadataListItem(
                                    mediaMetadata = window.mediaItem.metadata!!,
                                    isSelected = false,
                                    isActive = isActive,
                                    isPlaying = isPlaying && isActive,
                                    trailingContent = {
                                        if (inSelectMode) {
                                            Checkbox(
                                                checked = window.mediaItem.mediaId in selection,
                                                onCheckedChange = onCheckedChange,
                                            )
                                        } else {
                                            if (!isListenTogetherGuest) {
                                                IconButton(
                                                    onClick = {
                                                        menuState.show {
                                                            QueueMenu(
                                                                mediaMetadata = window.mediaItem.metadata!!,
                                                                playerBottomSheetState = playerBottomSheetState,
                                                                onShowDetailsDialog = {
                                                                    window.mediaItem.mediaId.let {
                                                                        bottomSheetPageState.show {
                                                                            ShowMediaInfo(it)
                                                                        }
                                                                    }
                                                                },
                                                                onDismiss = menuState::dismiss,
                                                            )
                                                        }
                                                    },
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.more_vert),
                                                        contentDescription = stringResource(R.string.more_options),
                                                    )
                                                }
                                            }
                                            if (!locked && !isListenTogetherGuest) {
                                                IconButton(
                                                    onClick = { },
                                                    modifier = Modifier.draggableHandle(),
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.drag_handle),
                                                        contentDescription = stringResource(R.string.baaja_player_reorder),
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                            .baajaSurface(
                                                ink = queueInk,
                                                color = if (isActive) MaterialTheme.colorScheme.primaryContainer else background,
                                                shape = QueueRowShape,
                                                shadowOffset = if (isActive) 3.dp else 0.dp,
                                                borderWidth = if (isActive) 2.dp else 1.5.dp,
                                            )
                                            .combinedClickable(
                                                onClick = {
                                                    if (inSelectMode) {
                                                        onCheckedChange(window.mediaItem.mediaId !in selection)
                                                    } else if (!isListenTogetherGuest) {
                                                        if (index == currentWindowIndex) {
                                                            if (isCasting) {
                                                                if (castIsPlaying) {
                                                                    castHandler?.pause()
                                                                } else {
                                                                    castHandler?.play()
                                                                }
                                                            } else {
                                                                playerConnection.togglePlayPause()
                                                            }
                                                        } else {
                                                            if (isCasting) {
                                                                val mediaId = window.mediaItem.mediaId
                                                                val navigated = castHandler?.navigateToMediaIfInQueue(mediaId) ?: false
                                                                if (!navigated) {
                                                                    playerConnection.player.seekToDefaultPosition(window.firstPeriodIndex)
                                                                }
                                                            } else {
                                                                playerConnection.player.seekToDefaultPosition(
                                                                    window.firstPeriodIndex,
                                                                )
                                                                playerConnection.player.playWhenReady = true
                                                            }
                                                        }
                                                    }
                                                },
                                                onLongClick = {
                                                    if (!inSelectMode) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        inSelectMode = true
                                                        onCheckedChange(true)
                                                    }
                                                },
                                            ),
                                )
                            }
                        }

                        if (locked) {
                            content()
                        } else {
                            SwipeToDismissBox(
                                state = dismissBoxState,
                                backgroundContent = {},
                            ) {
                                content()
                            }
                        }
                    }
                }

                if (automix.isNotEmpty()) {
                    item(key = "automix_divider") {
                        Text(
                            text = stringResource(R.string.similar_content),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier =
                                Modifier
                                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp)
                                    .animateItem(),
                        )
                    }

                    itemsIndexed(
                        items = automix,
                        key = { _, it -> it.mediaId },
                    ) { index, item ->
                        Row(
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            MediaMetadataListItem(
                                mediaMetadata = item.metadata!!,
                                trailingContent = {
                                    if (!isListenTogetherGuest) {
                                        IconButton(
                                            onClick = {
                                                playerConnection.service.playNextAutomix(
                                                    item,
                                                    index,
                                                )
                                            },
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.playlist_play),
                                                contentDescription = stringResource(R.string.baaja_fix_play_next),
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                playerConnection.service.addToQueueAutomix(
                                                    item,
                                                    index,
                                                )
                                            },
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.queue_music),
                                                contentDescription = stringResource(R.string.baaja_fix_add_to_queue),
                                            )
                                        }
                                    }
                                },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                        .baajaSurface(queueInk.copy(alpha = 0.5f), background, QueueRowShape, shadowOffset = 0.dp, borderWidth = 1.5.dp)
                                        .combinedClickable(
                                            onClick = {
                                                if (!isListenTogetherGuest) {
                                                    val player = playerConnection.player
                                                    val hadQueue = player.mediaItemCount > 0
                                                    playerConnection.service.playNextAutomix(item, index)
                                                    if (hadQueue && player.mediaItemCount > 1) {
                                                        player.seekToNextMediaItem()
                                                        player.play()
                                                    }
                                                }
                                            },
                                            onLongClick = {
                                                menuState.show {
                                                    QueueMenu(
                                                        mediaMetadata = item.metadata!!,
                                                        playerBottomSheetState = playerBottomSheetState,
                                                        onShowDetailsDialog = {
                                                            item.mediaId.let {
                                                                bottomSheetPageState.show {
                                                                    ShowMediaInfo(it)
                                                                }
                                                            }
                                                        },
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        ).animateItem(),
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier =
                Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { }
                    .background(
                        if (pureBlack) {
                            Color.Black
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                    ).windowInsetsPadding(
                        WindowInsets.systemBars
                            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                    ),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .height(ListItemHeight)
                        .padding(start = 20.dp, end = 16.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.baaja_player_up_next),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                    )
                    if (!queueTitle.isNullOrBlank()) {
                        Text(
                            text = queueTitle.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                AnimatedVisibility(
                    visible = !inSelectMode,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it },
                ) {
                    Row {
                        BaajaPlayerPill(
                            text = null,
                            icon = if (locked) R.drawable.lock else R.drawable.lock_open,
                            contentDescription =
                                stringResource(if (locked) R.string.baaja_player_unlock_queue else R.string.baaja_player_lock_queue),
                            onClick = { locked = !locked },
                            active = !locked,
                            colors = baajaColors.onCard(),
                            modifier = Modifier.padding(horizontal = 6.dp).size(44.dp),
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text =
                            pluralStringResource(
                                R.plurals.n_song,
                                queueWindows.size,
                                queueWindows.size,
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )

                    Text(
                        text = makeTimeString(queueLength * 1000L),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            AnimatedVisibility(
                visible = inSelectMode,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                val selectedSongs =
                    remember(selection.toList(), mutableQueueWindows) {
                        mutableQueueWindows
                            .filter { it.mediaItem.mediaId in selection }
                            .mapNotNull { it.mediaItem.metadata }
                    }
                val selectedItems =
                    remember(selection.toList(), mutableQueueWindows) {
                        mutableQueueWindows.filter { it.mediaItem.mediaId in selection }
                    }
                val count = selection.size
                Row(
                    modifier =
                        Modifier
                            .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onExitSelectionMode,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                        )
                    }
                    Text(
                        text = pluralStringResource(R.plurals.n_selected, count, count),
                        modifier = Modifier.weight(1f),
                    )
                    Checkbox(
                        checked = count == mutableQueueWindows.size && count > 0,
                        onCheckedChange = {
                            if (count == mutableQueueWindows.size) {
                                selection.clear()
                            } else {
                                selection.clear()
                                mutableQueueWindows.forEach {
                                    selection.add(it.mediaItem.mediaId)
                                }
                            }
                        },
                    )
                    IconButton(
                        enabled = count > 0,
                        onClick = {
                            menuState.show {
                                SelectionMediaMetadataMenu(
                                    songSelection = selectedSongs,
                                    onDismiss = menuState::dismiss,
                                    clearAction = onExitSelectionMode,
                                    currentItems = selectedItems,
                                )
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null,
                            tint = LocalContentColor.current,
                        )
                    }
                }
            }
            HorizontalDivider(thickness = 2.dp, color = queueInk)
        }

        val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsStateWithLifecycle()

        Box(
            modifier =
                Modifier
                    .background(
                        if (pureBlack) {
                            Color.Black
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                    ).drawBehind {
                        drawLine(queueInk, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 2.dp.toPx())
                    }.fillMaxWidth()
                    .height(
                        ListItemHeight +
                            WindowInsets.systemBars
                                .asPaddingValues()
                                .calculateBottomPadding(),
                    ).align(Alignment.BottomCenter)
                    .clickable {
                        state.collapseSoft()
                    }.windowInsetsPadding(
                        WindowInsets.systemBars
                            .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                    ).padding(12.dp),
        ) {
            BaajaToggleIcon(
                icon = R.drawable.shuffle,
                contentDescription = stringResource(R.string.shuffle),
                active = shuffleModeEnabled,
                enabled = !isListenTogetherGuest,
                colors = baajaColors.onCard(),
                size = 44.dp,
                modifier = Modifier.align(Alignment.CenterStart),
                onClick = {
                    coroutineScope
                        .launch {
                            lazyListState.animateScrollToItem(
                                if (playerConnection.player.shuffleModeEnabled) playerConnection.player.currentMediaItemIndex else 0,
                            )
                        }.invokeOnCompletion {
                            playerConnection.player.shuffleModeEnabled =
                                !playerConnection.player.shuffleModeEnabled
                        }
                },
            )

            Icon(
                painter = painterResource(R.drawable.expand_more),
                contentDescription = stringResource(R.string.baaja_player_collapse_queue),
                modifier = Modifier.align(Alignment.Center),
            )

            BaajaToggleIcon(
                icon =
                    when (repeatMode) {
                        Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                        else -> R.drawable.repeat
                    },
                contentDescription =
                    stringResource(
                        if (repeatMode == Player.REPEAT_MODE_ONE) R.string.baaja_player_repeat_one else R.string.baaja_player_repeat,
                    ),
                active = repeatMode != Player.REPEAT_MODE_OFF,
                enabled = !isListenTogetherGuest,
                colors = baajaColors.onCard(),
                size = 44.dp,
                modifier = Modifier.align(Alignment.CenterEnd),
                onClick = playerConnection.player::toggleRepeatMode,
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .padding(
                        bottom =
                            ListItemHeight +
                                WindowInsets.systemBars
                                    .asPaddingValues()
                                    .calculateBottomPadding(),
                    ).align(Alignment.BottomCenter),
        )
    }
}

private val QueueRowShape = RoundedCornerShape(20.dp)
