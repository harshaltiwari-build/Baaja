package com.metrolist.music.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.metrolist.music.R
import com.metrolist.music.constants.ListenTogetherNoticeAcceptedKey
import com.metrolist.music.utils.rememberPreference

/** One-time notice shown before the first Listen Together room is created or joined. */
@Composable
fun ListenTogetherNoticeDialog(
    onContinue: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.baaja_fix2_lt_notice_title)) },
        text = { Text(stringResource(R.string.baaja_fix2_lt_notice_body)) },
        confirmButton = {
            TextButton(onClick = onContinue) { Text(stringResource(R.string.baaja_fix2_continue)) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

/**
 * Returns a gate: call `gate { createRoom() }` and the action runs immediately once the notice was
 * accepted, otherwise after the user taps Continue in the dialog (remembered in DataStore).
 */
@Composable
fun rememberListenTogetherNoticeGate(): (action: () -> Unit) -> Unit {
    var accepted by rememberPreference(ListenTogetherNoticeAcceptedKey, defaultValue = false)
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }

    pending?.let { action ->
        ListenTogetherNoticeDialog(
            onContinue = {
                accepted = true
                pending = null
                action()
            },
            onCancel = { pending = null },
        )
    }

    return { action -> if (accepted) action() else pending = action }
}
