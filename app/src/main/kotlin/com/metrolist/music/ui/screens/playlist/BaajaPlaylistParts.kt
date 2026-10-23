/**
 * Baaja (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Small Baaja pieces shared by the playlist screens and the history screen.
 */

package com.metrolist.music.ui.screens.playlist

import android.os.StatFs
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.ui.component.BaajaHeroCard
import com.metrolist.music.ui.component.BaajaTileColors
import com.metrolist.music.ui.theme.baajaInk
import com.metrolist.music.ui.theme.baajaOutline
import com.metrolist.music.ui.theme.baajaSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Joins the non-blank parts of a meta line with Baaja's middle dot. */
internal fun baajaMeta(vararg parts: String?): String = parts.filterNot { it.isNullOrBlank() }.joinToString(" · ")

/** Outlined pill that hosts the shared sort header (and optional trailing controls). */
@Composable
internal fun BaajaSortPill(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier =
            modifier
                .heightIn(min = 44.dp)
                .baajaSurface(baajaInk, MaterialTheme.colorScheme.surface, CircleShape, shadowOffset = 0.dp)
                .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** Row holding an optional title on the left and the sort pill on the right. */
@Composable
internal fun BaajaSortRow(
    modifier: Modifier = Modifier,
    title: String? = null,
    sort: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
        } else {
            Box(Modifier.weight(1f))
        }
        BaajaSortPill(content = sort)
    }
}

/** Outlined pill-shaped search field ("Find in liked songs"). */
@Composable
internal fun BaajaSearchPill(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    Row(
        modifier =
            modifier
                .height(48.dp)
                .baajaSurface(baajaInk, MaterialTheme.colorScheme.surface, CircleShape, shadowOffset = 0.dp)
                .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.search),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.text.isEmpty()) {
                Text(
                    text = placeholder,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp,
                    maxLines = 1,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = textColor, fontSize = 16.sp),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = placeholder }
                        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
            )
        }
        if (value.text.isNotEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { onValueChange(TextFieldValue()) },
            ) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = stringResource(R.string.clear),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** Card with a title, description and switch (e.g. "Pause history"). */
@Composable
internal fun BaajaSwitchCard(
    title: String,
    description: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .baajaOutline(baajaInk, shape, borderWidth = 2.dp, shadowOffset = 0.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            if (description != null) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors =
                SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedBorderColor = baajaInk,
                    uncheckedBorderColor = baajaInk,
                ),
        )
    }
}

/**
 * Yellow storage summary for Downloads: size of the download cache against free device space.
 * Reads `cacheSpace` once (a cheap in-memory counter) and StatFs off the main thread.
 */
@Composable
internal fun BaajaStorageCard(
    modifier: Modifier = Modifier,
    footer: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    val downloadCache = LocalPlayerConnection.current?.service?.downloadCache
    var used by remember { mutableLongStateOf(0L) }
    var free by remember { mutableLongStateOf(0L) }
    LaunchedEffect(downloadCache) {
        withContext(Dispatchers.IO) {
            used = runCatching { downloadCache?.cacheSpace ?: 0L }.getOrDefault(0L)
            free = runCatching { StatFs(context.filesDir.path).availableBytes }.getOrDefault(0L)
        }
    }
    val fraction = if (used + free > 0) used.toFloat() / (used + free) else 0f
    val usedText = Formatter.formatShortFileSize(context, used)
    val freeText = Formatter.formatShortFileSize(context, free)
    val percent = (fraction * 100).toInt()
    val barDescription = stringResource(R.string.baaja_library_storage_desc, usedText, percent)
    val ink = baajaInk

    BaajaHeroCard(containerColor = BaajaTileColors.downloads(), modifier = modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = usedText,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.baaja_library_storage_free, freeText),
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .baajaSurface(ink, MaterialTheme.colorScheme.surface, RoundedCornerShape(7.dp), shadowOffset = 0.dp)
                    .semantics { contentDescription = barDescription },
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceIn(if (used > 0) 0.03f else 0f, 1f))
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        footer()
    }
}
