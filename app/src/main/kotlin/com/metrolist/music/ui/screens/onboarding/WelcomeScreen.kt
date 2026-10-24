/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.onboarding

import android.app.Activity
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import com.metrolist.music.constants.BaajaJustSignedInKey
import com.metrolist.music.constants.BaajaMusicLanguagesKey
import com.metrolist.music.constants.BaajaPickedArtistsKey
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.ui.theme.BaajaInk
import com.metrolist.music.ui.theme.BaajaPink
import com.metrolist.music.ui.theme.BaajaPurple
import com.metrolist.music.ui.theme.BaajaYellow
import com.metrolist.music.ui.theme.baajaInk
import com.metrolist.music.ui.theme.baajaSurface
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.safeDataStoreEdit
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime

private const val STAGE_NONE = 0
private const val STAGE_WELCOME = 1
private const val STAGE_LOGIN = 2
private const val STAGE_LANGUAGES = 3
private const val STAGE_ARTISTS = 4
private const val STAGE_WELCOME_BACK = 5
private const val UPDATE_GRACE_MILLIS = 60_000L

/**
 * First-run flow laid over [content]: splash (once per cold start), then Welcome, then the Login intro.
 * "Continue as guest" or a finished choice marks the welcome as seen so it never returns.
 * [onSignIn] is called when the user taps "Sign in with Google" and should open the web sign-in.
 * [content] stays composed underneath so the app is ready the moment the overlay goes away.
 * Users who already saw the welcome or are signed in skip straight to [content].
 */
@Composable
fun BaajaWelcomeGate(
    onSignIn: () -> Unit,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var stage by rememberSaveable {
        // One synchronous read on first composition so returning users never see a flash of the welcome screen.
        val seen = context.dataStore.get(BaajaHasSeenWelcomeKey, false)
        val signedIn = "SAPISID" in runCatching { parseCookieString(context.dataStore.get(InnerTubeCookieKey, "")) }.getOrDefault(emptyMap())
        // An app that was updated over an older install already knows its user, so it skips the welcome.
        val updated =
            runCatching {
                val info = context.packageManager.getPackageInfo(context.packageName, 0)
                info.lastUpdateTime - info.firstInstallTime > UPDATE_GRACE_MILLIS
            }.getOrDefault(false)
        val justSignedIn = signedIn && context.dataStore.get(BaajaJustSignedInKey, false)
        mutableStateOf(
            when {
                justSignedIn -> STAGE_WELCOME_BACK
                !seen && !signedIn && !updated -> STAGE_WELCOME
                else -> STAGE_NONE
            },
        )
    }
    var splashVisible by remember {
        mutableStateOf(!BaajaSplashState.shown && (context as? Activity).isPlainLauncherStart())
    }
    LaunchedEffect(Unit) {
        BaajaSplashState.shown = true
        if (splashVisible) {
            delay(SPLASH_MILLIS)
            splashVisible = false
        }
    }

    var languageCodes by rememberSaveable { mutableStateOf("") }
    // True when the language step was opened from the "Welcome back" screen: it saves and finishes, with no artist step.
    var languagesOnly by rememberSaveable { mutableStateOf(false) }
    var pickedRaw by rememberSaveable { mutableStateOf("") }

    fun markSeen() {
        stage = STAGE_NONE
        scope.launch {
            context.safeDataStoreEdit {
                it[BaajaHasSeenWelcomeKey] = true
                it[BaajaJustSignedInKey] = false
            }
        }
    }

    // Back never reaches the app underneath from the first-run screens: login goes back to welcome,
    // welcome (and the splash) send the app to the background.
    BackHandler(enabled = stage == STAGE_LOGIN) { stage = STAGE_WELCOME }
    BackHandler(enabled = stage == STAGE_LANGUAGES) { if (languagesOnly) stage = STAGE_WELCOME_BACK else stage = STAGE_LOGIN }
    BackHandler(enabled = stage == STAGE_ARTISTS) { stage = STAGE_LANGUAGES }
    BackHandler(enabled = stage == STAGE_WELCOME || splashVisible) {
        (context as? Activity)?.moveTaskToBack(true)
    }

    val covered = stage != STAGE_NONE || splashVisible
    Box(Modifier.fillMaxSize()) {
        // Hide the app underneath from screen readers while an overlay covers it.
        Box(Modifier.fillMaxSize().then(if (covered) Modifier.clearAndSetSemantics { } else Modifier)) {
            content()
        }
        // The first-run screens start (and play their entrance motion) once the splash is leaving.
        AnimatedVisibility(
            visible = stage != STAGE_NONE && !splashVisible,
            enter = fadeIn(tween(0)),
            exit = fadeOut(tween(250)),
        ) {
            val swallowTouches =
                Modifier.pointerInput(Unit) {
                    // Nothing reaches the app underneath while the first-run screens are up.
                    awaitPointerEventScope { while (true) awaitPointerEvent() }
                }
            if (stage == STAGE_WELCOME_BACK) {
                BaajaWelcomeBack(
                    onContinue = ::markSeen,
                    onAddLanguages = {
                        languagesOnly = true
                        stage = STAGE_LANGUAGES
                    },
                    modifier = swallowTouches,
                )
            } else if (stage == STAGE_LANGUAGES) {
                val chosen = decodeLanguages(languageCodes)
                BaajaLanguageStep(
                    selected = chosen,
                    onToggle = { code ->
                        languageCodes = encodeLanguages(if (code in chosen) chosen - code else chosen + code)
                    },
                    onNext = {
                        scope.launch { context.safeDataStoreEdit { it[BaajaMusicLanguagesKey] = languageCodes } }
                        if (languagesOnly) markSeen() else stage = STAGE_ARTISTS
                    },
                    onSkip = { if (languagesOnly) stage = STAGE_WELCOME_BACK else markSeen() },
                    modifier = swallowTouches,
                )
            } else if (stage == STAGE_ARTISTS) {
                val pickedArtists = decodeArtists(pickedRaw)
                BaajaArtistStep(
                    languages = decodeLanguages(languageCodes),
                    picked = pickedArtists,
                    onToggle = { artist ->
                        pickedRaw =
                            encodeArtists(
                                if (pickedArtists.any { it.id == artist.id }) pickedArtists.filterNot { it.id == artist.id } else pickedArtists + artist,
                            )
                    },
                    onDone = {
                        scope.launch { context.safeDataStoreEdit { it[BaajaPickedArtistsKey] = pickedRaw } }
                        markSeen()
                    },
                    onSkip = ::markSeen,
                    modifier = swallowTouches,
                )
            } else if (stage == STAGE_LOGIN) {
                BaajaLoginIntro(
                    onBack = { stage = STAGE_WELCOME },
                    onSignIn = {
                        markSeen()
                        onSignIn()
                    },
                    onContinueAsGuest = { stage = STAGE_LANGUAGES },
                    modifier = swallowTouches,
                )
            } else {
                BaajaWelcomeScreen(
                    onStart = { stage = STAGE_LOGIN },
                    onSignIn = { stage = STAGE_LOGIN },
                    modifier = swallowTouches,
                )
            }
        }
        AnimatedVisibility(
            visible = splashVisible,
            enter = fadeIn(tween(0)),
            exit = androidx.compose.animation.ExitTransition.None,
        ) {
            BaajaSplash(
                Modifier.pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
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

    val clock = rememberBaajaClock()
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
            BaajaBrandRow(Modifier.align(Alignment.CenterHorizontally).fadeUp(clock, 0f), clock = clock)
            Spacer(Modifier.height(20.dp))

            WelcomeCollage(covers = covers, clock = clock, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(28.dp))
            WelcomeHeadline(clock, Modifier.fadeUp(clock, 0.5f))
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.baaja_onb_welcome_body),
                fontSize = 17.sp,
                lineHeight = 24.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fadeUp(clock, 0.75f),
            )

            Spacer(Modifier.height(28.dp))
            Spacer(Modifier.weight(1f))

            BaajaCtaButton(
                text = stringResource(R.string.baaja_onb_welcome_start),
                onClick = onStart,
                modifier = Modifier.heightIn(min = 64.dp).fadeUp(clock, 1f),
                clock = clock,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().fadeUp(clock, 1.2f),
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
private fun WelcomeHeadline(
    clock: BaajaClock,
    modifier: Modifier = Modifier,
) {
    val start = stringResource(R.string.baaja_onb_welcome_title_start)
    val highlight = stringResource(R.string.baaja_onb_welcome_title_highlight)
    val end = stringResource(R.string.baaja_onb_welcome_title_end)
    val headlineSize = 42.sp
    FlowRow(
        modifier.fillMaxWidth().semantics(mergeDescendants = true) { heading() },
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
                    .graphicsLayer {
                        // "moves" wiggles: rotate -2..2 degrees while swelling up to 6%.
                        val w = clock.wave(2.6f, 1.4f)
                        rotationZ = -2f + 4f * w
                        scaleX = 1f + 0.06f * w
                        scaleY = 1f + 0.06f * w
                    }.baajaSurface(baajaInk, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp), shadowOffset = 0.dp)
                    .padding(horizontal = 8.dp),
        )
        end.split(' ').filter(String::isNotBlank).forEach { word ->
            Text(word, fontSize = headlineSize, lineHeight = 46.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp)
        }
    }
}

/**
 * Tilted artwork card with a spinning record behind it, bouncing equaliser bars, rising notes and bobbing
 * feature stickers (WelcomeMotion.dc.html). All motion comes from [clock] and is read in graphicsLayer/draw.
 */
@Composable
private fun WelcomeCollage(
    covers: List<String>,
    clock: BaajaClock,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val cardSize = minOf(maxWidth - 56.dp, 300.dp)
        Box(Modifier.fillMaxWidth().height(cardSize + 24.dp)) {
            // A record peeking out from behind the card's right edge.
            WelcomeVinyl(
                clock = clock,
                size = cardSize * (260f / 300f),
                modifier = Modifier.align(Alignment.Center).offset(x = cardSize * 0.3f, y = 0.dp),
            )
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(cardSize)
                    .graphicsLayer {
                        // Swings in from below with overshoot, then floats.
                        val enter = clock.enter(0.15f, 0.9f)
                        val e = easeOutBack(enter, 1.3f)
                        val fl = clock.wave(5f, 1.1f)
                        val settled = enter >= 1f
                        rotationZ = if (settled) lerpF(-3f, -1.2f, fl) else lerpF(-14f, -3f, e)
                        val sc = lerpF(0.7f, 1f, e)
                        scaleX = sc
                        scaleY = sc
                        translationY = if (settled) -9.dp.toPx() * fl else (1f - e) * 40.dp.toPx()
                        alpha = (enter * 3f).coerceAtMost(1f)
                    }.baajaSurface(baajaInk, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(34.dp), shadowOffset = 6.dp, borderWidth = 2.5.dp),
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
                WelcomeEqualizer(
                    clock = clock,
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 16.dp),
                )
            }

            WelcomeNote(clock, 0f, 26.dp, 18f, BaajaPurple, Modifier.align(Alignment.TopCenter).offset(x = 103.dp, y = 46.dp))
            WelcomeNote(clock, 1.2f, (-22).dp, -16f, BaajaPink, Modifier.align(Alignment.TopCenter).offset(x = (-87).dp, y = 56.dp))
            WelcomeNote(clock, 2.4f, 14.dp, 10f, BaajaYellow, Modifier.align(Alignment.TopCenter).offset(x = 13.dp, y = 36.dp))

            WelcomeSticker(
                clock = clock,
                period = 3.2f,
                delay = 0f,
                text = stringResource(R.string.baaja_onb_welcome_sticker_no_ads),
                container = BaajaYellow,
                content = BaajaInk,
                icon = null,
                rotation = -8f,
                modifier = Modifier.align(Alignment.TopStart).offset(x = (-8).dp, y = cardSize * 0.1f),
            )
            WelcomeSticker(
                clock = clock,
                period = 3.6f,
                delay = 0.5f,
                text = stringResource(R.string.baaja_onb_welcome_sticker_lyrics),
                container = BaajaPink,
                content = Color.White,
                icon = R.drawable.favorite,
                rotation = 6f,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = cardSize * 0.68f),
            )
            WelcomeSticker(
                clock = clock,
                period = 3.4f,
                delay = 1f,
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
    clock: BaajaClock,
    period: Float,
    delay: Float,
    text: String,
    container: Color,
    content: Color,
    @DrawableRes icon: Int?,
    rotation: Float,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .stickerBob(clock, period, delay, rotation)
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
