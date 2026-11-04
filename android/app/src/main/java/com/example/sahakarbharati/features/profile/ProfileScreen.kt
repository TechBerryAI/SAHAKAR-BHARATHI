package com.example.sahakarbharati.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import com.example.sahakarbharati.core.copy.LocalCopy
import com.example.sahakarbharati.core.copy.LocalLanguage
import com.example.sahakarbharati.core.copy.blankToNull
import com.example.sahakarbharati.core.copy.formatMobile
import com.example.sahakarbharati.core.copy.participantCopy
import com.example.sahakarbharati.core.copy.personInitials
import com.example.sahakarbharati.data.model.ParticipantProfile
import com.example.sahakarbharati.data.model.isStaffRole
import com.example.sahakarbharati.data.remote.ProfileUpdateRequest
import com.example.sahakarbharati.data.rememberAppSession
import com.example.sahakarbharati.data.sahakarViewModel
import com.example.sahakarbharati.core.ui.InitialAvatar
import com.example.sahakarbharati.core.ui.SaffronButton
import com.example.sahakarbharati.core.ui.SignedInScaffold
import com.example.sahakarbharati.core.ui.StatusChip
import com.example.sahakarbharati.core.ui.UiState
import com.example.sahakarbharati.core.ui.WhiteCard
import com.example.sahakarbharati.core.ui.occupationLabel
import com.example.sahakarbharati.ui.navigation.RathYatraDestinations
import com.example.sahakarbharati.ui.theme.RathYatraColors

private val occupations = listOf("Farmer", "Fisherman", "Both", "Allied")
private val DeepGreen = Color(0xFF0F3D2E)
private val Ink = Color(0xFF1A1C19)
private val Muted = Color(0xFF5C665F)
private val FieldOutline = Color(0xFFD4CFC6)
private val SaffronPressed = Color(0xFFC9741F)
private val ChipSaffron = Color(0x29E9872A)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onSelectTab: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val copy = LocalCopy.current
    val text = participantCopy(LocalLanguage.current)
    val session = rememberAppSession()
    val viewModel: ProfileViewModel = sahakarViewModel()
    val loaded = (viewModel.state as? UiState.Success)?.data ?: session.cachedProfile()
    val staff = isStaffRole(loaded?.role ?: session.role())
    var fullName by rememberSaveable { mutableStateOf("") }
    var occupation by rememberSaveable { mutableStateOf("") }
    var addressLine by rememberSaveable { mutableStateOf("") }
    var locality by rememberSaveable { mutableStateOf("") }
    var district by rememberSaveable { mutableStateOf("") }
    var stateName by rememberSaveable { mutableStateOf("") }
    var pincode by rememberSaveable { mutableStateOf("") }
    var dirty by rememberSaveable { mutableStateOf(false) }
    var formError by rememberSaveable { mutableStateOf("") }
    var confirmLogout by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(loaded) {
        val profile = loaded ?: return@LaunchedEffect
        if (!dirty) {
            fullName = profile.fullName
            occupation = profile.occupation.orEmpty()
            addressLine = profile.addressLine.orEmpty()
            locality = profile.locality.orEmpty()
            district = profile.district.orEmpty()
            stateName = profile.state.orEmpty()
            pincode = profile.pincode.orEmpty()
        }
    }

    val banner = formError.ifBlank {
        viewModel.saveError ?: viewModel.refreshError ?: (viewModel.state as? UiState.Error)?.message.orEmpty()
    }
    val loading = viewModel.state is UiState.Loading && loaded == null

    SignedInScaffold(
        title = copy.tabProfile,
        selectedTab = RathYatraDestinations.PROFILE,
        showAdmin = staff,
        onSelectTab = onSelectTab,
        modifier = modifier,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (loading) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RathYatraColors.Saffron)
                }
            } else {
                PersonHeader(loaded, text.registeredParticipant, copy)
                AccountPanel(text, loaded)
                if (banner.isNotBlank()) {
                    Text(banner, color = RathYatraColors.Emergency, style = MaterialTheme.typography.bodyMedium)
                }
                if (viewModel.saved && formError.isBlank() && viewModel.saveError == null) {
                    Text(text.saved, color = RathYatraColors.Success, style = MaterialTheme.typography.bodyMedium)
                }
                WhiteCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PanelTitle(text.personalDetails, Icons.Outlined.Person)
                        ProfileField(text.fullName, fullName) {
                            fullName = it
                            dirty = true
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            FieldLabel(copy.occupation)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                occupations.forEach { code ->
                                    OccupationChip(
                                        label = occupationLabel(copy, code) ?: code,
                                        selected = occupation == code,
                                        onClick = {
                                            occupation = if (occupation == code) "" else code
                                            dirty = true
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
                WhiteCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PanelTitle(text.addressTitle, Icons.Outlined.Place)
                        ProfileField(text.address, addressLine) { addressLine = it; dirty = true }
                        ProfileField(text.locality, locality) { locality = it; dirty = true }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.weight(1f)) {
                                ProfileField(text.district, district) { district = it; dirty = true }
                            }
                            Box(Modifier.weight(1f)) {
                                ProfileField(text.stateName, stateName) { stateName = it; dirty = true }
                            }
                        }
                        ProfileField(text.pincode, pincode, KeyboardType.Number) {
                            pincode = it.filter(Char::isDigit).take(6)
                            dirty = true
                        }
                    }
                }
                SaffronButton(
                    text = if (viewModel.saving) text.saving else text.saveChanges,
                    onClick = {
                        if (viewModel.saving) return@SaffronButton
                        if (fullName.trim().length < 2) {
                            formError = text.fullNameRequired
                            return@SaffronButton
                        }
                        if (pincode.isNotEmpty() && !Regex("^\\d{6}$").matches(pincode)) {
                            formError = text.pinDigits
                            return@SaffronButton
                        }
                        formError = ""
                        dirty = false
                        viewModel.save(
                            ProfileUpdateRequest(
                                fullName = fullName.trim(),
                                occupation = occupation.ifBlank { null },
                                addressLine = blankToNull(addressLine),
                                locality = blankToNull(locality),
                                district = blankToNull(district),
                                state = blankToNull(stateName),
                                pincode = blankToNull(pincode),
                            ),
                        )
                    },
                )
                Surface(
                    onClick = { confirmLogout = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = RathYatraColors.White,
                    shadowElevation = 1.dp,
                ) {
                    Row(
                        Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = null,
                            tint = RathYatraColors.Emergency,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text.logOut,
                            modifier = Modifier.padding(start = 8.dp),
                            color = RathYatraColors.Emergency,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text(text.logOutConfirm) },
            text = { Text(text.logOutBody) },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    onLogout()
                }) {
                    Text(text.logOut, color = RathYatraColors.Emergency)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) {
                    Text(text.cancel)
                }
            },
            containerColor = RathYatraColors.White,
        )
    }
}

@Composable
private fun PersonHeader(profile: ParticipantProfile?, roleLine: String, copy: com.example.sahakarbharati.core.copy.RathYatraCopy) {
    val name = profile?.fullName.orEmpty()
    WhiteCard {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InitialAvatar(personInitials(name), size = 72.dp, fontSize = 22.sp)
            Column {
                Text(name.ifBlank { roleLine }, color = Color(0xFF17263F), fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                Text(roleLine, color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
                occupationLabel(copy, profile?.occupation)?.let { label ->
                    Box(Modifier.padding(top = 8.dp)) {
                        StatusChip(label, container = ChipSaffron, content = SaffronPressed)
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountPanel(text: com.example.sahakarbharati.core.copy.ParticipantCopy, profile: ParticipantProfile?) {
    WhiteCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PanelTitle(text.account, Icons.Outlined.Shield)
            InfoLine(Icons.Outlined.Badge, text.loginId, profile?.loginId?.ifBlank { null } ?: "—")
            InfoLine(Icons.Outlined.Phone, text.mobile, profile?.mobile?.let(::formatMobile) ?: "—")
            InfoLine(Icons.Outlined.Email, text.email, profile?.email?.ifBlank { null } ?: "—")
        }
    }
}

@Composable
private fun PanelTitle(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .size(32.dp)
                .background(ChipSaffron, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = SaffronPressed, modifier = Modifier.size(18.dp))
        }
        Text(title, color = Color(0xFF17263F), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun InfoLine(icon: ImageVector, label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .background(ChipSaffron, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = SaffronPressed, modifier = Modifier.size(18.dp))
        }
        Column {
            Text(label, fontSize = 12.sp, color = Muted)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Ink)
        }
    }
}

@Composable
private fun FieldLabel(label: String) {
    Text(
        label.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.4.sp,
        color = DeepGreen,
    )
}

@Composable
private fun OccupationChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = if (selected) ChipSaffron else Color.White,
        border = BorderStroke(1.dp, if (selected) RathYatraColors.Saffron else FieldOutline),
        modifier = Modifier.height(40.dp),
    ) {
        Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (selected) SaffronPressed else Ink,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(label)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(1.dp, FieldOutline, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp),
            textStyle = TextStyle(color = Ink, fontSize = 16.sp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            cursorBrush = SolidColor(RathYatraColors.Saffron),
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) { inner() }
            },
        )
    }
}
