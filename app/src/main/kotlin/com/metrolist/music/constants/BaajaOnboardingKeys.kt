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

/** Music languages picked during first run or in Settings, as comma-separated codes (see BaajaLanguages). */
val BaajaMusicLanguagesKey = stringPreferencesKey("baajaMusicLanguages")

/** Artists the user picked, one per line: id, name and thumbnail separated by tabs. */
val BaajaPickedArtistsKey = stringPreferencesKey("baajaPickedArtists")

/** Set when a Google sign-in just finished, so the next start shows the "Welcome back" screen once. */
val BaajaJustSignedInKey = booleanPreferencesKey("baajaJustSignedIn")

/** Profile photo of the signed-in YouTube Music account. */
val BaajaAccountImageKey = stringPreferencesKey("baajaAccountImage")
