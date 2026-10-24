/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.constants

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.safeDataStoreEdit
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** One switch that replaces auto load more, auto radio queue and autoplay. */
val KeepPlayingSimilarKey = booleanPreferencesKey("baajaKeepPlayingSimilar")

/**
 * Reads [KeepPlayingSimilarKey]. Until it has been written, falls back to the three switches it
 * replaced: off only when all of them were explicitly turned off, on otherwise.
 */
fun Preferences.keepPlayingSimilar(): Boolean =
    this[KeepPlayingSimilarKey]
        ?: !(this[AutoLoadMoreKey] == false && this[AutoRadioQueueKey] == false && this[AutoplayKey] == false)

/** Live value of [keepPlayingSimilar] for composables. */
@Composable
fun rememberKeepPlayingSimilar(): State<Boolean> {
    val context = LocalContext.current
    return remember { context.dataStore.data.map { it.keepPlayingSimilar() }.distinctUntilChanged() }
        .collectAsState(initial = true)
}

/** Writes the new switch, and mirrors the old radio key that screens outside this clean-up still read. */
@Composable
fun rememberSetKeepPlayingSimilar(): (Boolean) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return { enabled ->
        scope.launch {
            context.safeDataStoreEdit {
                it[KeepPlayingSimilarKey] = enabled
                it[AutoRadioQueueKey] = enabled
            }
        }
    }
}

/** How songs hand over to each other. Stored in [CrossfadeEnabledKey] and [AutomixTransitionsKey]. */
enum class TransitionStyle { OFF, CROSSFADE, AUTOMIX }

fun Preferences.transitionStyle(): TransitionStyle =
    when {
        this[CrossfadeEnabledKey] != true -> TransitionStyle.OFF
        this[AutomixTransitionsKey] == true -> TransitionStyle.AUTOMIX
        else -> TransitionStyle.CROSSFADE
    }

@Composable
fun rememberTransitionStyle(): State<TransitionStyle> {
    val context = LocalContext.current
    return remember { context.dataStore.data.map { it.transitionStyle() }.distinctUntilChanged() }
        .collectAsState(initial = TransitionStyle.OFF)
}

@Composable
fun rememberSetTransitionStyle(): (TransitionStyle) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return { style ->
        scope.launch {
            context.safeDataStoreEdit {
                it[CrossfadeEnabledKey] = style != TransitionStyle.OFF
                it[AutomixTransitionsKey] = style == TransitionStyle.AUTOMIX
            }
        }
    }
}
