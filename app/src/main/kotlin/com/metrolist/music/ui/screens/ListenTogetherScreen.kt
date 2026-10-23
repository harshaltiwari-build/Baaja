/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.metrolist.music.ui.component.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.AppBarHeight
import com.metrolist.music.ui.component.rememberListenTogetherNoticeGate
import com.metrolist.music.constants.ListenTogetherUsernameKey
import com.metrolist.music.listentogether.ConnectionState
import com.metrolist.music.listentogether.JoinRequestPayload
import com.metrolist.music.listentogether.ListenTogetherEvent
import com.metrolist.music.listentogether.SuggestionReceivedPayload
import com.metrolist.music.listentogether.UserInfo
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.theme.BaajaArtwork
import com.metrolist.music.ui.theme.BaajaCard
import com.metrolist.music.ui.theme.BaajaIconButton
import com.metrolist.music.ui.theme.BaajaPillButton
import com.metrolist.music.ui.theme.baajaInk
import com.metrolist.music.ui.theme.baajaSurface
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ListenTogetherScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    val listenTogetherManager = LocalListenTogetherManager.current
    val windowInsets = LocalPlayerAwareWindowInsets.current
    val joiningRoomTemplate = stringResource(R.string.joining_room)

    if (listenTogetherManager == null) return
    val noticeGate = rememberListenTogetherNoticeGate()

    val connectionState by listenTogetherManager.connectionState.collectAsStateWithLifecycle()
    val roomState by listenTogetherManager.roomState.collectAsStateWithLifecycle()
    val userId by listenTogetherManager.userId.collectAsStateWithLifecycle()
    val pendingJoinRequests by listenTogetherManager.pendingJoinRequests.collectAsStateWithLifecycle()
    val pendingSuggestions by listenTogetherManager.pendingSuggestions.collectAsStateWithLifecycle()

    var savedUsername by rememberPreference(ListenTogetherUsernameKey, "")
    var roomCodeInput by rememberSaveable { mutableStateOf("") }
    var usernameInput by rememberSaveable { mutableStateOf(savedUsername) }

    var isCreatingRoom by rememberSaveable { mutableStateOf(false) }
    var isJoiningRoom by rememberSaveable { mutableStateOf(false) }
    var joinErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    var selectedUserForMenu by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedUsername by rememberSaveable { mutableStateOf<String?>(null) }

    val waitingForApprovalText = stringResource(R.string.waiting_for_approval)
    val invalidRoomCodeText = stringResource(R.string.invalid_room_code)
    val joinRequestDeniedText = stringResource(R.string.join_request_denied)

    LaunchedEffect(savedUsername) {
        if (usernameInput.isBlank() && savedUsername.isNotBlank()) {
            usernameInput = savedUsername
        }
    }

    LaunchedEffect(listenTogetherManager) {
        listenTogetherManager.events.collect { event ->
            when (event) {
                is ListenTogetherEvent.JoinRejected -> {
                    val reason = event.reason
                    joinErrorMessage =
                        when {
                            reason.isNullOrBlank() -> joinRequestDeniedText
                            reason.contains("invalid", ignoreCase = true) -> invalidRoomCodeText
                            else -> "$joinRequestDeniedText: $reason"
                        }
                    isJoiningRoom = false
                    isCreatingRoom = false
                }

                is ListenTogetherEvent.JoinApproved -> {
                    isJoiningRoom = false
                    joinErrorMessage = null
                }

                is ListenTogetherEvent.RoomCreated -> {
                    isCreatingRoom = false
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("ListenTogetherRoom", event.roomCode)
                    clipboard.setPrimaryClip(clip)
                }

                else -> {}
            }
        }
    }

    val isInRoom = listenTogetherManager.isInRoom
    val isHost = roomState?.hostId == userId

    // User action menu dialog
    if (selectedUserForMenu != null && selectedUsername != null) {
        UserActionDialog(
            username = selectedUsername ?: "",
            onKick = {
                selectedUserForMenu?.let {
                    listenTogetherManager.kickUser(it, "Removed by host")
                }
                selectedUserForMenu = null
                selectedUsername = null
            },
            onPermanentKick = {
                selectedUserForMenu?.let { userId ->
                    selectedUsername?.let { username ->
                        listenTogetherManager.blockUser(username)
                        listenTogetherManager.kickUser(userId, R.string.user_blocked_by_host.toString())
                    }
                }
                selectedUserForMenu = null
                selectedUsername = null
            },
            onTransferOwnership = {
                selectedUserForMenu?.let {
                    listenTogetherManager.transferHost(it)
                }
                selectedUserForMenu = null
                selectedUsername = null
            },
            onDismiss = {
                selectedUserForMenu = null
                selectedUsername = null
            },
        )
    }

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsStateWithLifecycle()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazyListState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .imePadding(),
        contentPadding =
            PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = windowInsets.asPaddingValues().calculateTopPadding() + 20.dp,
                bottom = windowInsets.asPaddingValues().calculateBottomPadding() + 16.dp + AppBarHeight,
            ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(key = "header") {
            HeaderSection()
        }

        if (isInRoom) {
            roomState?.let { room ->
                item(key = "room") {
                    RoomStatusCard(
                        roomCode = room.roomCode,
                        isHost = isHost,
                        context = context,
                    )
                }

                if (room.hasCurrentTrack() && room.currentTrack.title.isNotBlank()) {
                    item(key = "now_playing") {
                        NowPlayingInRoomCard(
                            title = room.currentTrack.title,
                            artist = room.currentTrack.artist,
                            thumbnail = room.currentTrack.thumbnail,
                            isPlaying = room.isPlaying,
                        )
                    }
                }

                // Connected users
                val connectedUsers = room.usersList.filter { it.isConnected }
                val currentUserIdValue = userId ?: ""
                item(key = "users") {
                    ConnectedUsersSection(
                        users = connectedUsers,
                        isHost = isHost,
                        currentUserId = currentUserIdValue,
                        onUserClick = { clickedUserId, username ->
                            if (isHost && clickedUserId != currentUserIdValue) {
                                selectedUserForMenu = clickedUserId
                                selectedUsername = username
                            }
                        },
                    )
                }

                // Pending join requests (host only)
                if (isHost && pendingJoinRequests.isNotEmpty()) {
                    item(key = "join_requests") {
                        PendingJoinRequestsSection(
                            requests = pendingJoinRequests,
                            onApprove = { listenTogetherManager.approveJoin(it) },
                            onReject = { listenTogetherManager.rejectJoin(it, "Rejected by host") },
                        )
                    }
                }

                // Pending suggestions (host only)
                if (isHost && pendingSuggestions.isNotEmpty()) {
                    item(key = "suggestions") {
                        PendingSuggestionsSection(
                            suggestions = pendingSuggestions,
                            onApprove = { listenTogetherManager.approveSuggestion(it) },
                            onReject = { listenTogetherManager.rejectSuggestion(it, "Rejected by host") },
                        )
                    }
                }

                item(key = "connection") {
                    ConnectionStatusCard(
                        connectionState = connectionState,
                        onConnect = { listenTogetherManager.connect() },
                        onDisconnect = { listenTogetherManager.disconnect() },
                        onReconnect = { listenTogetherManager.forceReconnect() },
                    )
                }

                // Leave room button
                item(key = "leave") {
                    LeaveRoomButton(onClick = { listenTogetherManager.leaveRoom() })
                }
            }
        } else {
            item(key = "hero") {
                TogetherHero(savedUsername = savedUsername)
            }

            // Connection status card
            item(key = "connection") {
                ConnectionStatusCard(
                    connectionState = connectionState,
                    onConnect = { listenTogetherManager.connect() },
                    onDisconnect = { listenTogetherManager.disconnect() },
                    onReconnect = { listenTogetherManager.forceReconnect() },
                )
            }

            if (connectionState == ConnectionState.CONNECTED) {
                item(key = "background_note") {
                    Text(
                        text = stringResource(R.string.listen_together_background_disconnect_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Join/Create room section
            item(key = "join_create") {
                JoinCreateRoomSection(
                    usernameInput = usernameInput,
                    onUsernameChange = { usernameInput = it },
                    roomCodeInput = roomCodeInput,
                    onRoomCodeChange = { roomCodeInput = it },
                    savedUsername = savedUsername,
                    isJoiningRoom = isJoiningRoom,
                    joinErrorMessage = joinErrorMessage,
                    waitingForApprovalText = waitingForApprovalText,
                    bringIntoViewRequester = bringIntoViewRequester,
                    onCreateRoom = { noticeGate {
                        val username = usernameInput.takeIf { it.isNotBlank() } ?: savedUsername
                        val finalUsername = username.trim()
                        if (finalUsername.isNotBlank()) {
                            savedUsername = finalUsername
                            Toast.makeText(context, R.string.creating_room, Toast.LENGTH_SHORT).show()
                            isCreatingRoom = true
                            isJoiningRoom = false
                            joinErrorMessage = null
                            listenTogetherManager.connect()
                            listenTogetherManager.createRoom(finalUsername)
                        } else {
                            Toast.makeText(context, R.string.error_username_empty, Toast.LENGTH_SHORT).show()
                        }
                    } },
                    onJoinRoom = { noticeGate {
                        val username = usernameInput.takeIf { it.isNotBlank() } ?: savedUsername
                        val finalUsername = username.trim()
                        if (finalUsername.isNotBlank()) {
                            savedUsername = finalUsername
                            Toast
                                .makeText(
                                    context,
                                    String.format(joiningRoomTemplate, roomCodeInput),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            isJoiningRoom = true
                            isCreatingRoom = false
                            joinErrorMessage = null
                            listenTogetherManager.connect()
                            listenTogetherManager.joinRoom(roomCodeInput, finalUsername)
                        } else {
                            Toast.makeText(context, R.string.error_username_empty, Toast.LENGTH_SHORT).show()
                        }
                    } },
                    onFieldFocused = {
                        coroutineScope.launch {
                            bringIntoViewRequester.bringIntoView()
                        }
                    },
                )
            }

            item(key = "how") {
                HowItWorksCard()
            }
        }

        // Settings link
        item(key = "settings") {
            SettingsLinkCard(
                onClick = { navController.navigate("settings/listen_together") },
            )
        }
    }
}

private data class FaceColors(val container: Color, val content: Color)

/** Avatar colours from the theme (yellow, teal, pink, lilac) so dark mode stays legible. */
@Composable
private fun faceColors(index: Int): FaceColors {
    val scheme = MaterialTheme.colorScheme
    val palette =
        listOf(
            FaceColors(scheme.secondaryContainer, scheme.onSecondaryContainer),
            FaceColors(scheme.tertiaryContainer, scheme.onTertiaryContainer),
            FaceColors(scheme.secondary.copy(alpha = 0.22f).compositeOver(scheme.surface), scheme.onSurface),
            FaceColors(scheme.primaryContainer, scheme.onPrimaryContainer),
        )
    return palette[Math.floorMod(index, palette.size)]
}

@Composable
private fun HeaderSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.baaja_ai_together_title),
            fontSize = 34.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.baaja_ai_together_tagline),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Decorative lilac card with overlapping friend avatars. */
@Composable
private fun TogetherHero(savedUsername: String) {
    val initials =
        listOf(
            savedUsername.trim().take(1).uppercase().ifBlank { null },
            null,
            null,
        )
    Box(
        Modifier
            .fillMaxWidth()
            .height(180.dp)
            .baajaSurface(baajaInk, MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(28.dp), shadowOffset = 4.dp, borderWidth = 2.5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy((-12).dp)) {
            initials.forEachIndexed { index, initial ->
                val colors = faceColors(index)
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(colors.container)
                        .border(2.5.dp, baajaInk, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (initial != null) {
                        Text(initial, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = colors.content)
                    } else {
                        Icon(painterResource(R.drawable.person), null, tint = colors.content, modifier = Modifier.size(32.dp))
                    }
                }
            }
            Box(
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(2.5.dp, baajaInk, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.add), null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(30.dp))
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    connectionState: ConnectionState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onReconnect: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val dotColor =
        when (connectionState) {
            ConnectionState.CONNECTED -> scheme.primary
            ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> scheme.tertiary
            ConnectionState.ERROR -> scheme.error
            ConnectionState.DISCONNECTED -> scheme.onSurfaceVariant
        }
    BaajaCard(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(22.dp),
        color = if (connectionState == ConnectionState.ERROR) scheme.errorContainer else scheme.surfaceVariant,
        shadowOffset = 0.dp,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                        .border(1.5.dp, baajaInk, CircleShape),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text =
                        when (connectionState) {
                            ConnectionState.CONNECTED -> stringResource(R.string.listen_together_connected)
                            ConnectionState.CONNECTING -> stringResource(R.string.listen_together_connecting)
                            ConnectionState.RECONNECTING -> stringResource(R.string.listen_together_reconnecting)
                            ConnectionState.ERROR -> stringResource(R.string.listen_together_error)
                            ConnectionState.DISCONNECTED -> stringResource(R.string.listen_together_disconnected)
                        },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (connectionState == ConnectionState.ERROR) scheme.onErrorContainer else scheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }

            if (connectionState == ConnectionState.CONNECTING || connectionState == ConnectionState.RECONNECTING) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                    color = scheme.primary,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                if (connectionState == ConnectionState.DISCONNECTED || connectionState == ConnectionState.ERROR) {
                    BaajaPillButton(
                        text = stringResource(R.string.connect),
                        onClick = onConnect,
                        icon = R.drawable.link,
                        height = 46.dp,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    BaajaPillButton(
                        text = stringResource(R.string.disconnect),
                        onClick = onDisconnect,
                        primary = false,
                        height = 46.dp,
                        modifier = Modifier.weight(1f),
                    )
                    BaajaPillButton(
                        text = stringResource(R.string.baaja_ai_together_reconnect),
                        onClick = onReconnect,
                        primary = false,
                        icon = R.drawable.sync,
                        height = 46.dp,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private fun copyToClipboard(
    context: Context,
    label: String,
    text: String,
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    clipboard.setPrimaryClip(android.content.ClipData.newPlainText(label, text))
    Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
}

@Composable
private fun RoomStatusCard(
    roomCode: String,
    isHost: Boolean,
    context: Context,
) {
    val content = MaterialTheme.colorScheme.onSecondaryContainer
    Column(
        Modifier
            .fillMaxWidth()
            .baajaSurface(baajaInk, MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(28.dp), shadowOffset = 4.dp, borderWidth = 2.5.dp)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.room_code).uppercase(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = content,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = roomCode,
                fontSize = 40.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp,
                color = content,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            BaajaIconButton(
                icon = R.drawable.content_copy,
                contentDescription = stringResource(R.string.copy_code),
                onClick = { copyToClipboard(context, "Room Code", roomCode) },
                size = 48.dp,
            )
        }
        Text(
            text =
                stringResource(R.string.baaja_ai_together_share_hint) + " " +
                    if (isHost) {
                        stringResource(R.string.listen_together_you_are_host)
                    } else {
                        stringResource(R.string.listen_together_you_are_guest)
                    },
            fontSize = 15.sp,
            color = content,
        )

        Text(
            text = stringResource(R.string.baaja_fix_community_server),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = content.copy(alpha = 0.75f),
        )

        if (isHost) {
            val inviteText = stringResource(R.string.baaja_fix_share_room, roomCode)
            BaajaPillButton(
                text = stringResource(R.string.baaja_fix_copy_invite),
                onClick = { copyToClipboard(context, "Listen Together Invite", inviteText) },
                primary = false,
                icon = R.drawable.link,
                height = 46.dp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun NowPlayingInRoomCard(
    title: String,
    artist: String,
    thumbnail: String,
    isPlaying: Boolean,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .baajaSurface(baajaInk, MaterialTheme.colorScheme.surface, RoundedCornerShape(22.dp), shadowOffset = 3.dp)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BaajaArtwork(thumbnail.ifBlank { null }, Modifier.size(54.dp), shape = RoundedCornerShape(14.dp), shadowOffset = 0.dp)
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.baaja_ai_together_playing).uppercase(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (artist.isNotBlank()) {
                Text(artist, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Icon(
            painterResource(if (isPlaying) R.drawable.equalizer else R.drawable.pause),
            contentDescription = stringResource(if (isPlaying) R.string.play else R.string.pause),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun ConnectedUsersSection(
    users: List<UserInfo>,
    isHost: Boolean,
    currentUserId: String,
    onUserClick: (String, String) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.baaja_ai_together_in_room, users.size),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        users.forEachIndexed { index, user ->
            UserRow(
                user = user,
                colorIndex = index,
                isCurrentUser = user.userId == currentUserId,
                isClickable = isHost && user.userId != currentUserId,
                onClick = { onUserClick(user.userId, user.username) },
            )
        }
    }
}

@Composable
private fun UserRow(
    user: UserInfo,
    colorIndex: Int,
    isCurrentUser: Boolean,
    isClickable: Boolean,
    onClick: () -> Unit,
) {
    val colors = faceColors(colorIndex)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = isClickable, role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(colors.container)
                .border(2.dp, baajaInk, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = user.username.take(1).uppercase(),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.content,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = user.username,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (isCurrentUser) {
                Text(
                    text = stringResource(R.string.you_label),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (user.isHost) {
            Row(
                Modifier
                    .heightIn(min = 28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(1.5.dp, baajaInk, CircleShape)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(painterResource(R.drawable.crown), null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                Text(
                    text = stringResource(R.string.host_label),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
        if (isClickable) {
            Icon(
                painterResource(R.drawable.more_vert),
                contentDescription = stringResource(R.string.manage_user),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun PendingRow(
    leading: @Composable () -> Unit,
    title: String,
    subtitle: String?,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        leading()
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        BaajaIconButton(
            icon = R.drawable.check,
            contentDescription = stringResource(R.string.approve),
            onClick = onApprove,
            filled = true,
            size = 44.dp,
        )
        BaajaIconButton(
            icon = R.drawable.close,
            contentDescription = stringResource(R.string.reject),
            onClick = onReject,
            size = 44.dp,
        )
    }
}

@Composable
private fun PendingJoinRequestsSection(
    requests: List<JoinRequestPayload>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
) {
    BaajaCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), shadowOffset = 3.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.listen_together_join_requests),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            requests.forEachIndexed { index, request ->
                val colors = faceColors(index + 1)
                PendingRow(
                    leading = {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(colors.container)
                                .border(2.dp, baajaInk, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = request.username.take(1).uppercase(),
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.content,
                            )
                        }
                    },
                    title = request.username,
                    subtitle = null,
                    onApprove = { onApprove(request.userId) },
                    onReject = { onReject(request.userId) },
                )
            }
        }
    }
}

@Composable
private fun PendingSuggestionsSection(
    suggestions: List<SuggestionReceivedPayload>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
) {
    BaajaCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), shadowOffset = 3.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.pending_suggestions),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            suggestions.forEach { suggestion ->
                PendingRow(
                    leading = {
                        BaajaArtwork(
                            suggestion.trackInfo.thumbnail.ifBlank { null },
                            Modifier.size(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            shadowOffset = 0.dp,
                        )
                    },
                    title = suggestion.trackInfo.title,
                    subtitle = suggestion.fromUsername,
                    onApprove = { onApprove(suggestion.suggestionId) },
                    onReject = { onReject(suggestion.suggestionId) },
                )
            }
        }
    }
}

/** Outlined pill text field with Baaja's hard shadow. */
@Composable
private fun BaajaInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: Int? = null,
    fontSize: TextUnit = 17.sp,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    height: Dp = 56.dp,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = onSurface, fontSize = fontSize, fontWeight = FontWeight.SemiBold, letterSpacing = letterSpacing),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(capitalization = capitalization),
        modifier = modifier.heightIn(min = height),
        decorationBox = { inner ->
            Row(
                Modifier
                    .heightIn(min = height)
                    .baajaSurface(baajaInk, MaterialTheme.colorScheme.surface, CircleShape, shadowOffset = 3.dp)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (leadingIcon != null) {
                    Icon(painterResource(leadingIcon), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    inner()
                }
                if (value.isNotEmpty()) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(role = Role.Button) { onValueChange("") },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(R.drawable.close), stringResource(R.string.clear), modifier = Modifier.size(18.dp))
                    }
                }
            }
        },
    )
}

@Composable
private fun JoinCreateRoomSection(
    usernameInput: String,
    onUsernameChange: (String) -> Unit,
    roomCodeInput: String,
    onRoomCodeChange: (String) -> Unit,
    savedUsername: String,
    isJoiningRoom: Boolean,
    joinErrorMessage: String?,
    waitingForApprovalText: String,
    bringIntoViewRequester: BringIntoViewRequester,
    onCreateRoom: () -> Unit,
    onJoinRoom: () -> Unit,
    onFieldFocused: () -> Unit = {},
) {
    val hasUsername = usernameInput.trim().isNotBlank() || savedUsername.isNotBlank()
    val hasRoomCode = roomCodeInput.length == 8

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // Username input
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.baaja_ai_together_your_name), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            BaajaInputField(
                value = usernameInput,
                onValueChange = onUsernameChange,
                placeholder = stringResource(R.string.enter_username),
                leadingIcon = R.drawable.person,
                capitalization = KeyboardCapitalization.Words,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) onFieldFocused() },
            )
        }

        // Create Room button
        BaajaPillButton(
            text = stringResource(R.string.baaja_ai_together_start),
            onClick = onCreateRoom,
            enabled = hasUsername,
            icon = R.drawable.add,
            height = 60.dp,
            modifier = Modifier.fillMaxWidth(),
        )

        // Room code input + Join
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.baaja_ai_together_have_code), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                BaajaInputField(
                    value = roomCodeInput,
                    onValueChange = { if (it.length <= 8) onRoomCodeChange(it.uppercase()) },
                    placeholder = stringResource(R.string.enter_room_code),
                    fontSize = 18.sp,
                    letterSpacing = 1.5.sp,
                    capitalization = KeyboardCapitalization.Characters,
                    modifier =
                        Modifier
                            .weight(1f)
                            .bringIntoViewRequester(bringIntoViewRequester)
                            .onFocusChanged { if (it.isFocused) onFieldFocused() },
                )
                val joinEnabled = hasUsername && hasRoomCode
                val ink = baajaInk
                Box(
                    Modifier
                        .heightIn(min = 56.dp)
                        .clip(CircleShape)
                        .background(if (joinEnabled) ink else ink.copy(alpha = 0.35f))
                        .clickable(enabled = joinEnabled, role = Role.Button, onClick = onJoinRoom)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.join_room),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.surface,
                    )
                }
            }
        }

        // Waiting for approval indicator
        AnimatedVisibility(
            visible = isJoiningRoom,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .baajaSurface(baajaInk, MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(18.dp), shadowOffset = 0.dp)
                        .padding(16.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = waitingForApprovalText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Error message
        AnimatedVisibility(
            visible = joinErrorMessage != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .baajaSurface(baajaInk, MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(18.dp), shadowOffset = 0.dp)
                        .padding(16.dp),
            ) {
                Icon(
                    painterResource(R.drawable.error),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = joinErrorMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun HowItWorksCard() {
    val steps =
        listOf(
            stringResource(R.string.baaja_ai_together_step1),
            stringResource(R.string.baaja_ai_together_step2),
            stringResource(R.string.baaja_ai_together_step3),
        )
    BaajaCard(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowOffset = 0.dp,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.baaja_ai_together_how), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            steps.forEachIndexed { index, step ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .border(2.dp, baajaInk, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            (index + 1).toString(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    Text(step, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun LeaveRoomButton(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .baajaSurface(baajaInk, MaterialTheme.colorScheme.surface, CircleShape, shadowOffset = 3.dp)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.logout), null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
        Text(stringResource(R.string.leave_room), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun SettingsLinkCard(onClick: () -> Unit) {
    BaajaCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        shadowOffset = 0.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.settings),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.listen_together_settings_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                painter = painterResource(R.drawable.arrow_forward),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun UserActionDialog(
    username: String,
    onKick: () -> Unit,
    onPermanentKick: () -> Unit,
    onTransferOwnership: () -> Unit,
    onDismiss: () -> Unit,
) {
    DefaultDialog(
        onDismiss = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.group),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.manage_user),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = username,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        buttons = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Kick button
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onKick),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.kick_user),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text = stringResource(R.string.kick_user_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Permanently kick button
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onPermanentKick),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.permanently_kick_user),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.permanently_kick_user_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Transfer ownership button
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onTransferOwnership),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.crown),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.transfer_ownership),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(R.string.transfer_ownership_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
