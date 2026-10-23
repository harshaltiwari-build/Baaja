package com.metrolist.music.ui.screens.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.music.R
import com.metrolist.music.constants.AccountChannelHandleKey
import com.metrolist.music.constants.AccountEmailKey
import com.metrolist.music.constants.AccountNameKey
import com.metrolist.music.constants.DataSyncIdKey
import com.metrolist.music.constants.InnerTubeAuthUserKey
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.VisitorDataKey
import com.metrolist.music.ui.component.InfoLabel
import com.metrolist.music.ui.component.TextFieldDialog
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.AccountSettingsViewModel

/** A Profile account row with a trailing switch (sync, "more content"). */
@Composable
internal fun ProfileSwitchRow(
    @DrawableRes icon: Int,
    label: String,
    badge: Color,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BaajaIconBadge(icon, badge, enabled = enabled)
        Text(label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

/** Paste/edit the YouTube Music login token (advanced login). Saves everything and restarts the app. */
@Composable
internal fun AdvancedLoginDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val accountSettingsViewModel: AccountSettingsViewModel = hiltViewModel()
    val (accountName, _) = rememberPreference(AccountNameKey, "")
    val (accountEmail, _) = rememberPreference(AccountEmailKey, "")
    val (accountChannelHandle, _) = rememberPreference(AccountChannelHandleKey, "")
    val (innerTubeCookie, _) = rememberPreference(InnerTubeCookieKey, "")
    val (visitorData, _) = rememberPreference(VisitorDataKey, "")
    val (dataSyncId, _) = rememberPreference(DataSyncIdKey, "")
    val (authUser, _) = rememberPreference(InnerTubeAuthUserKey, "0")

    val text =
        """
        ***INNERTUBE COOKIE*** =$innerTubeCookie
        ***VISITOR DATA*** =$visitorData
        ***DATASYNC ID*** =$dataSyncId
        ***AUTH USER*** =$authUser
        ***ACCOUNT NAME*** =$accountName
        ***ACCOUNT EMAIL*** =$accountEmail
        ***ACCOUNT CHANNEL HANDLE*** =$accountChannelHandle
        """.trimIndent()

    TextFieldDialog(
        initialTextFieldValue = TextFieldValue(text),
        onDone = { data ->
            var cookie = ""
            var visitorDataValue = ""
            var dataSyncIdValue = ""
            var authUserValue = "0"
            var accountNameValue = ""
            var accountEmailValue = ""
            var accountChannelHandleValue = ""

            data.split("\n").forEach {
                when {
                    it.startsWith("***INNERTUBE COOKIE*** =") -> cookie = it.substringAfter("=")
                    it.startsWith("***VISITOR DATA*** =") -> visitorDataValue = it.substringAfter("=")
                    it.startsWith("***DATASYNC ID*** =") -> dataSyncIdValue = it.substringAfter("=")
                    it.startsWith("***AUTH USER*** =") -> authUserValue = it.substringAfter("=")
                    it.startsWith("***ACCOUNT NAME*** =") -> accountNameValue = it.substringAfter("=")
                    it.startsWith("***ACCOUNT EMAIL*** =") -> accountEmailValue = it.substringAfter("=")
                    it.startsWith("***ACCOUNT CHANNEL HANDLE*** =") -> accountChannelHandleValue = it.substringAfter("=")
                }
            }
            // Writes all credentials to DataStore and waits for completion before restarting.
            accountSettingsViewModel.saveTokenAndRestart(
                context = context,
                cookie = cookie,
                visitorData = visitorDataValue,
                dataSyncId = dataSyncIdValue,
                authUser = authUserValue,
                accountName = accountNameValue,
                accountEmail = accountEmailValue,
                accountChannelHandle = accountChannelHandleValue,
            )
        },
        onDismiss = onDismiss,
        singleLine = false,
        maxLines = 20,
        isInputValid = { fullText ->
            // Validate the cookie value on its own line, not the whole multi-line template.
            val cookieValue =
                fullText.lines()
                    .find { it.startsWith("***INNERTUBE COOKIE*** =") }
                    ?.substringAfter("***INNERTUBE COOKIE*** =")
                    ?.trim()
                    ?: ""
            cookieValue.isNotEmpty() && "SAPISID" in parseCookieString(cookieValue)
        },
        extraContent = {
            Spacer(Modifier.height(8.dp))
            InfoLabel(text = stringResource(R.string.token_adv_login_description))
        },
    )
}
