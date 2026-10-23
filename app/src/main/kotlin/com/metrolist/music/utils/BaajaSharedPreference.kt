/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.ConcurrentHashMap

private val sharedScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
private val sharedBooleans = ConcurrentHashMap<Preferences.Key<Boolean>, StateFlow<Boolean>>()

private fun sharedBooleanFlow(
    context: Context,
    key: Preferences.Key<Boolean>,
    default: Boolean,
): StateFlow<Boolean> =
    sharedBooleans.getOrPut(key) {
        context.dataStore.data
            .map { it[key] ?: default }
            .distinctUntilChanged()
            .stateIn(sharedScope, SharingStarted.Eagerly, default)
    }

/**
 * Read-only boolean preference backed by ONE app-wide flow per key. Use inside list rows, where
 * [rememberPreference] would otherwise open a DataStore collection (and a coroutine scope) per row.
 */
@Composable
fun rememberSharedBooleanPreference(
    key: Preferences.Key<Boolean>,
    defaultValue: Boolean,
): State<Boolean> {
    val appContext = LocalContext.current.applicationContext
    val flow = remember(key) { sharedBooleanFlow(appContext, key, defaultValue) }
    return flow.collectAsStateWithLifecycle()
}
