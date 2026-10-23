/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.recognition

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.metrolist.music.ui.component.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.db.entities.RecognitionHistory
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.theme.BaajaArtwork
import com.metrolist.music.ui.theme.BaajaIconButton
import com.metrolist.music.ui.theme.baajaInk
import com.metrolist.music.ui.theme.baajaSurface
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.SearchRoutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@Composable
fun RecognitionHistoryScreen(navController: NavController) {
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()

    val historyItems by database.recognitionHistory().collectAsStateWithLifecycle(initialValue = emptyList())
    var showClearDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<RecognitionHistory?>(null) }

    if (showClearDialog) {
        DefaultDialog(
            onDismiss = { showClearDialog = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.delete),
                    contentDescription = null,
                )
            },
            title = { Text(stringResource(R.string.clear_recognition_history)) },
            buttons = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            database.query {
                                clearRecognitionHistory()
                            }
                        }
                        showClearDialog = false
                    },
                ) {
                    Text(stringResource(R.string.clear))
                }
            },
        ) {
            Text(
                text = stringResource(R.string.clear_recognition_history_confirm),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }

    itemToDelete?.let { item ->
        DefaultDialog(
            onDismiss = { itemToDelete = null },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.delete),
                    contentDescription = null,
                )
            },
            title = { Text(stringResource(R.string.delete)) },
            buttons = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            database.query {
                                deleteRecognitionHistoryById(item.id)
                            }
                        }
                        itemToDelete = null
                    },
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
        ) {
            Text(
                text = stringResource(R.string.delete_playlist_confirm, item.title),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }

    val insets = LocalPlayerAwareWindowInsets.current
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(insets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        HistoryHeader(
            onBack = { navController.navigateUp() },
            onBackLong = { navController.backToMain() },
            showClear = historyItems.isNotEmpty(),
            onClear = { showClearDialog = true },
        )
        if (historyItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(insets.only(WindowInsetsSides.Bottom).asPaddingValues()),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        Modifier
                            .size(88.dp)
                            .baajaSurface(baajaInk, MaterialTheme.colorScheme.secondaryContainer, CircleShape, shadowOffset = 3.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.history),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    Text(
                        text = stringResource(R.string.baaja_ai_rec_history_empty),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = insets.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding() + 16.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = historyItems,
                    key = { "recognition_${it.id}" },
                ) { item ->
                    RecognitionHistoryItem(
                        item = item,
                        onClick = {
                            // Search for the track on YouTube Music
                            val searchQuery = "${item.title} ${item.artist}"
                            navController.navigate(SearchRoutes.resultRoute(searchQuery))
                        },
                        onDelete = {
                            itemToDelete = item
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryHeader(
    onBack: () -> Unit,
    onBackLong: () -> Unit,
    showClear: Boolean,
    onClear: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
            stringResource(R.string.recognition_history),
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (showClear) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onClear),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.clear_all),
                    contentDescription = stringResource(R.string.clear_recognition_history),
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun RecognitionHistoryItem(
    item: RecognitionHistory,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm") }
    val shape = RoundedCornerShape(20.dp)

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .baajaSurface(baajaInk, MaterialTheme.colorScheme.surface, shape, shadowOffset = 3.dp)
                .clickable { onClick() }
                .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BaajaArtwork(item.coverArtUrl, Modifier.size(60.dp), shape = RoundedCornerShape(14.dp), shadowOffset = 0.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.artist,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.recognizedAt.format(dateFormatter),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            )
        }
        BaajaIconButton(
            icon = R.drawable.delete,
            contentDescription = stringResource(R.string.delete_from_history),
            onClick = onDelete,
            size = 44.dp,
        )
    }
}
