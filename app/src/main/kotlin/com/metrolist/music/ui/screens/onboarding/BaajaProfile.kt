/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.onboarding

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.metrolist.music.constants.BaajaProfileAvatarVersionKey
import com.metrolist.music.constants.BaajaProfileBioKey
import com.metrolist.music.constants.BaajaProfileNameKey
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * What Baaja shows for the user. Edited values win, then the YouTube account, then a default.
 * @property name display name to show (never blank unless [fallbackName] was blank too)
 * @property bio optional tagline, blank when unset
 * @property avatar the user's own photo when one was set, otherwise null
 * @property remoteAvatarUrl YouTube account picture, used when there is no own photo
 */
data class BaajaProfile(
    val name: String,
    val bio: String,
    val avatar: File?,
    val remoteAvatarUrl: String?,
    val hasCustomName: Boolean,
) {
    /** Model to hand to an image loader: own photo first, then the account picture. */
    val avatarModel: Any? get() = avatar ?: remoteAvatarUrl
}

/** First letter of [name] in upper case, or a note symbol for blank names. */
fun profileInitial(name: String): String =
    name.trim().firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "♪"

/** Picks the edited name when set, else the account name, else [fallback]. */
fun resolveProfileName(
    edited: String,
    accountName: String,
    fallback: String,
): String = edited.trim().ifBlank { accountName.trim() }.ifBlank { fallback }

private const val AVATAR_DIR = "profile"
private const val AVATAR_FILE = "avatar.jpg"
const val PROFILE_AVATAR_MAX_PX = 512

fun profileAvatarFile(context: Context): File = File(File(context.filesDir, AVATAR_DIR), AVATAR_FILE)

/** Largest power-of-two sample size that keeps the decoded image at least [maxPx] on its long edge. */
internal fun sampleSizeFor(
    width: Int,
    height: Int,
    maxPx: Int,
): Int {
    var sample = 1
    while (maxOf(width, height) / (sample * 2) >= maxPx) sample *= 2
    return sample
}

/**
 * Copies the picked image into app-private storage as a JPEG no larger than 512 px.
 * Returns true on success. Runs on the IO dispatcher.
 */
suspend fun saveProfileAvatar(
    context: Context,
    source: Uri,
): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching false
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, PROFILE_AVATAR_MAX_PX)
            }
            val decoded =
                resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, options) }
                    ?: return@runCatching false
            val longEdge = maxOf(decoded.width, decoded.height)
            val scaled =
                if (longEdge > PROFILE_AVATAR_MAX_PX) {
                    val ratio = PROFILE_AVATAR_MAX_PX.toFloat() / longEdge
                    Bitmap.createScaledBitmap(
                        decoded,
                        (decoded.width * ratio).toInt().coerceAtLeast(1),
                        (decoded.height * ratio).toInt().coerceAtLeast(1),
                        true,
                    )
                } else {
                    decoded
                }
            val target = profileAvatarFile(context)
            target.parentFile?.mkdirs()
            target.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            true
        }.getOrDefault(false)
    }

fun deleteProfileAvatar(context: Context) {
    runCatching { profileAvatarFile(context).delete() }
}

/**
 * The profile to show everywhere the user's name/avatar appears. Safe to call from any screen,
 * for example the Home top bar: `val profile = rememberBaajaProfile()` then show
 * `profile.avatarModel` (or [profileInitial] of `profile.name` when it is null).
 */
@Composable
fun rememberBaajaProfile(): BaajaProfile {
    val account = rememberBaajaAccount()
    val editedName by rememberPreference(BaajaProfileNameKey, "")
    val bio by rememberPreference(BaajaProfileBioKey, "")
    val avatarVersion by rememberPreference(BaajaProfileAvatarVersionKey, 0L)
    val context = androidx.compose.ui.platform.LocalContext.current
    val guestName = androidx.compose.ui.res.stringResource(com.metrolist.music.R.string.baaja_onb_profile_guest_name)
    val avatar =
        remember(avatarVersion) {
            profileAvatarFile(context).takeIf { avatarVersion > 0L && it.exists() }
        }
    return BaajaProfile(
        name = resolveProfileName(editedName, if (account.isLoggedIn) account.name else "", guestName),
        bio = bio.trim(),
        avatar = avatar,
        remoteAvatarUrl = account.imageUrl,
        hasCustomName = editedName.isNotBlank(),
    )
}
