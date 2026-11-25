/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.metrolist.music.R
import com.metrolist.music.playback.MusicVideoMode

/** A surface the music video draws into while this view is on screen. */
@OptIn(UnstableApi::class)
@Composable
private fun MusicVideoSurface(
    modifier: Modifier = Modifier,
    pictureInPicture: Boolean = false,
) {
    val context = LocalContext.current
    val view = remember { TextureView(context) }
    DisposableEffect(view) {
        MusicVideoMode.attachSurface(view, pictureInPicture)
        onDispose { MusicVideoMode.detachSurface(view) }
    }
    AndroidView(factory = { view }, modifier = modifier)
}

/** The music video in the player, in place of the artwork. */
@OptIn(UnstableApi::class)
@Composable
fun MusicVideoCard(
    state: MusicVideoMode.State,
    modifier: Modifier = Modifier,
) {
    Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        MusicVideoSurface(Modifier.fillMaxSize())
        when (state) {
            MusicVideoMode.State.Loading -> CircularProgressIndicator(color = Color.White, modifier = Modifier.size(36.dp))
            MusicVideoMode.State.NoVideo, MusicVideoMode.State.Failed ->
                Text(
                    stringResource(if (state == MusicVideoMode.State.NoVideo) R.string.baaja_video_none else R.string.baaja_video_failed),
                    color = Color.White,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                )
            else -> Unit
        }
    }
}

/** Shrinks the app to a floating window showing just the music video. */
@OptIn(UnstableApi::class)
fun enterMusicVideoPictureInPicture(activity: Activity) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val ratio = Rational((MusicVideoMode.aspectRatio.value * 1000).toInt().coerceIn(420, 2390), 1000)
    runCatching { activity.enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(ratio).build()) }
}

/** What the Picture-in-Picture window shows: only the video. */
@OptIn(UnstableApi::class)
@Composable
fun MusicVideoOverlay(isInPictureInPicture: Boolean) {
    val state by MusicVideoMode.state.collectAsStateWithLifecycle()
    if (!isInPictureInPicture || state == MusicVideoMode.State.Off) return
    val aspect by MusicVideoMode.aspectRatio.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        MusicVideoSurface(Modifier.fillMaxWidth().aspectRatio(aspect.coerceIn(0.3f, 3f)), pictureInPicture = true)
    }
}
