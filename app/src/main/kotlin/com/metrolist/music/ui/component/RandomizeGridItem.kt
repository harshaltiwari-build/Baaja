package com.metrolist.music.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.R
import com.metrolist.music.ui.theme.BaajaInk
import com.metrolist.music.ui.theme.BaajaYellow
import com.metrolist.music.ui.theme.baajaInk
import com.metrolist.music.ui.theme.baajaOutline

/** "Surprise me" speed dial tile: a yellow Baaja card with an ink dice; shows a loader while picking. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RandomizeGridItem(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loadingAlpha by animateFloatAsState(
        targetValue = if (isLoading) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "loadingAlpha",
    )
    val shape = RoundedCornerShape(20.dp)

    Box(modifier = modifier.padding(end = 4.dp, bottom = 5.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .fillMaxSize()
                    .baajaOutline(baajaInk, shape, borderWidth = 2.dp, shadowOffset = 3.dp)
                    .clip(shape)
                    .background(BaajaYellow)
                    .clickable(role = Role.Button, onClick = onClick),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(8.dp).alpha(1f - loadingAlpha),
            ) {
                Icon(
                    painter = painterResource(R.drawable.baaja_sd_dice),
                    contentDescription = null,
                    tint = BaajaInk,
                    modifier = Modifier.size(36.dp),
                )
                Text(
                    text = stringResource(R.string.baaja_sd_surprise),
                    color = BaajaInk,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (loadingAlpha > 0f) {
                LoadingIndicator(
                    modifier = Modifier.size(44.dp).alpha(loadingAlpha),
                    color = BaajaInk,
                )
            }
        }
    }
}
