/**
 * Baaja Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.metrolist.music.ui.screens.onboarding.profileInitial
import com.metrolist.music.ui.screens.onboarding.rememberBaajaProfile
import com.metrolist.music.R
import com.metrolist.music.ui.theme.BaajaLogo
import com.metrolist.music.ui.theme.baajaInk

/**
 * Top app bar for the main tabs. On Home it shows the Baaja logo and wordmark centred between a
 * round profile avatar; other tabs show their title on the left.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaajaMainTopBar(
    isHome: Boolean,
    title: String,
    accountImageUrl: String?,
    showUpdateBadge: Boolean,
    onProfileClick: () -> Unit,
    onProfileLongClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    containerColor: Color,
    modifier: Modifier = Modifier,
) {
    val colors =
        TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            scrolledContainerColor = containerColor,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
        )
    val avatar: @Composable () -> Unit = {
        BaajaProfileAvatar(
            imageUrl = accountImageUrl,
            showBadge = showUpdateBadge,
            onClick = onProfileClick,
            onLongClick = onProfileLongClick,
        )
    }
    if (isHome) {
        CenterAlignedTopAppBar(
            title = { BaajaWordmark() },
            actions = {
                Row(
                    modifier = Modifier.padding(end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    avatar()
                }
            },
            scrollBehavior = scrollBehavior,
            colors = colors,
            modifier = modifier,
        )
    } else {
        TopAppBar(
            title = {
                Text(
                    text = title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    maxLines = 1,
                )
            },
            actions = {
                Row(
                    modifier = Modifier.padding(end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    avatar()
                }
            },
            scrollBehavior = scrollBehavior,
            colors = colors,
            modifier = modifier,
        )
    }
}

/** Baaja logo tile plus the lowercase "baaja" wordmark. */
@Composable
fun BaajaWordmark(modifier: Modifier = Modifier) {
    val logoDescription = stringResource(R.string.baaja_home_logo)
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = logoDescription },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BaajaLogo(size = 36.dp)
        Text(
            text = stringResource(R.string.baaja_home_wordmark),
            fontSize = 27.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.3).sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

/** Round yellow avatar with an ink outline; shows the account picture or a person icon. */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BaajaProfileAvatar(
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    showBadge: Boolean = false,
) {
    val description = stringResource(R.string.baaja_home_profile)
    val profile = rememberBaajaProfile()
    val avatarModel: Any? = profile.avatarModel ?: imageUrl
    BadgedBox(
        modifier = modifier,
        badge = { if (showBadge) Badge() },
    ) {
        Box(
            modifier =
                Modifier
                    .size(44.dp)
                    .border(2.dp, baajaInk, CircleShape)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .combinedClickable(
                        role = Role.Button,
                        onClickLabel = description,
                        onClick = onClick,
                        onLongClick = onLongClick,
                    ).semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            if (avatarModel != null) {
                AsyncImage(
                    model = avatarModel,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (profile.hasCustomName) {
                Text(
                    text = profileInitial(profile.name),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.account),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
