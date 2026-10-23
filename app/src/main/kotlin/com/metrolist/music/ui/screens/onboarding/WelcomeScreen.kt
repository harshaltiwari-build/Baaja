/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.music.LocalDatabase
import com.metrolist.music.R
import com.metrolist.music.constants.BaajaHasSeenWelcomeKey
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.ui.theme.BaajaInk
import com.metrolist.music.ui.theme.BaajaPink
import com.metrolist.music.ui.theme.BaajaYellow
import com.metrolist.music.ui.theme.baajaInk
import com.metrolist.music.ui.theme.baajaSurface
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.safeDataStoreEdit
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * Shows the first-run welcome screen over [content] until the user starts as a guest or chooses to sign in.
 * [content] stays composed underneath so the app is ready the moment the welcome screen goes away.
 * Users who are already signed in (e.g. after an update) never see it.
 */
@Composable
fun BaajaWelcomeGate(
    onSignIn: () -> Unit,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showWelcome by rememberSaveable {
        // One synchronous read on first composition so returning users never see a flash of the welcome screen.
        val seen = context.dataStore.get(BaajaHasSeenWelcomeKey, false)
        val signedIn = "SAPISID" in runCatching { parseCookieString(context.dataStore.get(InnerTubeCookieKey, "")) }.getOrDefault(emptyMap())
        mutableStateOf(!seen && !signedIn)
    }

    fun markSeen() {
        showWelcome = false
        scope.launch { context.safeDataStoreEdit { it[BaajaHasSeenWelcomeKey] = true } }
    }

    Box(Modifier.fillMaxSize()) {
        content()
        AnimatedVisibility(
            visible = showWelcome,
            enter = fadeIn(tween(0)),
            exit = fadeOut(tween(250)),
        ) {
            BaajaWelcomeScreen(
                onStart = ::markSeen,
                onSignIn = {
                    markSeen()
                    onSignIn()
                },
                modifier =
                    Modifier.pointerInput(Unit) {
                        // Swallow touches so nothing reaches the app underneath while the welcome screen is up.
                        awaitPointerEventScope { while (true) awaitPointerEvent() }
                    },
            )
        }
    }
}

/** First-run welcome: brand, artwork collage with feature stickers, headline and the two ways in. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BaajaWelcomeScreen(
    onStart: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val database = LocalDatabase.current
    val covers by remember(database) {
        database
            .mostPlayedSongs(fromTimeStamp = LocalDateTime.of(2000, 1, 1, 0, 0), limit = 4)
            .map { songs -> songs.mapNotNull { it.song.thumbnailUrl } }
    }.collectAsStateWithLifecycle(emptyList())

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        val viewportHeight = maxHeight
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .heightIn(min = viewportHeight)
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(20.dp))
            BaajaBrandRow(Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(20.dp))

            WelcomeCollage(covers = covers, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(28.dp))
            WelcomeHeadline()
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.baaja_onb_welcome_body),
                fontSize = 17.sp,
                lineHeight = 24.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(28.dp))
            Spacer(Modifier.weight(1f))

            BaajaCtaButton(
                text = stringResource(R.string.baaja_onb_welcome_start),
                onClick = onStart,
                modifier = Modifier.heightIn(min = 64.dp),
            )
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.baaja_onb_welcome_have_account),
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.baaja_onb_sign_in),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier =
                        Modifier
                            .clip(CircleShape)
                            .clickable(role = Role.Button, onClick = onSignIn)
                            .heightIn(min = 44.dp)
                            .padding(horizontal = 6.dp, vertical = 12.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WelcomeHeadline() {
    val start = stringResource(R.string.baaja_onb_welcome_title_start)
    val highlight = stringResource(R.string.baaja_onb_welcome_title_highlight)
    val end = stringResource(R.string.baaja_onb_welcome_title_end)
    val headlineSize = 42.sp
    FlowRow(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) { heading() },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        start.split(' ').filter(String::isNotBlank).forEach { word ->
            Text(word, fontSize = headlineSize, lineHeight = 46.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp)
        }
        Text(
            highlight,
            fontSize = headlineSize,
            lineHeight = 46.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-1).sp,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier =
                Modifier
                    .baajaSurface(baajaInk, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp), shadowOffset = 0.dp)
                    .padding(horizontal = 8.dp),
        )
        end.split(' ').filter(String::isNotBlank).forEach { word ->
            Text(word, fontSize = headlineSize, lineHeight = 46.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp)
        }
    }
}

/** Tilted artwork card (real covers when the library has some, drawn placeholders otherwise) with feature stickers. */
@Composable
private fun WelcomeCollage(
    covers: List<String>,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val cardSize = minOf(maxWidth - 56.dp, 300.dp)
        Box(Modifier.fillMaxWidth().height(cardSize + 24.dp)) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(cardSize)
                    .rotate(-3f)
                    .baajaSurface(baajaInk, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(34.dp), shadowOffset = 6.dp, borderWidth = 2.5.dp),
            ) {
                Column(Modifier.fillMaxSize()) {
                    repeat(2) { row ->
                        Row(Modifier.weight(1f)) {
                            repeat(2) { col ->
                                val index = row * 2 + col
                                val cover = covers.getOrNull(index)
                                Box(Modifier.weight(1f).fillMaxSize()) {
                                    if (covers.size >= 4 && cover != null) {
                                        AsyncImage(
                                            model = cover,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    } else {
                                        BaajaPlaceholderArt(variant = index, modifier = Modifier.fillMaxSize())
                                    }
                                }
                            }
                        }
                    }
                }
            }

            WelcomeSticker(
                text = stringResource(R.string.baaja_onb_welcome_sticker_no_ads),
                container = BaajaYellow,
                content = BaajaInk,
                icon = null,
                rotation = -8f,
                modifier = Modifier.align(Alignment.TopStart).offset(x = (-8).dp, y = cardSize * 0.1f),
            )
            WelcomeSticker(
                text = stringResource(R.string.baaja_onb_welcome_sticker_lyrics),
                container = BaajaPink,
                content = Color.White,
                icon = R.drawable.favorite,
                rotation = 6f,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = cardSize * 0.68f),
            )
            WelcomeSticker(
                text = stringResource(R.string.baaja_onb_welcome_sticker_offline),
                container = Color.White,
                content = BaajaInk,
                icon = R.drawable.download,
                rotation = -4f,
                modifier = Modifier.align(Alignment.TopStart).offset(x = 2.dp, y = cardSize * 0.86f),
            )
        }
    }
}

@Composable
private fun WelcomeSticker(
    text: String,
    container: Color,
    content: Color,
    @DrawableRes icon: Int?,
    rotation: Float,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .rotate(rotation)
            .heightIn(min = 40.dp)
            .baajaSurface(BaajaInk, container, CircleShape, shadowOffset = 2.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(painterResource(icon), null, tint = content, modifier = Modifier.size(16.dp))
        Text(text, color = content, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
    }
}
