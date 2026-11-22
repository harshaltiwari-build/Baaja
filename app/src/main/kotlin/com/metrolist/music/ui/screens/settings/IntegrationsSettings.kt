package com.metrolist.music.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.LastFmScrobbleEnabledKey
import com.metrolist.music.constants.LastFmSessionKey
import com.metrolist.music.constants.LastFmUsernameKey
import com.metrolist.music.constants.ListenBrainzEnabledKey
import com.metrolist.music.constants.ListenBrainzTokenKey
import com.metrolist.music.constants.ListenBrainzUsernameKey
import com.metrolist.music.constants.ScrobbleNowPlayingKey
import com.metrolist.music.scrobble.LastFm
import com.metrolist.music.scrobble.ListenBrainz
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.component.TextButton
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.utils.safeDataStoreEdit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrationsSettings(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val lastFmUser by rememberPreference(LastFmUsernameKey, "")
    val lastFmSession by rememberPreference(LastFmSessionKey, "")
    val (lastFmEnabled, onLastFmEnabledChange) = rememberPreference(LastFmScrobbleEnabledKey, true)
    val listenBrainzUser by rememberPreference(ListenBrainzUsernameKey, "")
    val listenBrainzToken by rememberPreference(ListenBrainzTokenKey, "")
    val (listenBrainzEnabled, onListenBrainzEnabledChange) = rememberPreference(ListenBrainzEnabledKey, true)
    val (nowPlaying, onNowPlayingChange) = rememberPreference(ScrobbleNowPlayingKey, true)

    var showLastFmLogin by remember { mutableStateOf(false) }
    var showListenBrainzLogin by remember { mutableStateOf(false) }

    if (showLastFmLogin) {
        CredentialsDialog(
            title = stringResource(R.string.baaja_lastfm_sign_in),
            firstLabel = stringResource(R.string.baaja_lastfm_username),
            secondLabel = stringResource(R.string.baaja_lastfm_password),
            onDismiss = { showLastFmLogin = false },
            onSubmit = { username, password, done ->
                scope.launch {
                    val result =
                        withContext(Dispatchers.IO) {
                            runCatching { LastFm.getMobileSession(username.trim(), password) }
                        }
                    result
                        .onSuccess { session ->
                            context.safeDataStoreEdit {
                                it[LastFmUsernameKey] = session.username
                                it[LastFmSessionKey] = session.key
                            }
                            showLastFmLogin = false
                        }.onFailure {
                            Toast.makeText(context, R.string.baaja_scrobble_sign_in_failed, Toast.LENGTH_LONG).show()
                        }
                    done()
                }
            },
        )
    }

    if (showListenBrainzLogin) {
        CredentialsDialog(
            title = stringResource(R.string.baaja_listenbrainz_connect),
            firstLabel = null,
            secondLabel = stringResource(R.string.baaja_listenbrainz_token),
            description = stringResource(R.string.baaja_listenbrainz_token_hint),
            onDismiss = { showListenBrainzLogin = false },
            onSubmit = { _, token, done ->
                scope.launch {
                    val result =
                        withContext(Dispatchers.IO) { runCatching { ListenBrainz.validateToken(token.trim()) } }
                    result
                        .onSuccess { name ->
                            context.safeDataStoreEdit {
                                it[ListenBrainzUsernameKey] = name
                                it[ListenBrainzTokenKey] = token.trim()
                            }
                            showListenBrainzLogin = false
                        }.onFailure {
                            Toast.makeText(context, R.string.baaja_scrobble_sign_in_failed, Toast.LENGTH_LONG).show()
                        }
                    done()
                }
            },
        )
    }

    Column(
        Modifier
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            ).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))

        val lastFmConnected = lastFmSession.isNotBlank()
        Material3SettingsGroup(
            title = stringResource(R.string.baaja_lastfm),
            items =
                buildList {
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.account),
                            title = {
                                Text(
                                    if (lastFmConnected) {
                                        stringResource(R.string.baaja_scrobble_signed_in_as, lastFmUser)
                                    } else {
                                        stringResource(R.string.baaja_lastfm_sign_in)
                                    },
                                )
                            },
                            description = {
                                Text(
                                    when {
                                        !LastFm.isConfigured -> stringResource(R.string.baaja_lastfm_not_configured)
                                        lastFmConnected -> stringResource(R.string.baaja_scrobble_tap_to_sign_out)
                                        else -> stringResource(R.string.baaja_lastfm_desc)
                                    },
                                )
                            },
                            enabled = LastFm.isConfigured || lastFmConnected,
                            onClick = {
                                if (lastFmConnected) {
                                    scope.launch {
                                        context.safeDataStoreEdit {
                                            it.remove(LastFmSessionKey)
                                            it.remove(LastFmUsernameKey)
                                        }
                                    }
                                } else {
                                    showLastFmLogin = true
                                }
                            },
                        ),
                    )
                    if (lastFmConnected) {
                        add(switchItem(R.drawable.history, R.string.baaja_scrobble_enabled, lastFmEnabled, onLastFmEnabledChange))
                    }
                },
        )

        Spacer(Modifier.height(16.dp))

        val listenBrainzConnected = listenBrainzToken.isNotBlank()
        Material3SettingsGroup(
            title = stringResource(R.string.baaja_listenbrainz),
            items =
                buildList {
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.key),
                            title = {
                                Text(
                                    if (listenBrainzConnected) {
                                        stringResource(R.string.baaja_scrobble_signed_in_as, listenBrainzUser)
                                    } else {
                                        stringResource(R.string.baaja_listenbrainz_connect)
                                    },
                                )
                            },
                            description = {
                                Text(
                                    if (listenBrainzConnected) {
                                        stringResource(R.string.baaja_scrobble_tap_to_sign_out)
                                    } else {
                                        stringResource(R.string.baaja_listenbrainz_desc)
                                    },
                                )
                            },
                            onClick = {
                                if (listenBrainzConnected) {
                                    scope.launch {
                                        context.safeDataStoreEdit {
                                            it.remove(ListenBrainzTokenKey)
                                            it.remove(ListenBrainzUsernameKey)
                                        }
                                    }
                                } else {
                                    showListenBrainzLogin = true
                                }
                            },
                        ),
                    )
                    if (listenBrainzConnected) {
                        add(
                            switchItem(
                                R.drawable.history,
                                R.string.baaja_scrobble_enabled,
                                listenBrainzEnabled,
                                onListenBrainzEnabledChange,
                            ),
                        )
                    }
                },
        )

        if (lastFmConnected || listenBrainzConnected) {
            Spacer(Modifier.height(16.dp))
            Material3SettingsGroup(
                title = stringResource(R.string.baaja_scrobble_options),
                items = listOf(switchItem(R.drawable.music_note, R.string.baaja_scrobble_now_playing, nowPlaying, onNowPlayingChange)),
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.baaja_scrobble_rules),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Spacer(Modifier.height(24.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.baaja_integrations)) },
        navigationIcon = {
            IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
            }
        },
    )
}

@Composable
private fun switchItem(
    icon: Int,
    title: Int,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) = Material3SettingsItem(
    icon = painterResource(icon),
    title = { Text(stringResource(title)) },
    trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
    onClick = { onChange(!checked) },
)

@Composable
private fun CredentialsDialog(
    title: String,
    firstLabel: String?,
    secondLabel: String,
    description: String? = null,
    onDismiss: () -> Unit,
    onSubmit: (first: String, second: String, done: () -> Unit) -> Unit,
) {
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val canSubmit = !busy && second.isNotBlank() && (firstLabel == null || first.isNotBlank())

    DefaultDialog(
        onDismiss = { if (!busy) onDismiss() },
        title = { Text(title) },
        buttons = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text(stringResource(android.R.string.cancel))
            }
            TextButton(
                enabled = canSubmit,
                onClick = {
                    busy = true
                    onSubmit(first, second) { busy = false }
                },
            ) {
                Text(stringResource(R.string.baaja_scrobble_connect))
            }
        },
    ) {
        description?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 12.dp))
        }
        if (firstLabel != null) {
            OutlinedTextField(
                value = first,
                onValueChange = { first = it },
                label = { Text(firstLabel) },
                singleLine = true,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
        }
        OutlinedTextField(
            value = second,
            onValueChange = { second = it },
            label = { Text(secondLabel) },
            singleLine = true,
            enabled = !busy,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
