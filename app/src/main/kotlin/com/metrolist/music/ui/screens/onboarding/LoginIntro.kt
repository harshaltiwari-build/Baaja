/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.R
import com.metrolist.music.ui.theme.BaajaInk
import com.metrolist.music.ui.theme.BaajaLogo
import com.metrolist.music.ui.theme.baajaInk
import com.metrolist.music.ui.theme.baajaSurface

/**
 * Intro shown before Google's sign-in page opens: what signing in gives you, then either
 * "Sign in with Google" (opens the existing web sign-in) or "Continue as guest".
 */
@Composable
internal fun BaajaLoginIntro(
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onContinueAsGuest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        val viewportHeight = maxHeight
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .heightIn(min = viewportHeight)
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Box(
                Modifier
                    .offset(x = (-12).dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = stringResource(R.string.back_button_desc),
                    modifier = Modifier.size(26.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                val logoDescription = stringResource(R.string.baaja_onb_logo_description)
                BaajaLogo(Modifier.semantics { contentDescription = logoDescription }, size = 72.dp)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.baaja_onb_login_title),
                        fontSize = 34.sp,
                        lineHeight = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        stringResource(R.string.baaja_onb_login_body),
                        fontSize = 17.sp,
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .baajaSurface(baajaInk, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp), shadowOffset = 0.dp)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                listOf(
                    R.string.baaja_onb_login_perk_sync,
                    R.string.baaja_onb_login_perk_mixes,
                    R.string.baaja_onb_login_perk_resume,
                ).forEach { perk ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(30.dp)
                                .baajaSurface(baajaInk, MaterialTheme.colorScheme.primary, CircleShape, shadowOffset = 0.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painterResource(R.drawable.check),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Text(stringResource(perk), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                BaajaCtaButton(
                    text = stringResource(R.string.baaja_onb_login_google),
                    onClick = onSignIn,
                    trailingIcon = null,
                    leading = {
                        Box(
                            Modifier
                                .size(30.dp)
                                .baajaSurface(BaajaInk, androidx.compose.ui.graphics.Color.White, CircleShape, shadowOffset = 0.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("G", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = BaajaInk)
                        }
                    },
                )
                BaajaCtaButton(
                    text = stringResource(R.string.baaja_onb_login_guest),
                    onClick = onContinueAsGuest,
                    primary = false,
                    trailingIcon = null,
                )
                Text(
                    stringResource(R.string.baaja_onb_login_note),
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
            }
        }
    }
}
