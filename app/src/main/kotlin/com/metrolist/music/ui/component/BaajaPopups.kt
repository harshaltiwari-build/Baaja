/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem as M3DropdownMenuItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties
import com.metrolist.music.ui.theme.baajaInk
import com.metrolist.music.ui.theme.baajaSurface
import androidx.compose.material3.DropdownMenu as M3DropdownMenu
import androidx.compose.material3.SnackbarHost as M3SnackbarHost

/** True inside the confirm slot of a Baaja dialog: [TextButton] then renders accent-filled. */
val LocalBaajaPrimaryButton = compositionLocalOf { false }

/** Card used by every Baaja dialog: surface colour, ink outline, hard offset shadow, 28 dp corners. */
@Composable
fun BaajaDialogSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    color: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .padding(end = 6.dp, bottom = 8.dp)
            .baajaSurface(baajaInk, color, shape, shadowOffset = 5.dp),
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            content()
        }
    }
}

/**
 * Pill button for dialogs. [primary] fills it with the accent colour; otherwise it is an outlined white pill.
 * [accent] overrides the text/fill colour (for example the error colour on destructive actions).
 */
@Composable
fun BaajaDialogButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
    accent: Color = Color.Unspecified,
    content: @Composable RowScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val fill = if (accent != Color.Unspecified) accent else scheme.primary
    val container = if (primary) fill else scheme.surface
    val contentColor =
        when {
            primary && accent != Color.Unspecified -> scheme.surface
            primary -> scheme.onPrimary
            accent != Color.Unspecified -> accent
            else -> scheme.onSurface
        }
    Row(
        modifier
            .defaultMinSize(minHeight = 44.dp)
            .baajaSurface(
                baajaInk,
                container.copy(alpha = if (enabled) 1f else 0.45f),
                CircleShape,
                shadowOffset = if (enabled) 3.dp else 0.dp,
            ).clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor.copy(alpha = if (enabled) 1f else 0.6f)) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp)) {
                content()
            }
        }
    }
}

/**
 * Drop-in replacement for Material3's TextButton: every dialog and menu button in the app inherits
 * Baaja's pill look. Inside a [LocalBaajaPrimaryButton] slot (a dialog's confirm button) it is accent-filled.
 */
@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    shapes: androidx.compose.material3.ButtonShapes? = null,
    elevation: androidx.compose.material3.ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val default = ButtonDefaults.textButtonColors().contentColor
    val custom = if (colors.contentColor != default && colors.contentColor != Color.Unspecified) colors.contentColor else Color.Unspecified
    BaajaDialogButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        primary = LocalBaajaPrimaryButton.current,
        accent = custom,
        content = content,
    )
}

/** Drop-in replacement for Material3's AlertDialog, drawn as a Baaja card with pill buttons. */
@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(28.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    iconContentColor: Color = MaterialTheme.colorScheme.primary,
    titleContentColor: Color = MaterialTheme.colorScheme.onSurface,
    textContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    properties: DialogProperties = DialogProperties(),
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties.let {
            DialogProperties(
                dismissOnBackPress = it.dismissOnBackPress,
                dismissOnClickOutside = it.dismissOnClickOutside,
                securePolicy = it.securePolicy,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = it.decorFitsSystemWindows,
            )
        },
    ) {
        BaajaDialogSurface(
            modifier = modifier.padding(24.dp).widthIn(max = 560.dp),
            shape = shape,
            color = containerColor,
        ) {
            Column(Modifier.padding(24.dp)) {
                if (icon != null) {
                    CompositionLocalProvider(LocalContentColor provides iconContentColor) {
                        Box(Modifier.align(Alignment.CenterHorizontally)) { icon() }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                if (title != null) {
                    CompositionLocalProvider(LocalContentColor provides titleContentColor) {
                        ProvideTextStyle(MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)) {
                            Box(Modifier.align(if (icon == null) Alignment.Start else Alignment.CenterHorizontally)) { title() }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                if (text != null) {
                    CompositionLocalProvider(LocalContentColor provides textContentColor) {
                        ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                            Box(Modifier.weight(1f, fill = false).align(Alignment.Start)) { text() }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                FlowRow(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (dismissButton != null) dismissButton()
                    CompositionLocalProvider(LocalBaajaPrimaryButton provides true) { confirmButton() }
                }
            }
        }
    }
}

/** Drop-in replacement for Material3's DropdownMenu: ink-outlined rounded surface. */
@Composable
fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    scrollState: androidx.compose.foundation.ScrollState = rememberScrollState(),
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    M3DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        scrollState = scrollState,
        properties = properties,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(2.dp, baajaInk),
        content = content,
    )
}

/** Drop-in replacement for Material3's DropdownMenuItem with bolder text. */
@Composable
fun DropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: androidx.compose.material3.MenuItemColors = MenuDefaults.itemColors(),
    contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding,
    interactionSource: MutableInteractionSource? = null,
) {
    M3DropdownMenuItem(
        text = {
            ProvideTextStyle(MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)) { text() }
        },
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        enabled = enabled,
        colors = colors,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
    )
}

/** Baaja snackbar: ink-outlined card with a hard shadow, accent action pill. */
@Composable
fun BaajaSnackbar(
    data: SnackbarData,
    modifier: Modifier = Modifier,
) {
    val visuals: SnackbarVisuals = data.visuals
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier
            .padding(start = 16.dp, end = 20.dp, top = 8.dp, bottom = 12.dp)
            .baajaSurface(baajaInk, scheme.surface, RoundedCornerShape(20.dp), shadowOffset = 4.dp)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            visuals.message,
            color = scheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f, fill = false).padding(vertical = 6.dp),
        )
        val label = visuals.actionLabel
        if (label != null) {
            Text(
                label,
                color = scheme.onPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier =
                    Modifier
                        .baajaSurface(baajaInk, scheme.primary, CircleShape, shadowOffset = 0.dp, borderWidth = 2.dp)
                        .clickable(role = Role.Button) { data.performAction() }
                        .defaultMinSize(minHeight = 40.dp)
                        .padding(horizontal = 16.dp, vertical = 9.dp),
            )
        }
    }
}

/** SnackbarHost that shows [BaajaSnackbar] unless the caller supplies its own snackbar. */
@Composable
fun SnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    snackbar: @Composable (SnackbarData) -> Unit = { BaajaSnackbar(it) },
) {
    M3SnackbarHost(hostState = hostState, modifier = modifier, snackbar = snackbar)
}

/** Corner shape shared by every Baaja bottom sheet. */
val BaajaSheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

/**
 * Draws Baaja's 2 dp ink outline along the top and sides of a bottom sheet. Apply to the sheet's
 * content column; the bottom edge is drawn off-screen and clipped by the sheet.
 */
@Composable
fun Modifier.baajaSheetOutline(): Modifier {
    val ink = baajaInk
    return drawBehind {
        val stroke = 4.dp.toPx()
        val outline =
            BaajaSheetShape.createOutline(
                androidx.compose.ui.geometry.Size(size.width, size.height + 400f),
                layoutDirection,
                this,
            )
        drawOutline(outline, ink, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
    }
}

/** Small drag handle pill drawn at the top of Baaja sheets. */
@Composable
fun BaajaSheetHandle(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(width = 44.dp, height = 6.dp)
                .baajaSurface(baajaInk, baajaInk.copy(alpha = 0.18f), CircleShape, shadowOffset = 0.dp, borderWidth = 1.5.dp),
        )
    }
}

/** Outlined card around the item (song, album, artist, playlist…) a menu was opened for. */
@Composable
fun BaajaMenuHeader(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(end = 4.dp, bottom = 4.dp, top = 2.dp)
            .baajaSurface(baajaInk, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp), shadowOffset = 3.dp)
            .padding(vertical = 2.dp),
    ) {
        content()
    }
}

/** Outlined text field colours: ink outline, accent when focused, surface fill. */
@Composable
fun baajaTextFieldColors(): androidx.compose.material3.TextFieldColors {
    val scheme = MaterialTheme.colorScheme
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = scheme.primary,
        unfocusedBorderColor = baajaInk,
        disabledBorderColor = baajaInk.copy(alpha = 0.4f),
        errorBorderColor = scheme.error,
        focusedContainerColor = scheme.surface,
        unfocusedContainerColor = scheme.surface,
        disabledContainerColor = scheme.surface,
        errorContainerColor = scheme.surface,
        focusedTextColor = scheme.onSurface,
        unfocusedTextColor = scheme.onSurface,
        focusedPlaceholderColor = scheme.onSurfaceVariant,
        unfocusedPlaceholderColor = scheme.onSurfaceVariant,
        cursorColor = scheme.primary,
    )
}

/** Ink-outlined card with a hard shadow for grouping content inside dialogs and sheets. */
@Composable
fun BaajaOutlinedCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .padding(end = 3.dp, bottom = 4.dp)
            .baajaSurface(baajaInk, color, RoundedCornerShape(20.dp), shadowOffset = 3.dp),
        content = content,
    )
}

/**
 * Card for custom dialogs built on BasicAlertDialog. Drop-in for the Material Card those dialogs used;
 * the shape/colour/elevation parameters are accepted for compatibility but the Baaja look wins.
 */
@Composable
fun BaajaDialogCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    colors: androidx.compose.material3.CardColors? = null,
    elevation: androidx.compose.material3.CardElevation? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    BaajaDialogSurface(modifier = modifier) {
        Column(content = content)
    }
}
