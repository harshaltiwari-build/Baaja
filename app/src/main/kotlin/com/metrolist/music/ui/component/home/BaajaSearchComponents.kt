/**
 * Baaja Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import com.metrolist.music.constants.AppBarHeight
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.R
import com.metrolist.music.ui.theme.baajaInk
import com.metrolist.music.ui.theme.baajaOutline

/** Round icon-only button with a 44dp target. */
@Composable
fun BaajaPlainIconButton(
    @DrawableRes icon: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    iconSize: Dp = 22.dp,
) {
    Box(
        modifier =
            modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

/**
 * Baaja's outlined pill search field. The border turns accent while [highlighted], the hard ink
 * shadow stays. [trailing] hosts extra actions such as the library/online toggle.
 */
@Composable
fun BaajaSearchField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onSearch: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    focusRequester: FocusRequester? = null,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    height: Dp = 54.dp,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val ink = baajaInk
    val shape = CircleShape
    val textColor = MaterialTheme.colorScheme.onSurface
    Row(
        modifier =
            modifier
                .height(height)
                .baajaOutline(ink, shape, borderWidth = 2.dp, shadowOffset = 3.dp)
                .then(if (highlighted) Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.search),
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(22.dp),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = textColor, fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(value.text) }),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.text.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    inner()
                }
            },
            modifier =
                Modifier
                    .weight(1f)
                    .semantics { contentDescription = placeholder }
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                    .then(
                        if (onFocusChanged != null) {
                            Modifier.onFocusChanged { onFocusChanged(it.isFocused) }
                        } else {
                            Modifier
                        },
                    ),
        )
        if (value.text.isNotEmpty()) {
            BaajaPlainIconButton(
                icon = R.drawable.close,
                contentDescription = stringResource(R.string.baaja_home_clear_search),
                onClick = { onValueChange(TextFieldValue("")) },
                iconSize = 20.dp,
            )
        }
        trailing()
    }
}

/** Suggestion/history row: icon, query with the typed prefix in bold, optional remove and fill-in buttons. */
@Composable
fun BaajaSuggestionRow(
    query: String,
    typed: String,
    isHistory: Boolean,
    onClick: () -> Unit,
    onFillTextField: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
) {
    val divider = MaterialTheme.colorScheme.outlineVariant
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val strong = MaterialTheme.colorScheme.onSurface
    val text =
        buildAnnotatedString {
            if (typed.isNotEmpty() && query.startsWith(typed, ignoreCase = true)) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = strong)) { append(query.substring(0, typed.length)) }
                withStyle(SpanStyle(color = muted)) { append(query.substring(typed.length)) }
            } else {
                withStyle(SpanStyle(color = strong)) { append(query) }
            }
        }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable(onClick = onClick)
                .padding(start = BaajaHomeGutter, end = 10.dp)
                .drawBottomDivider(divider),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            painter = painterResource(if (isHistory) R.drawable.history else R.drawable.search),
            contentDescription = null,
            tint = muted,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = text,
            fontSize = 17.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (onDelete != null) {
            BaajaPlainIconButton(
                icon = R.drawable.close,
                contentDescription = stringResource(R.string.baaja_home_remove_from_recent, query),
                onClick = onDelete,
                tint = muted,
                iconSize = 18.dp,
            )
        }
        BaajaPlainIconButton(
            icon = R.drawable.arrow_top_left,
            contentDescription = stringResource(R.string.baaja_home_fill_in, query),
            onClick = onFillTextField,
            tint = muted,
            iconSize = 18.dp,
        )
    }
}

private fun Modifier.drawBottomDivider(color: Color): Modifier =
    drawBehind {
        val stroke = 1.5.dp.toPx()
        drawLine(
            color,
            Offset(0f, size.height - stroke / 2),
            Offset(size.width, size.height - stroke / 2),
            strokeWidth = stroke,
        )
    }

/** Recent-search pill shown on the Explore landing. */
@Composable
fun BaajaRecentChip(
    query: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .height(40.dp)
                .border(1.5.dp, baajaInk, CircleShape)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.history),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = query,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Header for browse pages (charts, moods, new releases…): back button (long-press goes home),
 * big bold title, optional actions. Draws its own surface so lists can scroll underneath.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BaajaBrowseTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onBackLongClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .height(AppBarHeight)
                .padding(start = 10.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .combinedClickable(
                        role = Role.Button,
                        onClick = onBack,
                        onLongClick = onBackLongClick,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.arrow_back),
                contentDescription = stringResource(R.string.baaja_home_back),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(26.dp),
            )
        }
        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.3).sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        actions()
    }
}
