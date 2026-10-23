/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

/**
 * Preferred audio output, stored as a stable device key (see `AudioOutputDevice.key`).
 * Missing or blank means "Automatic" (let Android route).
 */
val AudioOutputDeviceKey = stringPreferencesKey("baajaAudioOutputDevice")

/** Process audio in 32-bit float and hand float PCM to the system mixer (default off). */
val FloatOutputKey = booleanPreferencesKey("baajaFloatOutput")

/** DJ-style crossfades: transition length and start point picked from the audio itself. */
val AutomixTransitionsKey = booleanPreferencesKey("baajaAutomixTransitions")

/** Smart downloads: how many most-played songs to keep downloaded. 0 = off. */
val SmartDownloadsCountKey = intPreferencesKey("baajaSmartDownloadsCount")

/** Smart downloads only run on unmetered networks (default on). */
val SmartDownloadsWifiOnlyKey = booleanPreferencesKey("baajaSmartDownloadsWifiOnly")

/** Song ids that Smart downloads added itself. Only these may ever be removed by the feature. */
val SmartDownloadsTrackedKey = stringSetPreferencesKey("baajaSmartDownloadsTracked")

/** Song ids the user removed after Smart downloads added them; never re-added automatically. */
val SmartDownloadsDismissedKey = stringSetPreferencesKey("baajaSmartDownloadsDismissed")

/** Epoch millis of the last completed Smart downloads sync. */
val SmartDownloadsLastRunKey = longPreferencesKey("baajaSmartDownloadsLastRun")
