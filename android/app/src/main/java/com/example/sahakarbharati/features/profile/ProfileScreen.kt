package com.example.sahakarbharati.features.profile

import com.example.sahakarbharati.core.ui.occupationLabel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sahakarbharati.core.copy.AppLanguage
import com.example.sahakarbharati.core.copy.LocalCopy
import com.example.sahakarbharati.core.copy.LocalLanguage
import com.example.sahakarbharati.core.ui.ApplySystemBarIcons
import com.example.sahakarbharati.core.ui.DemoAlertDialog
import com.example.sahakarbharati.core.ui.UiState
import com.example.sahakarbharati.data.AppSession
import com.example.sahakarbharati.data.sahakarViewModel
import com.example.sahakarbharati.ui.navigation.RathYatraDestinations
import com.example.sahakarbharati.core.ui.LanguageChips
import com.example.sahakarbharati.core.ui.ProfilePhoto
import com.example.sahakarbharati.core.ui.RathBottomBar
import com.example.sahakarbharati.core.ui.SaffronButton
import com.example.sahakarbharati.core.ui.StackTopBar
import com.example.sahakarbharati.core.ui.StatusChip
import com.example.sahakarbharati.core.ui.WhiteCard
import com.example.sahakarbharati.ui.theme.RathYatraColors

@Composable
fun ProfileScreen(
    session: AppSession,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onOpenPass: () -> Unit,
    onSelectTab: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ApplySystemBarIcons(lightIcons = false)
    val copy = LocalCopy.current
    val hindi = LocalLanguage.current == AppLanguage.HI
    val context = LocalContext.current
    val profileViewModel: ProfileViewModel = sahakarViewModel()
    LaunchedEffect(Unit) { profileViewModel.load() }
    val profile = (profileViewModel.state as? UiState.Success)?.data
    var photoTick by rememberSaveable { mutableStateOf(0) }
    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            session.storePhoto(context, uri)
            photoTick += 1
        }
    }
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    val name = profile?.fullName?.ifBlank { session.name } ?: session.name.ifBlank { if (hindi) "प्रतिभागी" else "Participant" }
    val loginId = profile?.loginId?.ifBlank { session.loginId } ?: session.loginId.ifBlank { "—" }
    val mobile = profile?.mobile?.ifBlank { session.mobile } ?: session.mobile
    val initials = name.split(" ").filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "SB" }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = RathYatraColors.Ivory,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                StackTopBar(title = copy.profileTitle, onBack = onBack)
            }
        },
        bottomBar = {
            RathBottomBar(selectedKey = RathYatraDestinations.PROFILE, onSelect = onSelectTab)
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WhiteCard {
                Column {
                Row(
                    Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    key(photoTick) {
                        ProfilePhoto(
                            photoPath = session.displayPhotoPath,
                            initials = initials,
                            size = 72.dp,
                            onClick = {
                                pickPhoto.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.titleLarge, color = RathYatraColors.Navy, fontWeight = FontWeight.Bold)
                        Text(
                            if (hindi) "पंजीकृत प्रतिभागी" else "Registered participant",
                            style = MaterialTheme.typography.bodySmall,
                            color = RathYatraColors.OnSurfaceVariant,
                        )
                        Spacer(Modifier.height(6.dp))
                        StatusChip(
                            loginId,
                            container = RathYatraColors.PrimaryFixed,
                            content = RathYatraColors.BurntSaffron,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (hindi) "फ़ोटो केवल इस फ़ोन पर रहती है, सर्वर पर नहीं" else "Photo is kept on this phone only, not uploaded",
                            style = MaterialTheme.typography.bodySmall,
                            color = RathYatraColors.OnSurfaceVariant,
                        )
                    }
                }
                }
            }

            (profileViewModel.state as? UiState.Error)?.let { error ->
                // Shown cached details may be out of date; say so rather than implying a fresh load.
                Text(
                    text = error.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = RathYatraColors.Emergency,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            SectionLabel(if (hindi) "खाता" else "Account")
            ProfileRow(
                icon = Icons.Outlined.Badge,
                title = if (hindi) "प्रतिभागी आईडी" else "Participant ID",
                subtitle = loginId,
            )
            ProfileRow(
                icon = Icons.Outlined.PhoneIphone,
                title = if (hindi) "मोबाइल" else "Mobile",
                subtitle = if (mobile.length == 10) "+91 $mobile" else mobile.ifBlank { "—" },
            )
            ProfileRow(
                icon = Icons.Outlined.Badge,
                title = if (hindi) "व्यवसाय" else "Occupation",
                subtitle = occupationLabel(copy, profile?.occupation ?: session.occupation) ?: "—",
            )
            ProfileRow(
                icon = Icons.Outlined.QrCode2,
                title = if (hindi) "उत्सव पास" else "Celebration pass",
                subtitle = if (hindi) "पास और चेक-इन अभी उपलब्ध नहीं हैं" else "Pass and check-in are not available yet",
                onClick = onOpenPass,
            )

            SectionLabel(if (hindi) "सेटिंग्स" else "Settings")
            WhiteCard {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Outlined.Language, contentDescription = null, tint = RathYatraColors.Navy)
                    Column(Modifier.weight(1f)) {
                        Text(if (hindi) "भाषा" else "Language", style = MaterialTheme.typography.titleSmall, color = RathYatraColors.OnSurface)
                        Text(
                            if (hindi) "ऐप की भाषा बदलें" else "Change the app language",
                            style = MaterialTheme.typography.bodySmall,
                            color = RathYatraColors.OnSurfaceVariant,
                        )
                    }
                    LanguageChips(onDarkBackground = false)
                }
            }
            WhiteCard {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Outlined.Notifications, contentDescription = null, tint = RathYatraColors.Navy)
                    Column(Modifier.weight(1f)) {
                        Text(if (hindi) "सूचनाएँ" else "Notifications", style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (hindi) "सूचनाएँ अभी उपलब्ध नहीं हैं" else "Notifications are not available yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = RathYatraColors.OnSurfaceVariant,
                        )
                    }
                    // No notification service exists yet, so the switch stays off and disabled
                    // rather than pretending to subscribe the participant.
                    Switch(
                        checked = false,
                        onCheckedChange = null,
                        enabled = false,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = RathYatraColors.Saffron,
                            checkedThumbColor = RathYatraColors.NavyDeep,
                        ),
                    )
                }
            }

            SectionLabel(if (hindi) "सहायता" else "Support")
            ProfileRow(
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                title = if (hindi) "सहायता और SOS" else "Help and SOS",
                subtitle = if (hindi) "हेल्पडेस्क और आपात संपर्क" else "Help desk and emergency contact",
                onClick = { showHelp = true },
            )

            Spacer(Modifier.height(8.dp))
            SaffronButton(
                text = if (hindi) "लॉग आउट" else "Log out",
                onClick = { confirmLogout = true },
                trailingIcon = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = if (hindi) "लॉग आउट" else "Log out",
            )
        }
    }

    if (showHelp) {
        DemoAlertDialog(
            title = copy.sosTitle,
            body = copy.sosBody,
            onDismiss = { showHelp = false },
        )
    }
    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text(if (hindi) "लॉग आउट करें?" else "Log out?") },
            text = {
                Text(
                    if (hindi) {
                        "ऐप फिर से खोलने के लिए लॉगिन आईडी और पिन से साइन इन करना होगा।"
                    } else {
                        "You will need your login ID and PIN to open the app again."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmLogout = false
                        onLogout()
                    },
                ) {
                    Text(if (hindi) "लॉग आउट" else "Log out", color = RathYatraColors.Emergency)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) {
                    Text(if (hindi) "रद्द करें" else "Cancel", color = RathYatraColors.Navy)
                }
            },
            containerColor = RathYatraColors.White,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = RathYatraColors.GoldDark,
        modifier = Modifier.padding(top = 4.dp, start = 4.dp),
    )
}

@Composable
private fun ProfileRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
) {
    WhiteCard(onClick = onClick) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = RathYatraColors.Navy)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = RathYatraColors.OnSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
            }
            if (onClick != null) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = RathYatraColors.Outline)
            }
        }
    }
}
