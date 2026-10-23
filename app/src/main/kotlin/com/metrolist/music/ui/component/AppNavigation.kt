/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import com.metrolist.music.ui.theme.baajaPressScale
import com.metrolist.music.ui.theme.rememberBaajaMotionEnabled
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metrolist.music.ui.screens.Screens
import com.metrolist.music.ui.theme.baajaOutline
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Stable
private fun isRouteSelected(currentRoute: String?, screenRoute: String, navigationItems: List<Screens>): Boolean {
    if (currentRoute == null) return false
    if (currentRoute == screenRoute) return true
    if (navigationItems.any { it.route == screenRoute } &&
        currentRoute.startsWith("$screenRoute/")) return true

    // Fix: match the route template, not the resolved route
    if (screenRoute == "search_input" &&
        (currentRoute.startsWith("search/") || currentRoute == "search/{query}")) return true

    return false
}

@Composable
fun AppNavigationRail(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    onSearchLongClick: (() -> Unit)? = null,
) {
    val containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
    val haptics = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current

    NavigationRail(
        modifier = modifier,
        containerColor = containerColor
    ) {
        Spacer(modifier = Modifier.weight(1f))

        navigationItems.forEach { screen ->
            val isSelected = remember(currentRoute, screen.route) {
                isRouteSelected(currentRoute, screen.route, navigationItems)
            }
            val currentIsSelected by rememberUpdatedState(isSelected)
            val iconRes = remember(isSelected, screen) {
                if (isSelected) screen.iconIdActive else screen.iconIdInactive
            }

            val isSearchItem = screen == Screens.Search && onSearchLongClick != null
            val interactionSource = remember { MutableInteractionSource() }

            // Long press detection using InteractionSource
            if (isSearchItem) {
                LaunchedEffect(interactionSource) {
                    var isLongClick = false
                    interactionSource.interactions.collectLatest { interaction ->
                        when (interaction) {
                            is PressInteraction.Press -> {
                                isLongClick = false
                                delay(viewConfiguration.longPressTimeoutMillis)
                                isLongClick = true
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSearchLongClick?.invoke()
                            }
                            is PressInteraction.Release -> {
                                if (!isLongClick) {
                                    onItemClick(screen, currentIsSelected)
                                }
                            }
                            is PressInteraction.Cancel -> {
                                isLongClick = false
                            }
                        }
                    }
                }
            }

            NavigationRailItem(
                selected = isSelected,
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                ),
                onClick = {
                    if (!isSearchItem) {
                        onItemClick(screen, currentIsSelected)
                    }
                    // Long presses are handled via InteractionSource
                },
                interactionSource = interactionSource,
                icon = {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = stringResource(screen.titleId)
                    )
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun AppNavigationBar(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    slimNav: Boolean = false,
    onSearchLongClick: (() -> Unit)? = null,
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = if (pureBlack) Color.Black else colorScheme.surface
    val pillColor = if (pureBlack) Color.Black else colorScheme.surfaceContainerLowest
    val outlineColor = if (pureBlack) Color.White else colorScheme.outline
    val haptics = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .baajaOutline(color = outlineColor, shape = CircleShape)
                .clip(CircleShape)
                .background(pillColor)
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navigationItems.forEach { screen ->
                val isSelected = remember(currentRoute, screen.route) {
                    isRouteSelected(currentRoute, screen.route, navigationItems)
                }
                val currentIsSelected by rememberUpdatedState(isSelected)
                val iconRes = remember(isSelected, screen) {
                    if (isSelected) screen.iconIdActive else screen.iconIdInactive
                }

                val isSearchItem = screen == Screens.Search && onSearchLongClick != null
                val interactionSource = remember { MutableInteractionSource() }

                // Long press detection using InteractionSource
                if (isSearchItem) {
                    LaunchedEffect(interactionSource) {
                        var isLongClick = false
                        interactionSource.interactions.collectLatest { interaction ->
                            when (interaction) {
                                is PressInteraction.Press -> {
                                    isLongClick = false
                                    delay(viewConfiguration.longPressTimeoutMillis)
                                    isLongClick = true
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSearchLongClick?.invoke()
                                }
                                is PressInteraction.Release -> {
                                    if (!isLongClick) {
                                        onItemClick(screen, currentIsSelected)
                                    }
                                }
                                is PressInteraction.Cancel -> {
                                    isLongClick = false
                                }
                            }
                        }
                    }
                }

                BaajaNavigationItem(
                    selected = isSelected,
                    iconRes = iconRes,
                    label = stringResource(screen.titleId),
                    showLabel = !slimNav,
                    pureBlack = pureBlack,
                    interactionSource = interactionSource,
                    onClick = {
                        if (!isSearchItem) {
                            onItemClick(screen, currentIsSelected)
                        }
                        // Long presses are handled via InteractionSource
                    },
                )
            }
        }
    }
}

@Composable
private fun RowScope.BaajaNavigationItem(
    selected: Boolean,
    @DrawableRes iconRes: Int,
    label: String,
    showLabel: Boolean,
    pureBlack: Boolean,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val expanded = selected && showLabel
    val contentColor = when {
        selected -> colorScheme.onPrimary
        pureBlack -> Color.White
        else -> colorScheme.onSurface
    }
    val motionEnabled = rememberBaajaMotionEnabled()
    // The accent pill grows into the selected tab and shrinks out of the old one, so it reads as sliding.
    val weight by animateFloatAsState(
        targetValue = if (expanded) 1.8f else 1f,
        animationSpec = if (motionEnabled) spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow) else snap(),
        label = "navItemWeight",
    )
    val background by animateColorAsState(
        targetValue = if (selected) colorScheme.primary else colorScheme.primary.copy(alpha = 0f),
        animationSpec = if (motionEnabled) tween(180) else snap(),
        label = "navItemBackground",
    )
    // Small bounce on the icon whenever this tab becomes selected.
    val iconScale = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        if (selected && motionEnabled) {
            iconScale.snapTo(0.7f)
            iconScale.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium))
        }
    }

    Row(
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight()
            .padding(vertical = 6.dp)
            .baajaPressScale(interactionSource)
            .clip(CircleShape)
            .drawBehind { drawRect(background) }
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = if (expanded) null else label,
            tint = contentColor,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    val s = iconScale.value
                    scaleX = s
                    scaleY = s
                },
        )
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(160, delayMillis = 60)) + expandHorizontally(tween(240, easing = FastOutSlowInEasing)),
            exit = fadeOut(tween(90)) + shrinkHorizontally(tween(200, easing = FastOutSlowInEasing)),
        ) {
            Text(
                text = label,
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
