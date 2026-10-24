/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.search

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.innertube.utils.YouTubeUrlParser
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalIsPlayerExpanded
import com.metrolist.music.LocalNavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.PauseSearchHistoryKey
import com.metrolist.music.constants.SearchSource
import com.metrolist.music.constants.SearchSourceKey
import com.metrolist.music.db.entities.SearchHistory
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.home.BaajaHomeGutter
import com.metrolist.music.ui.component.home.BaajaPlainIconButton
import com.metrolist.music.ui.component.home.BaajaProfileAvatar
import com.metrolist.music.ui.component.home.BaajaSearchField
import com.metrolist.music.ui.theme.BaajaIconButton
import com.metrolist.music.ui.theme.baajaInk
import com.metrolist.music.ui.theme.baajaSurface
import com.metrolist.music.utils.SearchRoutes
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.HomeViewModel
import com.metrolist.music.viewmodels.MoodAndGenresViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * The Search tab. Before the user types it shows the Explore landing (recent searches, charts,
 * new releases, moods); once the field is focused or has text it shows suggestions (online) or
 * library results (local).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    pureBlack: Boolean,
    savedStateHandle: SavedStateHandle,
) {
    val navController = LocalNavController.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val playerConnection = LocalPlayerConnection.current
    val isPlayerExpanded = LocalIsPlayerExpanded.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var isHandlingScrollToTop by remember { mutableStateOf(false) }

    // Shares the activity-scoped HomeViewModel that the top bar already uses for the account picture.
    val activity = LocalActivity.current as? ComponentActivity
    val accountImageUrl =
        if (activity != null) {
            val homeViewModel: HomeViewModel = hiltViewModel(viewModelStoreOwner = activity)
            homeViewModel.accountImageUrl.collectAsStateWithLifecycle().value
        } else {
            null
        }
    val moodAndGenresViewModel: MoodAndGenresViewModel = hiltViewModel()
    val moodAndGenres by moodAndGenresViewModel.moodAndGenres.collectAsStateWithLifecycle()
    val exploreMoods = remember(moodAndGenres) { moodAndGenres?.flatMap { it.items } }
    val recentSearches by database.searchHistory().collectAsStateWithLifecycle(initialValue = emptyList())

    val scrollToTopCount by savedStateHandle.getStateFlow("scrollToTopCount", 0).collectAsStateWithLifecycle(initialValue = 0)

    var lastHandledCount by rememberSaveable { mutableIntStateOf(0) }
    var isFieldFocused by remember { mutableStateOf(false) }

    // Re-selecting the Search tab jumps straight into typing.
    LaunchedEffect(scrollToTopCount) {
        if (scrollToTopCount > lastHandledCount) {
            lastHandledCount = scrollToTopCount
            isHandlingScrollToTop = true

            kotlinx.coroutines.delay(100)

            if (!isPlayerExpanded) {
                focusManager.clearFocus(force = true)
                kotlinx.coroutines.delay(50)
                try {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                } catch (e: Exception) {
                }
            }

            kotlinx.coroutines.delay(500)
            isHandlingScrollToTop = false
        }
    }

    var searchSource by rememberEnumPreference(SearchSourceKey, SearchSource.ONLINE)
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    val pauseSearchHistory by rememberPreference(PauseSearchHistoryKey, defaultValue = false)
    val isTyping = isFieldFocused || query.text.isNotEmpty()

    BackHandler(enabled = isTyping) {
        query = TextFieldValue()
        focusManager.clearFocus()
    }

    fun handleSearch(searchQuery: String) {
        if (searchQuery.isEmpty()) {
            return
        }

        focusManager.clearFocus()

        when (val parsedUrl = YouTubeUrlParser.parse(searchQuery)) {
            is YouTubeUrlParser.ParsedUrl.Video -> {
                playerConnection?.playQueue(
                    YouTubeQueue(
                        WatchEndpoint(videoId = parsedUrl.id),
                    ),
                )
            }

            is YouTubeUrlParser.ParsedUrl.Playlist -> {
                navController.navigate("online_playlist/${parsedUrl.id}")
            }

            is YouTubeUrlParser.ParsedUrl.Album -> {
                navController.navigate("album/MPREb_${parsedUrl.id}")
            }

            is YouTubeUrlParser.ParsedUrl.Artist -> {
                navController.navigate("artist/${parsedUrl.id}")
            }

            null -> {
                navController.navigate(SearchRoutes.resultRoute(searchQuery))
            }
        }

        if (!pauseSearchHistory) {
            coroutineScope.launch(Dispatchers.IO) {
                database.query {
                    insert(SearchHistory(query = searchQuery))
                }
            }
        }
    }

    val onSearch: (String) -> Unit = { searchQuery -> handleSearch(searchQuery) }

    val bottomPadding = LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom).asPaddingValues()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        if (!isTyping) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = BaajaHomeGutter, end = BaajaHomeGutter, top = 16.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.baaja_home_explore),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.6).sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                BaajaIconButton(
                    icon = R.drawable.settings,
                    contentDescription = stringResource(R.string.settings),
                    onClick = { navController.navigate("settings") },
                    size = 44.dp,
                )
                Spacer(Modifier.width(10.dp))
                BaajaProfileAvatar(
                    imageUrl = accountImageUrl,
                    onClick = { navController.navigate("profile") },
                )
            }
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = if (isTyping) 8.dp else BaajaHomeGutter,
                        end = BaajaHomeGutter,
                        top = if (isTyping) 12.dp else 14.dp,
                        bottom = 14.dp,
                    ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isTyping) 4.dp else 10.dp),
        ) {
            if (isTyping) {
                BaajaPlainIconButton(
                    icon = R.drawable.arrow_back,
                    contentDescription = stringResource(R.string.baaja_home_back),
                    onClick = {
                        query = TextFieldValue()
                        focusManager.clearFocus()
                    },
                    iconSize = 26.dp,
                )
            }
            BaajaSearchField(
                value = query,
                onValueChange = { query = it },
                onSearch = onSearch,
                placeholder =
                    stringResource(
                        when (searchSource) {
                            SearchSource.LOCAL -> R.string.search_library
                            SearchSource.ONLINE -> R.string.baaja_home_search_hint
                        },
                    ),
                highlighted = isFieldFocused,
                focusRequester = focusRequester,
                onFocusChanged = { isFieldFocused = it },
                modifier = Modifier.weight(1f),
                trailing = {
                    BaajaPlainIconButton(
                        icon =
                            when (searchSource) {
                                SearchSource.LOCAL -> R.drawable.library_music
                                SearchSource.ONLINE -> R.drawable.language
                            },
                        contentDescription =
                            stringResource(
                                when (searchSource) {
                                    SearchSource.LOCAL -> R.string.baaja_home_search_online
                                    SearchSource.ONLINE -> R.string.baaja_home_search_library
                                },
                            ),
                        onClick = {
                            searchSource =
                                if (searchSource == SearchSource.ONLINE) {
                                    SearchSource.LOCAL
                                } else {
                                    SearchSource.ONLINE
                                }
                        },
                        iconSize = 20.dp,
                    )
                },
            )
            if (!isTyping) {
                val ink = baajaInk
                Box(
                    modifier =
                        Modifier
                            .size(54.dp)
                            .baajaSurface(ink, MaterialTheme.colorScheme.primary, CircleShape, shadowOffset = 3.dp)
                            .clickable(role = Role.Button) { navController.navigate("recognition?autoStart=true") },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.mic),
                        contentDescription = stringResource(R.string.baaja_home_identify_song),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .heightIn(min = 0.dp),
        ) {
            if (!isTyping) {
                ExploreLanding(
                    recentQueries = recentSearches,
                    onRecentClick = onSearch,
                    moods = exploreMoods,
                    contentPadding = bottomPadding,
                )
            } else {
                when (searchSource) {
                    SearchSource.LOCAL -> {
                        LocalSearchScreen(
                            query = query.text,
                            onDismiss = { navController.navigateUp() },
                            pureBlack = pureBlack,
                        )
                    }

                    SearchSource.ONLINE -> {
                        OnlineSearchScreen(
                            query = query.text,
                            onQueryChange = { query = it },
                            onSearch = onSearch,
                            onDismiss = { /* Don't dismiss when searching from suggestions */ },
                            pureBlack = pureBlack,
                        )
                    }
                }
            }
        }
    }

    // Handle lifecycle events to manage keyboard visibility
    DisposableEffect(lifecycleOwner, isPlayerExpanded) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        if (isHandlingScrollToTop) return@LifecycleEventObserver
                        // Always hide keyboard when resuming if player is expanded
                        if (isPlayerExpanded) {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    }

                    Lifecycle.Event.ON_PAUSE -> {
                        if (isHandlingScrollToTop) return@LifecycleEventObserver
                        // Clear focus when pausing to prevent keyboard from showing on resume
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }

                    else -> {}
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)

        // Initial check - hide keyboard if player is expanded
        if (isPlayerExpanded) {
            keyboardController?.hide()
            focusManager.clearFocus()
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
