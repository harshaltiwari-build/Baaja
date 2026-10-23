/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/** Set once the first-run welcome screen has been dismissed (as guest or by choosing to sign in). */
val BaajaHasSeenWelcomeKey = booleanPreferencesKey("baajaHasSeenWelcome")

/** Local profile, stored on this device only. Blank name/bio means "not set". */
val BaajaProfileNameKey = stringPreferencesKey("baajaProfileName")
val BaajaProfileBioKey = stringPreferencesKey("baajaProfileBio")

/** Bumped whenever the avatar file is replaced or removed; 0 means no custom photo. */
val BaajaProfileAvatarVersionKey = longPreferencesKey("baajaProfileAvatarVersion")
