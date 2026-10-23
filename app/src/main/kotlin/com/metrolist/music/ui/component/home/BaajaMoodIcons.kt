/**
 * Baaja Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component.home

import androidx.annotation.DrawableRes
import com.metrolist.music.R

/** Picks the Baaja sticker for a YouTube mood/genre category from its title (case-insensitive). */
@DrawableRes
fun baajaMoodIconFor(title: String): Int {
    val t = title.lowercase()
    return when {
        "chill" in t || "relax" in t || "sleep" in t || "calm" in t -> R.drawable.baaja_mood_chill
        "commute" in t || "travel" in t || "drive" in t || "road" in t -> R.drawable.baaja_mood_commute
        "energi" in t || "workout" in t || "work out" in t || "gym" in t || "run" in t -> R.drawable.baaja_mood_energize
        "feel good" in t || "feel-good" in t || "happy" in t || "good vibes" in t -> R.drawable.baaja_mood_feelgood
        "focus" in t || "study" in t || "concentrat" in t -> R.drawable.baaja_mood_focus
        "gaming" in t || "game" in t -> R.drawable.baaja_mood_gaming
        "party" in t || "dance" in t || "celebrat" in t -> R.drawable.baaja_mood_party
        "romanc" in t || "love" in t -> R.drawable.baaja_mood_romance
        "sad" in t || "blue" in t || "heartbreak" in t || "melanchol" in t -> R.drawable.baaja_mood_sad
        else -> R.drawable.baaja_mood_music
    }
}
