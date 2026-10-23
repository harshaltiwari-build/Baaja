/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val BaajaPurple = Color(0xFF7B4DFF)
val BaajaInk = Color(0xFF17151F)
val BaajaPink = Color(0xFFD7335A)
val BaajaYellow = Color(0xFFFFE066)

val BaajaLightColors: ColorScheme =
    lightColorScheme(
        primary = BaajaPurple,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD9CCFF),
        onPrimaryContainer = BaajaInk,
        inversePrimary = Color(0xFFCBB8FF),
        secondary = BaajaPink,
        onSecondary = Color.White,
        secondaryContainer = BaajaYellow,
        onSecondaryContainer = BaajaInk,
        tertiary = Color(0xFF0F766E),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFBDEFE6),
        onTertiaryContainer = BaajaInk,
        background = Color.White,
        onBackground = BaajaInk,
        surface = Color.White,
        onSurface = BaajaInk,
        surfaceVariant = Color(0xFFF4F2FA),
        onSurfaceVariant = Color(0xFF5E5A6B),
        surfaceTint = BaajaPurple,
        inverseSurface = BaajaInk,
        inverseOnSurface = Color.White,
        error = Color(0xFFB4233F),
        onError = Color.White,
        errorContainer = Color(0xFFFFD9DC),
        onErrorContainer = Color(0xFF410E0B),
        outline = BaajaInk,
        outlineVariant = Color(0xFFE6E3EE),
        scrim = Color.Black,
        surfaceBright = Color.White,
        surfaceDim = Color(0xFFE6E3EE),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFFAF9FC),
        surfaceContainer = Color(0xFFF4F2FA),
        surfaceContainerHigh = Color(0xFFEDEAF3),
        surfaceContainerHighest = Color(0xFFE6E3EE),
    )

val BaajaDarkColors: ColorScheme =
    darkColorScheme(
        primary = Color(0xFFA58BFF),
        onPrimary = BaajaInk,
        primaryContainer = Color(0xFF4A2FB0),
        onPrimaryContainer = Color(0xFFEDE7FF),
        inversePrimary = BaajaPurple,
        secondary = Color(0xFFFF8FA8),
        onSecondary = BaajaInk,
        secondaryContainer = Color(0xFF4A3F12),
        onSecondaryContainer = BaajaYellow,
        tertiary = Color(0xFF7FD8CE),
        onTertiary = BaajaInk,
        tertiaryContainer = Color(0xFF0F4D48),
        onTertiaryContainer = Color(0xFFBDEFE6),
        background = Color(0xFF121016),
        onBackground = Color(0xFFF4F2FA),
        surface = Color(0xFF121016),
        onSurface = Color(0xFFF4F2FA),
        surfaceVariant = Color(0xFF24212D),
        onSurfaceVariant = Color(0xFFC9C5D3),
        surfaceTint = Color(0xFFA58BFF),
        inverseSurface = Color(0xFFF4F2FA),
        inverseOnSurface = BaajaInk,
        error = Color(0xFFFFB2BA),
        onError = Color(0xFF561D26),
        errorContainer = Color(0xFF72333B),
        onErrorContainer = Color(0xFFFFD9DC),
        outline = Color(0xFFCFC9DC),
        outlineVariant = Color(0xFF3A3546),
        scrim = Color.Black,
        surfaceBright = Color(0xFF38343F),
        surfaceDim = Color(0xFF121016),
        surfaceContainerLowest = Color(0xFF0E0D12),
        surfaceContainerLow = Color(0xFF17151C),
        surfaceContainer = Color(0xFF1C1A22),
        surfaceContainerHigh = Color(0xFF24212D),
        surfaceContainerHighest = Color(0xFF2E2A38),
    )

/** Keeps album-art seeded light schemes on Baaja's white surfaces and ink outlines. */
fun ColorScheme.withBaajaLightSurfaces(): ColorScheme =
    copy(
        background = Color.White,
        surface = Color.White,
        surfaceContainerLowest = Color.White,
        onBackground = BaajaInk,
        onSurface = BaajaInk,
        outline = BaajaInk,
    )

val BaajaShapes =
    Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(24.dp),
        extraLarge = RoundedCornerShape(32.dp),
    )

val BaajaTypography: Typography =
    Typography().run {
        copy(
            displayLarge = displayLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp),
            displayMedium = displayMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
            displaySmall = displaySmall.copy(fontWeight = FontWeight.ExtraBold),
            headlineLarge = headlineLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
            headlineMedium = headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.25).sp),
            headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Bold),
            titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
            titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
            titleSmall = titleSmall.copy(fontWeight = FontWeight.SemiBold),
            labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
        )
    }

/**
 * Baaja's outlined "sticker" look: a solid border plus a hard offset shadow drawn behind the content.
 * Apply before clip/background so the shadow and border sit outside the clipped content.
 */
fun Modifier.baajaOutline(
    color: Color,
    shape: Shape,
    borderWidth: Dp = 2.dp,
    shadowOffset: Dp = 3.dp,
): Modifier = baajaShadow(color, shape, shadowOffset).border(borderWidth, color, shape)

/**
 * Just the hard offset shadow of [baajaOutline]. The outline is built once per size/shape in
 * [drawWithCache] instead of being re-created on every draw.
 */
fun Modifier.baajaShadow(
    color: Color,
    shape: Shape,
    shadowOffset: Dp = 3.dp,
): Modifier =
    if (shadowOffset <= 0.dp) {
        this
    } else {
        drawWithCache {
            val offset = shadowOffset.toPx()
            val outline = shape.createOutline(size, layoutDirection, this)
            onDrawBehind {
                translate(left = offset, top = offset * 1.33f) { drawOutline(outline, color) }
            }
        }
    }
