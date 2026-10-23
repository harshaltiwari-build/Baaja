/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.R
import com.metrolist.music.constants.BaajaProfileAvatarVersionKey
import com.metrolist.music.constants.BaajaProfileBioKey
import com.metrolist.music.constants.BaajaProfileNameKey
import com.metrolist.music.ui.theme.BaajaPillButton
import com.metrolist.music.ui.theme.baajaInk
import com.metrolist.music.ui.theme.baajaSurface
import com.metrolist.music.utils.safeDataStoreEdit
import kotlinx.coroutines.launch

private const val NAME_MAX = 30
private const val BIO_MAX = 80

/** Bottom sheet for editing the local profile (name, tagline, photo). Everything stays on this device. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileSheet(
    profile: BaajaProfile,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(if (profile.hasCustomName) profile.name else "") }
    var bio by rememberSaveable { mutableStateOf(profile.bio) }
    // Photo changes apply when saved: pickedPreview shows the new photo meanwhile.
    var pendingUri by rememberSaveable { mutableStateOf<String?>(null) }
    var removePhoto by rememberSaveable { mutableStateOf(false) }
    var saving by rememberSaveable { mutableStateOf(false) }
    var photoFailed by rememberSaveable { mutableStateOf(false) }

    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                pendingUri = uri.toString()
                removePhoto = false
                photoFailed = false
            }
        }

    val previewModel: Any? =
        when {
            removePhoto -> profile.remoteAvatarUrl
            pendingUri != null -> pendingUri
            else -> profile.avatarModel
        }
    val hasOwnPhoto = !removePhoto && (pendingUri != null || profile.avatar != null)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.baaja_onb_edit_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BaajaAvatar(
                    name = name.ifBlank { profile.name },
                    imageUrl = previewModel,
                    size = 84.dp,
                    shadowOffset = 3.dp,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BaajaSheetPill(stringResource(R.string.baaja_onb_edit_choose_photo)) {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    if (hasOwnPhoto) {
                        BaajaSheetPill(stringResource(R.string.baaja_onb_edit_remove_photo)) {
                            pendingUri = null
                            removePhoto = true
                        }
                    }
                }
            }
            if (photoFailed) {
                Text(
                    stringResource(R.string.baaja_onb_edit_photo_failed),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                )
            }

            ProfileField(
                value = name,
                onValueChange = { name = it.take(NAME_MAX) },
                label = stringResource(R.string.baaja_onb_edit_name),
                placeholder = profile.name,
                singleLine = true,
                imeAction = ImeAction.Next,
            )
            ProfileField(
                value = bio,
                onValueChange = { bio = it.take(BIO_MAX) },
                label = stringResource(R.string.baaja_onb_edit_bio),
                placeholder = stringResource(R.string.baaja_onb_edit_bio_hint),
                singleLine = false,
                imeAction = ImeAction.Done,
                counter = "${bio.length}/$BIO_MAX",
            )

            Text(
                stringResource(R.string.baaja_onb_edit_local_note),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BaajaPillButton(
                    text = stringResource(android.R.string.cancel),
                    onClick = onDismiss,
                    primary = false,
                    modifier = Modifier.weight(1f),
                )
                BaajaPillButton(
                    text = stringResource(R.string.save),
                    enabled = !saving,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        saving = true
                        scope.launch {
                            var version: Long? = null
                            val uri = pendingUri
                            if (uri != null) {
                                if (saveProfileAvatar(context, android.net.Uri.parse(uri))) {
                                    version = System.currentTimeMillis()
                                } else {
                                    photoFailed = true
                                    saving = false
                                    return@launch
                                }
                            } else if (removePhoto) {
                                deleteProfileAvatar(context)
                                version = 0L
                            }
                            context.safeDataStoreEdit {
                                it[BaajaProfileNameKey] = name.trim()
                                it[BaajaProfileBioKey] = bio.trim()
                                if (version != null) it[BaajaProfileAvatarVersionKey] = version
                            }
                            onDismiss()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun BaajaSheetPill(
    text: String,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .heightIn(min = 44.dp)
            .baajaSurface(baajaInk, MaterialTheme.colorScheme.surface, CircleShape, shadowOffset = 2.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
    }
}

@Composable
private fun ProfileField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    singleLine: Boolean,
    imeAction: ImeAction,
    counter: String? = null,
) {
    val ink = baajaInk
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, maxLines = 1) },
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 3,
        supportingText = counter?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = imeAction),
        shape = RoundedCornerShape(16.dp),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ink,
                unfocusedBorderColor = ink,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
        modifier = Modifier.fillMaxWidth(),
    )
}
