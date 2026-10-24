/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.recognition

import android.Manifest
import android.content.pm.PackageManager
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.db.entities.RecognitionHistory
import com.metrolist.music.ui.theme.BaajaArtwork
import com.metrolist.music.ui.theme.BaajaIconButton
import com.metrolist.music.ui.theme.BaajaPillButton
import com.metrolist.music.ui.theme.BaajaSectionHeader
import com.metrolist.music.ui.theme.baajaInk
import com.metrolist.music.ui.theme.baajaSurface
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.SearchRoutes
import com.metrolist.shazamkit.models.RecognitionResult
import com.metrolist.shazamkit.models.RecognitionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

@Composable
fun RecognitionScreen(
    navController: NavController,
    autoStart: Boolean = false,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()

    // Only reset in Ready state: Listening/Processing belong to a running widget-service
    // recognition that must not be cancelled; Success/NoMatch/Error are results pending
    // display and history saving.
    LaunchedEffect(Unit) {
        if (com.metrolist.music.recognition.MusicRecognitionService.recognitionStatus.value
                is RecognitionStatus.Ready
        ) {
            com.metrolist.music.recognition.MusicRecognitionService
                .reset()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            com.metrolist.music.recognition.MusicRecognitionService
                .reset()
        }
    }

    // Observe recognition status from service for real-time updates (Listening -> Processing -> Result)
    val recognitionStatus by com.metrolist.music.recognition.MusicRecognitionService.recognitionStatus
        .collectAsStateWithLifecycle()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            hasPermission = isGranted
            if (isGranted) {
                coroutineScope.launch {
                    com.metrolist.music.recognition.MusicRecognitionService
                        .recognize(context)
                }
            }
        }

    fun startRecognition() {
        if (hasPermission) {
            coroutineScope.launch {
                com.metrolist.music.recognition.MusicRecognitionService
                    .recognize(context)
            }
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(Unit) {
        if (autoStart &&
            com.metrolist.music.recognition.MusicRecognitionService.recognitionStatus.value
                is RecognitionStatus.Ready
        ) {
            startRecognition()
        }
    }

    fun resetToReady() {
        com.metrolist.music.recognition.MusicRecognitionService
            .reset()
    }

    fun saveToHistory(result: RecognitionResult) {
        // Skip if the widget service already persisted this result to avoid a duplicate entry
        if (com.metrolist.music.recognition.MusicRecognitionService.resultSavedExternally) return
        coroutineScope.launch(Dispatchers.IO) {
            database.query {
                insert(
                    RecognitionHistory(
                        trackId = result.trackId,
                        title = result.title,
                        artist = result.artist,
                        album = result.album,
                        coverArtUrl = result.coverArtUrl,
                        coverArtHqUrl = result.coverArtHqUrl,
                        genre = result.genre,
                        releaseDate = result.releaseDate,
                        label = result.label,
                        shazamUrl = result.shazamUrl,
                        appleMusicUrl = result.appleMusicUrl,
                        spotifyUrl = result.spotifyUrl,
                        isrc = result.isrc,
                        youtubeVideoId = result.youtubeVideoId,
                        recognizedAt = LocalDateTime.now(),
                    ),
                )
            }
        }
    }


    val recentItems by database.recognitionHistory().collectAsStateWithLifecycle(initialValue = emptyList())
    val status = recognitionStatus
    val insets = LocalPlayerAwareWindowInsets.current

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState())
                .padding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal).asPaddingValues())
                .padding(start = 20.dp, end = 20.dp, top = 8.dp)
                .padding(bottom = insets.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding() + 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        RecognitionHeader(
            onBack = { navController.navigateUp() },
            onBackLong = { navController.backToMain() },
            onHistory = { navController.navigate("recognition_history") },
        )

        // Heading + hint
        val (heading, sub) =
            when (status) {
                is RecognitionStatus.Ready -> stringResource(R.string.baaja_ai_rec_ready) to stringResource(R.string.baaja_ai_rec_ready_sub)
                is RecognitionStatus.Listening -> stringResource(R.string.listening) to stringResource(R.string.baaja_ai_rec_listening_sub)
                is RecognitionStatus.Processing -> stringResource(R.string.baaja_ai_rec_processing) to stringResource(R.string.baaja_ai_rec_processing_sub)
                is RecognitionStatus.Success -> stringResource(R.string.baaja_ai_rec_found) to stringResource(R.string.baaja_ai_rec_found_sub)
                is RecognitionStatus.NoMatch -> stringResource(R.string.no_match_found) to status.message
                is RecognitionStatus.Error -> stringResource(R.string.recognition_error) to status.message
            }
        Column(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                heading,
                fontSize = 32.sp,
                lineHeight = 37.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
                textAlign = TextAlign.Center,
            )
            Text(
                sub,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }

        ListenButton(
            status = status,
            onClick = {
                when (status) {
                    is RecognitionStatus.Listening -> resetToReady()
                    is RecognitionStatus.Processing -> Unit
                    else -> startRecognition()
                }
            },
        )

        AnimatedContent(
            targetState = status,
            contentKey = { it::class },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "recognition_result",
            modifier = Modifier.fillMaxWidth(),
        ) { current ->
            when (current) {
                is RecognitionStatus.Success ->
                    ResultCard(
                        result = current.result,
                        onPlayOnApp = { result ->
                            // Search for the track on YouTube Music
                            val searchQuery = "${result.title} ${result.artist}"
                            navController.navigate(SearchRoutes.resultRoute(searchQuery))
                        },
                        onClose = ::resetToReady,
                        onSaveToHistory = ::saveToHistory,
                    )

                is RecognitionStatus.NoMatch, is RecognitionStatus.Error ->
                    RetryCard(onTryAgain = { startRecognition() })

                else ->
                    RecentSection(
                        items = recentItems.take(3),
                        onItemClick = { item ->
                            navController.navigate(SearchRoutes.resultRoute("${item.title} ${item.artist}"))
                        },
                        onSeeAll = { navController.navigate("recognition_history") },
                    )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecognitionHeader(
    onBack: () -> Unit,
    onBackLong: () -> Unit,
    onHistory: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .combinedClickable(role = Role.Button, onClick = onBack, onLongClick = onBackLong),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.arrow_back), stringResource(R.string.back_button_desc), modifier = Modifier.size(26.dp))
        }
        Text(
            stringResource(R.string.baaja_ai_rec_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onHistory),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.history), stringResource(R.string.recognition_history), modifier = Modifier.size(24.dp))
        }
    }
}

/** Big outlined mic inside dashed rings that pulse while listening and spin while matching. */
@Composable
private fun ListenButton(
    status: RecognitionStatus,
    onClick: () -> Unit,
) {
    val listening = status is RecognitionStatus.Listening
    val processing = status is RecognitionStatus.Processing
    val transition = rememberInfiniteTransition(label = "listen")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "spin",
    )
    val active = listening || processing
    val ringColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    val ink = baajaInk
    val label =
        when (status) {
            is RecognitionStatus.Listening -> stringResource(R.string.baaja_ai_rec_stop)
            is RecognitionStatus.Success -> stringResource(R.string.baaja_ai_rec_again)
            is RecognitionStatus.Processing -> stringResource(R.string.processing)
            else -> stringResource(R.string.baaja_ai_rec_start)
        }

    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, 300.dp)
        val unit = side / 300
        Box(Modifier.size(side), contentAlignment = Alignment.Center) {
            val ringModifier =
                Modifier
                    .fillMaxSize()
                    .scale(if (listening) pulse else 1f)
                    .rotate(if (active) spin else 0f)
            Canvas(ringModifier) {
                val dash = PathEffect.dashPathEffect(floatArrayOf(10f * density / 2, 8f * density / 2))
                val stroke = 2.dp.toPx()
                drawCircle(ringColor, radius = size.minDimension / 2 - stroke, style = Stroke(stroke, pathEffect = dash))
                drawCircle(
                    ringColor,
                    radius = size.minDimension / 2 - (34.dp * unit.value).toPx() - stroke,
                    style = Stroke(stroke, pathEffect = dash),
                )
            }
            Box(
                Modifier
                    .size(side - 136.dp * unit.value)
                    .scale(if (listening) 2f - pulse else 1f)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .border(2.dp, ink, CircleShape),
            )
            Box(
                Modifier
                    .size(132.dp * unit.value)
                    .baajaSurface(ink, MaterialTheme.colorScheme.primary, CircleShape, shadowOffset = 5.dp, borderWidth = 3.dp)
                    .clickable(enabled = !processing, role = Role.Button, onClick = onClick)
                    .semantics { contentDescription = label },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(if (processing) R.drawable.music_note else R.drawable.mic),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(48.dp * unit.value),
                )
            }
        }
    }
}

@Composable
private fun ResultCard(
    result: RecognitionResult,
    onPlayOnApp: (RecognitionResult) -> Unit,
    onClose: () -> Unit,
    onSaveToHistory: (RecognitionResult) -> Unit,
) {
    // Save to history when success is shown
    LaunchedEffect(result) {
        onSaveToHistory(result)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(end = 4.dp, bottom = 6.dp)
            .baajaSurface(baajaInk, MaterialTheme.colorScheme.surface, RoundedCornerShape(26.dp), shadowOffset = 4.dp, borderWidth = 2.5.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            BaajaArtwork(
                model = result.coverArtHqUrl ?: result.coverArtUrl,
                modifier = Modifier.size(68.dp),
                shape = RoundedCornerShape(16.dp),
                shadowOffset = 0.dp,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.baaja_ai_rec_found_label).uppercase(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(result.title, fontSize = 21.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(result.artist, result.album?.takeIf { it.isNotBlank() }).joinToString(" · "),
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BaajaIconButton(
                icon = R.drawable.close,
                contentDescription = stringResource(R.string.close),
                onClick = onClose,
                size = 44.dp,
            )
        }
        BaajaPillButton(
            text = stringResource(R.string.play_on_app),
            onClick = { onPlayOnApp(result) },
            icon = R.drawable.play,
            height = 50.dp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RetryCard(onTryAgain: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .baajaSurface(baajaInk, MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(26.dp), shadowOffset = 0.dp)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BaajaPillButton(
            text = stringResource(R.string.try_again),
            onClick = onTryAgain,
            primary = false,
            icon = R.drawable.refresh,
            height = 50.dp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RecentSection(
    items: List<RecognitionHistory>,
    onItemClick: (RecognitionHistory) -> Unit,
    onSeeAll: () -> Unit,
) {
    if (items.isEmpty()) return
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BaajaSectionHeader(
            title = stringResource(R.string.baaja_ai_rec_recent),
            actionLabel = stringResource(R.string.baaja_ai_rec_see_all),
            onAction = onSeeAll,
        )
        items.forEach { item ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onItemClick(item) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BaajaArtwork(item.coverArtUrl, Modifier.size(48.dp), shape = RoundedCornerShape(12.dp), shadowOffset = 0.dp)
                Column(Modifier.weight(1f)) {
                    Text(item.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.artist, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(relativeTime(item.recognizedAt), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

private fun relativeTime(time: LocalDateTime): String {
    val millis = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    return DateUtils
        .getRelativeTimeSpanString(millis, System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE)
        .toString()
}
