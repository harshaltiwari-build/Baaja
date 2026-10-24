package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.music.R
import com.metrolist.music.ui.theme.BaajaInk
import com.metrolist.music.ui.theme.BaajaYellow
import com.metrolist.music.ui.theme.baajaInk
import com.metrolist.music.ui.theme.baajaOutline
import com.metrolist.music.ui.utils.resize

private val SpeedDialTileShape = RoundedCornerShape(20.dp)
private val SpeedDialLabelShape = RoundedCornerShape(10.dp)

/**
 * Speed dial tile: artwork in an ink-outlined rounded square (round for artists) with a hard offset
 * shadow and a small outlined label strip at the bottom. The active item turns its label accent.
 */
@Composable
fun SpeedDialGridItem(
    item: YTItem,
    isPinned: Boolean,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
) {
    val ink = baajaInk
    val isArtist = item is ArtistItem
    val artShape = if (isArtist) CircleShape else SpeedDialTileShape
    val scheme = MaterialTheme.colorScheme
    val labelColor = if (isActive) scheme.primary else scheme.surface
    val labelContent = if (isActive) scheme.onPrimary else scheme.onSurface

    // Right/bottom padding leaves room for the hard shadow inside the tile's bounds.
    Box(modifier = modifier.padding(end = 4.dp, bottom = 5.dp)) {
        ItemThumbnail(
            thumbnailUrl = remember(item.thumbnail) { item.thumbnail?.resize(200, 200) },
            isActive = isActive,
            isPlaying = isPlaying,
            shape = artShape,
            modifier = Modifier.fillMaxSize(),
        )

        Text(
            text = item.title,
            color = labelContent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = if (isArtist) 14.dp else 8.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .clip(SpeedDialLabelShape)
                    .background(labelColor)
                    .border(2.dp, ink, SpeedDialLabelShape)
                    .padding(horizontal = 6.dp, vertical = 3.dp),
        )

        if (isPinned) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .align(if (isArtist) Alignment.TopCenter else Alignment.TopEnd)
                        .padding(
                            top = if (isArtist) 0.dp else 6.dp,
                            end = if (isArtist) 0.dp else 6.dp,
                        )
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(BaajaYellow)
                        .border(2.dp, ink, CircleShape),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_push_pin),
                    contentDescription = stringResource(R.string.baaja_sd_pinned),
                    tint = BaajaInk,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}
