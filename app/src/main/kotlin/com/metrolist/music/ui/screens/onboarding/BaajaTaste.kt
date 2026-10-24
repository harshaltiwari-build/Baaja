/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.width
import com.metrolist.music.ui.component.BaajaTileColors
import com.metrolist.music.ui.theme.baajaSurface
import com.metrolist.music.ui.theme.baajaOutline
import com.metrolist.music.ui.theme.BaajaYellow
import com.metrolist.music.ui.theme.BaajaInk
import coil3.compose.AsyncImage
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.YouTube.SearchFilter
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.music.R
import com.metrolist.music.ui.theme.BaajaChip
import com.metrolist.music.ui.theme.BaajaIconButton
import com.metrolist.music.ui.theme.BaajaPillButton
import com.metrolist.music.ui.theme.baajaInk
import com.metrolist.music.ui.component.home.baajaPastelFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

data class BaajaLanguage(val code: String, val name: String, val native: String)

val BaajaLanguages =
    listOf(
        BaajaLanguage("hi", "Hindi", "हिन्दी"),
        BaajaLanguage("ta", "Tamil", "தமிழ்"),
        BaajaLanguage("te", "Telugu", "తెలుగు"),
        BaajaLanguage("pa", "Punjabi", "ਪੰਜਾਬੀ"),
        BaajaLanguage("mr", "Marathi", "मराठी"),
        BaajaLanguage("gu", "Gujarati", "ગુજરાતી"),
        BaajaLanguage("bn", "Bengali", "বাংলা"),
        BaajaLanguage("kn", "Kannada", "ಕನ್ನಡ"),
        BaajaLanguage("bho", "Bhojpuri", "भोजपुरी"),
        BaajaLanguage("ml", "Malayalam", "മലയാളം"),
        BaajaLanguage("ur", "Urdu", "اردو"),
        BaajaLanguage("en", "English", "English"),
    )

data class BaajaArtist(val id: String, val name: String, val thumbnail: String?)

fun decodeLanguages(raw: String): Set<String> = raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()

fun encodeLanguages(codes: Set<String>): String = BaajaLanguages.map { it.code }.filter { it in codes }.joinToString(",")

fun encodeArtists(artists: List<BaajaArtist>): String =
    artists.joinToString("\n") { "${it.id}\t${it.name.replace('\t', ' ').replace('\n', ' ')}\t${it.thumbnail.orEmpty()}" }

fun decodeArtists(raw: String): List<BaajaArtist> =
    raw.lineSequence()
        .mapNotNull { line ->
            val parts = line.split('\t')
            if (parts.size >= 2 && parts[0].isNotBlank()) {
                BaajaArtist(parts[0], parts[1], parts.getOrNull(2)?.ifBlank { null })
            } else {
                null
            }
        }.toList()

private fun ArtistItem.toBaaja() = BaajaArtist(id, title, thumbnail)

/** Language chips shared by the first-run step and the Settings dialog. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BaajaLanguageChips(
    selected: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BaajaLanguages.forEach { language ->
            val label = if (language.native == language.name) language.name else "${language.name} (${language.native})"
            BaajaChip(
                label = label,
                selected = language.code in selected,
                onClick = { onToggle(language.code) },
            )
        }
    }
}

/** Dialog window that covers the whole screen, behind the status and navigation bars, with no dimming. */
@Composable
fun BaajaFullScreenDialog(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismissRequest,
        properties =
            androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
    ) {
        val window = (androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        androidx.compose.runtime.SideEffect {
            window?.setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            )
            window?.setDimAmount(0f)
        }
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) { content() }
    }
}

/** Two step dots with "Skip for now" on the right; without steps (Settings) a back button on the left instead. */
@Composable
private fun TasteTopBar(
    step: Int?,
    onSkip: () -> Unit,
    skipLabel: String = stringResource(R.string.baaja_taste_skip),
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    val ink = baajaInk
    Row(
        Modifier.fillMaxWidth().height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (step == null) {
            BaajaIconButton(
                icon = R.drawable.arrow_back,
                contentDescription = stringResource(R.string.baaja_back),
                onClick = onSkip,
            )
            if (actionLabel != null) {
                TextButton(onClick = onAction) {
                    Text(actionLabel, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                (1..2).forEach { n ->
                    Box(
                        Modifier
                            .height(8.dp)
                            .width(if (n == step) 26.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (n == step) MaterialTheme.colorScheme.primary else ink.copy(alpha = 0.25f)),
                    )
                }
            }
            TextButton(onClick = onSkip) {
                Text(skipLabel, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun TasteHeading(title: String, sub: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, lineHeight = 36.sp)
        Text(sub, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Bottom action with a short note above it, fading in over the scrolling content. */
@Composable
private fun TasteBottomBar(
    note: String?,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val surface = MaterialTheme.colorScheme.surface
    Column(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(surface.copy(alpha = 0f), surface, surface)))
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (note != null) {
            Text(note, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        BaajaPillButton(text = label, onClick = onClick, enabled = enabled, height = 60.dp, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun tastePastels(): List<Color> =
    listOf(
        BaajaTileColors.lilac(),
        BaajaTileColors.peach(),
        BaajaTileColors.liked(),
        BaajaTileColors.mint(),
        BaajaTileColors.sky(),
    )

/** First-run step 1: which languages the user listens to. */
@Composable
fun BaajaLanguageStep(
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    showSteps: Boolean = true,
    actionLabel: String = stringResource(R.string.baaja_taste_next),
    skipLabel: String = stringResource(R.string.baaja_taste_skip),
) {
    val ink = baajaInk
    val pastels = tastePastels()
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 130.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            TasteTopBar(step = if (showSteps) 1 else null, onSkip = onSkip, skipLabel = skipLabel)
            TasteHeading(stringResource(R.string.baaja_taste_language_title), stringResource(R.string.baaja_taste_language_sub))
            BaajaLanguages.chunked(2).forEachIndexed { rowIndex, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEachIndexed { col, language ->
                        val isSelected = language.code in selected
                        val shape = RoundedCornerShape(20.dp)
                        Box(
                            Modifier
                                .weight(1f)
                                .height(76.dp)
                                .baajaSurface(
                                    ink,
                                    if (isSelected) pastels[(rowIndex * 2 + col) % pastels.size] else MaterialTheme.colorScheme.surface,
                                    shape,
                                    shadowOffset = if (isSelected) 3.dp else 0.dp,
                                    borderWidth = if (isSelected) 2.5.dp else 2.dp,
                                ).clickable(role = Role.Checkbox, onClick = { onToggle(language.code) })
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(language.native, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                if (language.native != language.name) {
                                    Text(language.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                }
                            }
                            if (isSelected) {
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 8.dp)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .border(2.dp, ink, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("✓", color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        TasteBottomBar(
            note =
                if (selected.isEmpty()) {
                    stringResource(R.string.baaja_taste_language_none)
                } else {
                    stringResource(R.string.baaja_taste_language_count, selected.size)
                },
            label = actionLabel,
            enabled = selected.isNotEmpty(),
            onClick = onNext,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private const val MIN_ARTISTS = 5

/**
 * First-run step 2 (and the Settings editor): pick artists from the chosen languages. Every pick
 * pulls in the artists that fans of it also like, so the suggestions grow as the user chooses.
 */
@Composable
fun BaajaArtistStep(
    languages: Set<String>,
    picked: List<BaajaArtist>,
    onToggle: (BaajaArtist) -> Unit,
    onDone: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    showSteps: Boolean = true,
    minPicks: Int = MIN_ARTISTS,
    onClearAll: (() -> Unit)? = null,
) {
    var base by remember { mutableStateOf<List<BaajaArtist>>(emptyList()) }
    var related by remember { mutableStateOf<List<BaajaArtist>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<BaajaArtist>?>(null) }
    val pickedIds = remember(picked) { picked.map { it.id }.toSet() }

    LaunchedEffect(languages) {
        loading = true
        val names = BaajaLanguages.filter { it.code in languages }.take(3).map { it.name }.ifEmpty { listOf("Hindi") }
        val lists =
            coroutineScope {
                names
                    .map { name ->
                        async {
                            YouTube
                                .search("top $name singers", SearchFilter.FILTER_ARTIST)
                                .getOrNull()
                                ?.items
                                ?.filterIsInstance<ArtistItem>()
                                ?.take(12)
                                ?.map { it.toBaaja() }
                                .orEmpty()
                        }
                    }.map { it.await() }
            }
        // Interleave so every chosen language gets space at the top of the grid.
        val merged = mutableListOf<BaajaArtist>()
        val longest = lists.maxOfOrNull { it.size } ?: 0
        for (i in 0 until longest) lists.forEach { list -> list.getOrNull(i)?.let(merged::add) }
        base = merged.distinctBy { it.id }
        loading = false
    }

    var knownPicks by remember { mutableStateOf(pickedIds) }
    LaunchedEffect(pickedIds) {
        val added = pickedIds - knownPicks
        knownPicks = pickedIds
        for (id in added) {
            val more =
                YouTube
                    .artist(id)
                    .getOrNull()
                    ?.sections
                    ?.flatMap { it.items }
                    ?.filterIsInstance<ArtistItem>()
                    ?.take(9)
                    ?.map { it.toBaaja() }
                    .orEmpty()
            related = (more + related).distinctBy { it.id }.take(60)
        }
    }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            results = null
        } else {
            delay(350)
            results =
                YouTube
                    .search(query.trim(), SearchFilter.FILTER_ARTIST)
                    .getOrNull()
                    ?.items
                    ?.filterIsInstance<ArtistItem>()
                    ?.map { it.toBaaja() }
                    .orEmpty()
        }
    }

    val suggested = remember(related, pickedIds) { related.filter { it.id !in pickedIds } }
    val popular = remember(base, related, pickedIds) { base.filter { it.id !in pickedIds && related.none { r -> r.id == it.id } } }
    val ink = baajaInk

    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                TasteTopBar(
                    step = if (showSteps) 2 else null,
                    onSkip = onSkip,
                    actionLabel = if (!showSteps && picked.isNotEmpty() && onClearAll != null) stringResource(R.string.baaja_taste_clear_all) else null,
                    onAction = { onClearAll?.invoke() },
                )
                TasteHeading(stringResource(R.string.baaja_taste_artists_title), stringResource(R.string.baaja_taste_artists_sub))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .baajaSurface(ink, MaterialTheme.colorScheme.surface, CircleShape, shadowOffset = 0.dp)
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (query.isEmpty()) {
                                    Text(
                                        stringResource(R.string.baaja_taste_search_artists),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                                inner()
                            }
                        },
                    )
                }
                val shownCount = picked.size.coerceAtMost(MIN_ARTISTS)
                val showProgress = minPicks > 0
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .baajaSurface(ink, BaajaYellow, CircleShape, shadowOffset = 0.dp, borderWidth = 2.5.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        if (showProgress) stringResource(R.string.baaja_taste_picked, picked.size, MIN_ARTISTS) else stringResource(R.string.baaja_taste_picked_plain, picked.size),
                        color = BaajaInk,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                    )
                    if (showProgress) {
                        Box(Modifier.weight(1f).height(8.dp).clip(CircleShape).background(BaajaInk.copy(alpha = 0.2f))) {
                            Box(Modifier.fillMaxWidth(shownCount / MIN_ARTISTS.toFloat()).height(8.dp).background(BaajaInk))
                        }
                        if (picked.size >= MIN_ARTISTS) Text("✓", color = BaajaInk, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 96.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 130.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                val shown = results
                if (shown != null) {
                    items(shown, key = { "r_${it.id}" }) { ArtistTile(it, it.id in pickedIds) { onToggle(it) } }
                } else {
                    items(picked, key = { "p_${it.id}" }) { ArtistTile(it, true) { onToggle(it) } }
                    if (suggested.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "more_header") {
                            Text(
                                "✦  " + stringResource(R.string.baaja_taste_more_like),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(top = 6.dp, start = 6.dp),
                            )
                        }
                        items(suggested, key = { "s_${it.id}" }) { ArtistTile(it, false) { onToggle(it) } }
                    }
                    items(popular, key = { "b_${it.id}" }) { ArtistTile(it, false) { onToggle(it) } }
                    if (loading) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "loading") {
                            Text(
                                stringResource(R.string.baaja_taste_loading),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                            )
                        }
                    }
                }
            }
        }
        TasteBottomBar(
            note = null,
            label =
                when {
                    minPicks == 0 -> stringResource(R.string.baaja_save)
                    picked.size >= minPicks -> stringResource(R.string.baaja_taste_done)
                    else -> stringResource(R.string.baaja_taste_pick_more, minPicks - picked.size)
                },
            enabled = picked.size >= minPicks,
            onClick = onDone,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun ArtistTile(
    artist: BaajaArtist,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val ink = baajaInk
    val ring = if (selected) MaterialTheme.colorScheme.primary else ink
    Column(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Checkbox, onClick = onClick)
            .semantics { contentDescription = artist.name }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(Modifier.widthIn(max = 104.dp).fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .fillMaxSize(0.92f)
                    .baajaOutline(ring, CircleShape, borderWidth = if (selected) 4.dp else 2.5.dp, shadowOffset = if (selected) 3.dp else 0.dp)
                    .clip(CircleShape)
                    .background(baajaPastelFor(artist.name)),
                contentAlignment = Alignment.Center,
            ) {
                if (artist.thumbnail != null) {
                    AsyncImage(
                        model = artist.thumbnail,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            if (selected) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .border(2.5.dp, ink, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✓", color = MaterialTheme.colorScheme.onPrimary, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        Text(
            artist.name,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            lineHeight = 15.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
