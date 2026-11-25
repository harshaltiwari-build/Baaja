/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.deviceconnect.DeviceConnectClient
import com.metrolist.music.deviceconnect.DeviceConnectCodeKey
import com.metrolist.music.deviceconnect.DeviceConnectEnabledKey
import com.metrolist.music.deviceconnect.DeviceConnectProtocol
import com.metrolist.music.deviceconnect.RemoteDevice
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.theme.BaajaArtwork
import com.metrolist.music.ui.theme.BaajaCard
import com.metrolist.music.ui.theme.BaajaPillButton
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.delay

/** Lets other phones control this one, and controls another Baaja phone on the same Wi-Fi. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceConnectScreen(navController: NavController) {
    val context = LocalContext.current
    val client = remember { DeviceConnectClient(context) }
    DisposableEffect(client) {
        client.startDiscovery()
        onDispose { client.release() }
    }
    val (enabled, onEnabledChange) = rememberPreference(DeviceConnectEnabledKey, defaultValue = false)
    val (code, onCodeChange) = rememberPreference(DeviceConnectCodeKey, defaultValue = "")
    LaunchedEffect(enabled, code) { if (enabled && code.isEmpty()) onCodeChange(DeviceConnectProtocol.newCode()) }

    val devices by client.devices.collectAsStateWithLifecycle()
    val connection by client.connection.collectAsStateWithLifecycle()
    val playback by client.playback.collectAsStateWithLifecycle()
    var pairing by remember { mutableStateOf<RemoteDevice?>(null) }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))

        // This phone
        BaajaCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.baaja_dc_allow), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(R.string.baaja_dc_allow_desc),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = enabled, onCheckedChange = onEnabledChange)
                }
                if (enabled && code.isNotEmpty()) {
                    Text(stringResource(R.string.baaja_dc_code_label), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            code.chunked(3).joinToString(" "),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 4.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            stringResource(R.string.baaja_dc_new_code),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onCodeChange(DeviceConnectProtocol.newCode()) }.padding(8.dp),
                        )
                    }
                }
            }
        }

        val connected = connection as? DeviceConnectClient.Connection.Connected
        if (connected != null) {
            RemoteControlCard(client, connected.device.name, playback)
        } else {
            Text(stringResource(R.string.baaja_dc_other_devices), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            when (connection) {
                DeviceConnectClient.Connection.WrongCode ->
                    Text(stringResource(R.string.baaja_dc_wrong_code), color = MaterialTheme.colorScheme.error)
                DeviceConnectClient.Connection.Failed ->
                    Text(stringResource(R.string.baaja_dc_failed), color = MaterialTheme.colorScheme.error)
                is DeviceConnectClient.Connection.Connecting ->
                    Text(stringResource(R.string.baaja_dc_connecting), color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> Unit
            }
            if (devices.isEmpty()) {
                Text(stringResource(R.string.baaja_dc_searching), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            devices.forEach { device ->
                BaajaCard(modifier = Modifier.fillMaxWidth(), onClick = { pairing = device }) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.devices), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(device.name, fontSize = 17.sp, modifier = Modifier.weight(1f).padding(start = 14.dp))
                        Icon(painterResource(R.drawable.navigate_next), contentDescription = null)
                    }
                }
            }
            pairing?.let { device ->
                PairingCard(
                    device = device,
                    onConnect = { entered ->
                        client.connect(device, entered)
                        pairing = null
                    },
                    onCancel = { pairing = null },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.baaja_dc_title)) },
        navigationIcon = {
            IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
            }
        },
    )
}

@Composable
private fun PairingCard(
    device: RemoteDevice,
    onConnect: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var entered by remember { mutableStateOf("") }
    BaajaCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.baaja_dc_enter_code, device.name), fontSize = 16.sp)
            OutlinedTextField(
                value = entered,
                onValueChange = { value -> entered = value.filter(Char::isDigit).take(6) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BaajaPillButton(text = stringResource(android.R.string.cancel), onClick = onCancel, primary = false, height = 46.dp, modifier = Modifier.weight(1f))
                BaajaPillButton(
                    text = stringResource(R.string.baaja_dc_connect),
                    onClick = { onConnect(entered) },
                    enabled = entered.length == 6,
                    height = 46.dp,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun RemoteControlCard(
    client: DeviceConnectClient,
    deviceName: String,
    playback: com.metrolist.music.deviceconnect.RemotePlayback?,
) {
    val playerConnection = LocalPlayerConnection.current
    var position by remember { mutableLongStateOf(0L) }
    var seeking by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(playback) {
        while (true) {
            position = playback?.currentPosition() ?: 0
            delay(500)
        }
    }
    BaajaCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.baaja_dc_controlling, deviceName), fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            if (playback == null || playback.mediaId.isEmpty()) {
                Text(stringResource(R.string.baaja_dc_nothing_playing), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    BaajaArtwork(playback.thumbnail, Modifier.size(64.dp), shape = RoundedCornerShape(14.dp), shadowOffset = 0.dp)
                    Column(Modifier.weight(1f)) {
                        Text(playback.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(playback.artist, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Column {
                    Slider(
                        value = seeking ?: position.toFloat(),
                        onValueChange = { seeking = it },
                        onValueChangeFinished = {
                            seeking?.let { client.seekTo(it.toLong()) }
                            seeking = null
                        },
                        valueRange = 0f..playback.durationMs.coerceAtLeast(1).toFloat(),
                    )
                    Row {
                        Text(makeTimeString(seeking?.toLong() ?: position), fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Text(makeTimeString(playback.durationMs), fontSize = 12.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    BaajaPillButton(text = "", onClick = client::previous, primary = false, icon = R.drawable.skip_previous, height = 50.dp, modifier = Modifier.weight(1f))
                    BaajaPillButton(
                        text = "",
                        onClick = { if (playback.playing) client.pause() else client.play() },
                        icon = if (playback.playing) R.drawable.pause else R.drawable.play,
                        height = 50.dp,
                        modifier = Modifier.weight(1.3f),
                    )
                    BaajaPillButton(text = "", onClick = client::next, primary = false, icon = R.drawable.skip_next, height = 50.dp, modifier = Modifier.weight(1f))
                }
                // Take the song over: it carries on here from the same spot, and stops on the other phone.
                if (playerConnection != null) {
                    BaajaPillButton(
                        text = stringResource(R.string.baaja_dc_play_here),
                        onClick = {
                            val startAt = playback.currentPosition()
                            client.pause()
                            playerConnection.playQueue(YouTubeQueue(WatchEndpoint(videoId = playback.mediaId)))
                            playerConnection.service.seekWhenReady(playback.mediaId, startAt)
                        },
                        primary = false,
                        icon = R.drawable.devices,
                        height = 46.dp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            BaajaPillButton(text = stringResource(R.string.baaja_dc_disconnect), onClick = client::disconnect, primary = false, height = 46.dp, modifier = Modifier.fillMaxWidth())
        }
    }
}
