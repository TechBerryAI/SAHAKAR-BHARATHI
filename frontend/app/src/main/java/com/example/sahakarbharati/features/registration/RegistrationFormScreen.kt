package com.example.sahakarbharati.features.registration

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Female
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Male
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Transgender
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.sahakarbharati.core.copy.AppLanguage
import com.example.sahakarbharati.core.copy.LocalCopy
import com.example.sahakarbharati.core.copy.LocalLanguage
import com.example.sahakarbharati.core.copy.RathYatraCopy
import com.example.sahakarbharati.core.ui.ApplySystemBarIcons
import com.example.sahakarbharati.core.ui.ProfilePhoto
import com.example.sahakarbharati.core.ui.SaffronButton
import com.example.sahakarbharati.core.ui.UiState
import com.example.sahakarbharati.data.remote.RegistrationRequest
import com.example.sahakarbharati.data.rememberAppSession
import com.example.sahakarbharati.data.sahakarViewModel
import com.example.sahakarbharati.core.ui.StackTopBar
import com.example.sahakarbharati.core.ui.StatusChip
import com.example.sahakarbharati.core.ui.WhiteCard
import com.example.sahakarbharati.ui.theme.FieldShape
import com.example.sahakarbharati.ui.theme.RathYatraColors
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationFormScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ApplySystemBarIcons(lightIcons = false)
    val copy = LocalCopy.current
    val language = LocalLanguage.current
    val extra = remember(language) { formExtraCopy(language) }
    val context = LocalContext.current
    val session = rememberAppSession()
    val scope = rememberCoroutineScope()
    val viewModel: RegistrationViewModel = sahakarViewModel()

    var step by rememberSaveable { mutableStateOf(0) }
    var showSos by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var dob by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf("") }
    var occupation by rememberSaveable { mutableStateOf("") }
    var mobile by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var addressLine by rememberSaveable { mutableStateOf("") }
    var locality by rememberSaveable { mutableStateOf("") }
    var district by rememberSaveable { mutableStateOf("") }
    var stateName by rememberSaveable { mutableStateOf("") }
    var pincode by rememberSaveable { mutableStateOf("") }
    var pin by rememberSaveable { mutableStateOf("") }
    var confirmPin by rememberSaveable { mutableStateOf("") }
    var locating by rememberSaveable { mutableStateOf(false) }
    var locationNote by rememberSaveable { mutableStateOf("") }
    var stepError by rememberSaveable { mutableStateOf("") }
    var photoPath by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(viewModel.state) {
        val current = viewModel.state
        if (current is UiState.Success) {
            viewModel.consumeSuccess()
            onContinue()
        }
        if (current is UiState.Error) {
            stepError = current.message
        }
    }

    val ageYears = remember(dob) { ageYearsFromDob(dob) }
    val ageText = if (language == AppLanguage.HI) "$ageYears वर्ष" else "$ageYears Years"
    val titles = listOf(copy.formTitle, extra.contactTitle, extra.profileTitle, extra.securityTitle)
    val stepLabels = listOf(copy.stepLabel, extra.contactStep, extra.profileStep, extra.securityStep)

    fun goBack() {
        stepError = ""
        if (step == 0) onBack() else step -= 1
    }

    fun fillFromLocation() {
        locating = true
        locationNote = extra.locatingAddress
        scope.launch {
            val result = resolveDeviceAddress(context)
            locating = false
            locationNote = if (result.found) extra.locationFilled else extra.locationUnavailable
            result.line?.let { addressLine = it }
            result.locality?.let { locality = it }
            result.district?.let { district = it }
            result.state?.let { stateName = it }
            result.pincode?.let { pincode = it }
        }
    }

    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            session.storePhoto(context, uri)?.let { photoPath = it }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.values.any { it }) {
            fillFromLocation()
        } else {
            locating = false
            locationNote = extra.locationDenied
        }
    }

    fun useCurrentLocation() {
        stepError = ""
        if (hasLocationPermission(context)) {
            fillFromLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    fun advance() {
        stepError = when (step) {
            0 -> when {
                name.isBlank() -> extra.nameRequired
                gender.isBlank() -> extra.genderRequired
                dob.isBlank() -> extra.dobRequired
                ageYears in 1..17 -> extra.ageTooYoung
                occupation.isBlank() -> extra.occupationRequired
                else -> ""
            }
            1 -> when {
                mobile.filter(Char::isDigit).length != 10 -> extra.mobileRequired
                else -> ""
            }
            2 -> when {
                addressLine.isBlank() && locality.isBlank() -> extra.addressRequired
                else -> ""
            }
            else -> when {
                pin.length < 4 -> extra.pinRequired
                pin != confirmPin -> extra.pinMismatch
                else -> ""
            }
        }
        if (stepError.isNotEmpty()) return
        if (step < 3) {
            step += 1
        } else {
            viewModel.submit(
                RegistrationRequest(
                    fullName = name,
                    gender = gender.ifBlank { null },
                    dateOfBirth = isoDob(dob),
                    occupation = occupation.ifBlank { null },
                    mobile = mobile,
                    email = email.ifBlank { null },
                    addressLine = addressLine.ifBlank { null },
                    locality = locality.ifBlank { null },
                    district = district.ifBlank { null },
                    state = stateName.ifBlank { null },
                    pincode = pincode.ifBlank { null },
                    pin = pin,
                ),
            )
        }
    }

    BackHandler(onBack = { goBack() })

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RathYatraColors.Ivory)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .windowInsetsPadding(WindowInsets.ime),
    ) {
        StackTopBar(
            title = titles[step],
            onBack = { goBack() },
            onSosClick = { showSos = true },
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(RathYatraColors.Saffron, RathYatraColors.Gold),
                    ),
                ),
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WhiteCard {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatusChip(
                            stepLabels[step],
                            container = RathYatraColors.PrimaryFixed,
                            content = RathYatraColors.BurntSaffron,
                            leadingDot = RathYatraColors.Saffron,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.VerifiedUser, null, Modifier.size(14.dp), RathYatraColors.Navy)
                            Text(" ${copy.govtPortal}", style = MaterialTheme.typography.labelSmall, color = RathYatraColors.Navy)
                        }
                    }
                    LinearProgressIndicator(
                        progress = { (step + 1) / 4f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = RathYatraColors.Saffron,
                        trackColor = RathYatraColors.SurfaceContainerHighest,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf(copy.stepPersonal, copy.stepContact, copy.stepProfile, copy.stepSecurity).forEachIndexed { index, label ->
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (index == step) RathYatraColors.Saffron else RathYatraColors.Outline,
                            )
                        }
                    }
                }
            }

            when (step) {
                0 -> PersonalStep(
                    copy = copy,
                    name = name,
                    onName = { name = it },
                    gender = gender,
                    onGender = { gender = it },
                    dob = dob,
                    onDobChange = { dob = it },
                    ageText = ageText,
                    ageYears = ageYears,
                    occupation = occupation,
                    onOccupation = { occupation = it },
                )
                1 -> ContactStep(
                    extra = extra,
                    mobile = mobile,
                    onMobile = {
                        mobile = it.filter(Char::isDigit).take(10)
                        if (stepError == extra.mobileRequired) stepError = ""
                    },
                    mobileError = if (stepError == extra.mobileRequired) stepError else null,
                    email = email,
                    onEmail = { email = it },
                )
                2 -> AddressStep(
                    extra = extra,
                    name = name,
                    photoPath = photoPath,
                    onPickPhoto = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    addressLine = addressLine,
                    onAddressLine = { addressLine = it },
                    locality = locality,
                    onLocality = { locality = it },
                    district = district,
                    onDistrict = { district = it },
                    stateName = stateName,
                    onState = { stateName = it },
                    pincode = pincode,
                    onPincode = { pincode = it.filter(Char::isDigit).take(6) },
                    locating = locating,
                    locationNote = locationNote,
                    onUseLocation = { useCurrentLocation() },
                )
                else -> SecurityStep(
                    extra = extra,
                    pin = pin,
                    onPin = { pin = it.filter(Char::isDigit).take(6) },
                    confirmPin = confirmPin,
                    onConfirmPin = { confirmPin = it.filter(Char::isDigit).take(6) },
                )
            }

            WhiteCard {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.PrivacyTip, contentDescription = null, tint = RathYatraColors.Navy)
                    Column {
                        Text(copy.privacyTitle, style = MaterialTheme.typography.titleSmall)
                        Text(
                            copy.privacyBody,
                            style = MaterialTheme.typography.bodySmall,
                            color = RathYatraColors.OnSurfaceVariant,
                        )
                    }
                }
            }
        }

        Column(
            Modifier
                .background(RathYatraColors.Ivory.copy(alpha = 0.96f))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (stepError.isNotEmpty()) {
                Text(
                    stepError,
                    style = MaterialTheme.typography.bodySmall,
                    color = RathYatraColors.Emergency,
                )
            }
            if (viewModel.state is UiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = RathYatraColors.Saffron,
                    strokeWidth = 3.dp,
                )
            } else {
                SaffronButton(
                    text = if (step == 3) copy.createAccount else copy.continueNext,
                    onClick = { advance() },
                    trailingIcon = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = if (step == 3) copy.createAccount else copy.continueNext,
                )
            }
        }
    }
    if (showSos) {
        com.example.sahakarbharati.core.ui.DemoAlertDialog(
            title = copy.sosTitle,
            body = copy.sosBody,
            onDismiss = { showSos = false },
        )
    }
}

@Composable
private fun ColumnScope.PersonalStep(
    copy: RathYatraCopy,
    name: String,
    onName: (String) -> Unit,
    gender: String,
    onGender: (String) -> Unit,
    dob: String,
    onDobChange: (String) -> Unit,
    ageText: String,
    ageYears: Int,
    occupation: String,
    onOccupation: (String) -> Unit,
) {
    WhiteCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(copy.legalName, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.OnSurfaceVariant)
            OutlinedTextField(
                value = name,
                onValueChange = onName,
                modifier = Modifier.fillMaxWidth(),
                shape = FieldShape,
                colors = fieldColors(),
            )
            Text(copy.nameHint, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
        }
    }
    WhiteCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(copy.gender, style = MaterialTheme.typography.labelSmall)
                StatusChip(copy.mandatory, container = RathYatraColors.PrimaryFixed, content = RathYatraColors.BurntSaffron)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GenderChip(copy.male, Icons.Outlined.Male, gender == "Male", Modifier.weight(1f)) { onGender("Male") }
                GenderChip(copy.female, Icons.Outlined.Female, gender == "Female", Modifier.weight(1f)) { onGender("Female") }
                GenderChip(copy.other, Icons.Outlined.Transgender, gender == "Other", Modifier.weight(1f)) { onGender("Other") }
            }
        }
    }
    WhiteCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(copy.dob, style = MaterialTheme.typography.labelSmall)
            BirthDateFields(dob = dob, onDobChange = onDobChange)
            Text(copy.ageMin, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
            if (dob.isNotBlank()) {
                StatusChip(ageText, container = RathYatraColors.GoldPale, content = RathYatraColors.GoldDark)
                if (ageYears >= 18) {
                    Text(copy.eligible, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.Success)
                }
            }
        }
    }
    WhiteCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(copy.occupation, style = MaterialTheme.typography.labelSmall)
            Text(copy.occupationHint, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
            OccupationDropdown(
                occupation = occupation,
                options = listOf(
                    "Farmer" to copy.farmer,
                    "Fisherman" to copy.fisherman,
                    "Both" to copy.both,
                    "Allied" to copy.allied,
                ),
                onOccupation = onOccupation,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OccupationDropdown(
    occupation: String,
    options: List<Pair<String, String>>,
    onOccupation: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == occupation }?.second.orEmpty()
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            shape = FieldShape,
            placeholder = {
                Text("Select", color = RathYatraColors.Outline)
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = fieldColors(),
        )
        Box(
            Modifier
                .matchParentSize()
                .clickable { expanded = !expanded },
        )
        DownwardMenu(expanded = expanded, onDismiss = { expanded = false }) {
            options.forEach { (key, label) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = label,
                            color = if (key == occupation) RathYatraColors.BurntSaffron else RathYatraColors.Navy,
                            fontWeight = if (key == occupation) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        onOccupation(key)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.ContactStep(
    extra: FormExtraCopy,
    mobile: String,
    onMobile: (String) -> Unit,
    mobileError: String?,
    email: String,
    onEmail: (String) -> Unit,
) {
    WhiteCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(extra.contactCard, style = MaterialTheme.typography.titleSmall, color = RathYatraColors.Navy)
            Text(extra.contactBody, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
        }
    }
    LabeledField(
        label = extra.mobileLabel,
        value = mobile,
        onValueChange = onMobile,
        hint = extra.mobileHint,
        error = mobileError,
        keyboardType = KeyboardType.Phone,
        prefix = "+91",
    )
    LabeledField(
        label = extra.emailLabel,
        value = email,
        onValueChange = onEmail,
        hint = extra.emailHint,
        keyboardType = KeyboardType.Email,
    )
}

@Composable
private fun ColumnScope.AddressStep(
    extra: FormExtraCopy,
    name: String,
    photoPath: String,
    onPickPhoto: () -> Unit,
    addressLine: String,
    onAddressLine: (String) -> Unit,
    locality: String,
    onLocality: (String) -> Unit,
    district: String,
    onDistrict: (String) -> Unit,
    stateName: String,
    onState: (String) -> Unit,
    pincode: String,
    onPincode: (String) -> Unit,
    locating: Boolean,
    locationNote: String,
    onUseLocation: () -> Unit,
) {
    ProfilePhotoCard(
        title = extra.photoTitle,
        hint = extra.photoHint,
        galleryLabel = extra.photoGallery,
        name = name,
        photoPath = photoPath,
        onPickPhoto = onPickPhoto,
    )
    WhiteCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    extra.profileCard,
                    style = MaterialTheme.typography.titleMedium,
                    color = RathYatraColors.Navy,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    extra.profileBody,
                    style = MaterialTheme.typography.bodyMedium,
                    color = RathYatraColors.OnSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(RathYatraColors.PrimaryFixed)
                    .clickable(enabled = !locating, onClick = onUseLocation)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(RathYatraColors.White),
                    contentAlignment = Alignment.Center,
                ) {
                    if (locating) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = RathYatraColors.Saffron, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.MyLocation, contentDescription = extra.useMyLocation, tint = RathYatraColors.BurntSaffron)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(extra.useMyLocation, style = MaterialTheme.typography.titleSmall, color = RathYatraColors.Navy)
                    Text(extra.useMyLocationHint, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
                }
            }
            if (locationNote.isNotEmpty()) {
                Text(locationNote, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.Navy)
            }
            AddressField(extra.houseLabel, addressLine, onAddressLine, extra.houseHint)
            AddressField(extra.localityLabel, locality, onLocality)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) {
                    AddressField(extra.districtLabel, district, onDistrict)
                }
                Box(Modifier.weight(1f)) {
                    AddressField(extra.stateLabel, stateName, onState)
                }
            }
            AddressField(extra.pincodeLabel, pincode, onPincode, keyboardType = KeyboardType.Number)
        }
    }
}

@Composable
private fun ColumnScope.ProfilePhotoCard(
    title: String,
    hint: String,
    galleryLabel: String,
    name: String,
    photoPath: String,
    onPickPhoto: () -> Unit,
) {
    WhiteCard {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.OnSurfaceVariant)
            ProfilePhoto(
                photoPath = photoPath,
                initials = name.split(" ").filter { it.isNotBlank() }.take(2)
                    .joinToString("") { it.first().uppercase() }
                    .ifBlank { "SB" },
                size = 88.dp,
                onClick = onPickPhoto,
            )
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = RathYatraColors.OnSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.clickable(onClick = onPickPhoto),
            ) {
                Icon(
                    Icons.Outlined.AddAPhoto,
                    contentDescription = galleryLabel,
                    tint = RathYatraColors.Navy,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    galleryLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = RathYatraColors.Navy,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.SecurityStep(
    extra: FormExtraCopy,
    pin: String,
    onPin: (String) -> Unit,
    confirmPin: String,
    onConfirmPin: (String) -> Unit,
) {
    WhiteCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(extra.securityCard, style = MaterialTheme.typography.titleSmall, color = RathYatraColors.Navy)
            Text(extra.securityBody, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
        }
    }
    LabeledField(
        label = extra.pinLabel,
        value = pin,
        onValueChange = onPin,
        hint = extra.pinHint,
        keyboardType = KeyboardType.NumberPassword,
        password = true,
    )
    LabeledField(
        label = extra.confirmPinLabel,
        value = confirmPin,
        onValueChange = onConfirmPin,
        keyboardType = KeyboardType.NumberPassword,
        password = true,
    )
}

@Composable
private fun AddressField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = RathYatraColors.Navy, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = FieldShape,
            colors = fieldColors(),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
        )
        if (!hint.isNullOrBlank()) {
            Text(hint, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
        }
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String? = null,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    prefix: String? = null,
    password: Boolean = false,
) {
    WhiteCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.OnSurfaceVariant)
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                shape = FieldShape,
                colors = fieldColors(isError = error != null),
                isError = error != null,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                prefix = prefix?.let { { Text(it, color = RathYatraColors.OnSurfaceVariant) } },
                singleLine = true,
            )
            val message = error ?: hint
            if (!message.isNullOrBlank()) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (error != null) RathYatraColors.Emergency else RathYatraColors.OnSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun GenderChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) RathYatraColors.Navy else RathYatraColors.SurfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) Color.White else RathYatraColors.Navy, modifier = Modifier.size(16.dp))
        Text(" $label", style = MaterialTheme.typography.labelSmall, color = if (selected) Color.White else RathYatraColors.Navy, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun fieldColors(isError: Boolean = false) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = if (isError) RathYatraColors.Emergency else RathYatraColors.Saffron,
    unfocusedBorderColor = if (isError) RathYatraColors.Emergency else RathYatraColors.Gold,
    errorBorderColor = RathYatraColors.Emergency,
    focusedContainerColor = RathYatraColors.White,
    unfocusedContainerColor = RathYatraColors.White,
    errorContainerColor = RathYatraColors.White,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDateFields(
    dob: String,
    onDobChange: (String) -> Unit,
) {
    val hindi = LocalLanguage.current == AppLanguage.HI
    val parts = dob.split("/").map { it.trim() }
    var day by remember { mutableStateOf(parts.getOrNull(0)?.toIntOrNull()) }
    var month by remember { mutableStateOf(parts.getOrNull(1)?.toIntOrNull()) }
    var year by remember { mutableStateOf(parts.getOrNull(2)?.toIntOrNull()) }
    val currentYear = remember { java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) }
    val years = (currentYear - 18 downTo currentYear - 100).toList()
    val monthNames = if (hindi) {
        listOf("जनवरी", "फरवरी", "मार्च", "अप्रैल", "मई", "जून", "जुलाई", "अगस्त", "सितंबर", "अक्टूबर", "नवंबर", "दिसंबर")
    } else {
        listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    }
    val maxDay = daysInMonth(month ?: 1, year ?: 2000)
    val dayLabel = if (hindi) "तारीख" else "Date"
    val monthLabel = if (hindi) "महीना" else "Month"
    val yearLabel = if (hindi) "वर्ष" else "Year"

    fun publish(nextDay: Int?, nextMonth: Int?, nextYear: Int?) {
        val safeDay = if (nextDay != null && nextMonth != null && nextYear != null) {
            nextDay.coerceAtMost(daysInMonth(nextMonth, nextYear))
        } else {
            nextDay
        }
        day = safeDay
        month = nextMonth
        year = nextYear
        onDobChange(
            if (safeDay != null && nextMonth != null && nextYear != null) {
                "%02d/%02d/%04d".format(safeDay, nextMonth, nextYear)
            } else {
                ""
            },
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DatePartDropdown(
            label = dayLabel,
            value = day?.toString().orEmpty(),
            options = (1..maxDay).map { it.toString() },
            modifier = Modifier.weight(1.05f),
            onSelect = { publish(it.toInt(), month, year) },
        )
        DatePartDropdown(
            label = monthLabel,
            value = month?.let { monthNames[it - 1] }.orEmpty(),
            options = monthNames,
            modifier = Modifier.weight(1.3f),
            onSelect = { publish(day, monthNames.indexOf(it) + 1, year) },
        )
        DatePartDropdown(
            label = yearLabel,
            value = year?.toString().orEmpty(),
            options = years.map { it.toString() },
            modifier = Modifier.weight(1f),
            onSelect = { publish(day, month, it.toInt()) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePartDropdown(
    label: String,
    value: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            shape = FieldShape,
            placeholder = {
                Text(
                    label,
                    color = RathYatraColors.Outline,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = fieldColors(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
        )
        Box(
            Modifier
                .matchParentSize()
                .clickable { expanded = !expanded },
        )
        DownwardMenu(expanded = expanded, onDismiss = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun BoxScope.DownwardMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    var anchorSize by remember { mutableStateOf(IntSize.Zero) }
    Box(Modifier.matchParentSize().onSizeChanged { anchorSize = it })
    if (!expanded || anchorSize == IntSize.Zero) return
    val density = LocalDensity.current
    val gap = with(density) { 4.dp.roundToPx() }
    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(0, anchorSize.height + gap),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true, clippingEnabled = false),
    ) {
        Surface(
            modifier = Modifier
                .width(with(density) { anchorSize.width.toDp() })
                .heightIn(max = 240.dp),
            shape = RoundedCornerShape(12.dp),
            color = RathYatraColors.White,
            shadowElevation = 8.dp,
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()), content = content)
        }
    }
}

private fun daysInMonth(month: Int, year: Int): Int {
    val calendar = java.util.Calendar.getInstance()
    calendar.set(java.util.Calendar.YEAR, year)
    calendar.set(java.util.Calendar.MONTH, month - 1)
    calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
    return calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
}

private data class FormExtraCopy(
    val contactTitle: String,
    val contactStep: String,
    val contactCard: String,
    val contactBody: String,
    val mobileLabel: String,
    val mobileHint: String,
    val emailLabel: String,
    val emailHint: String,
    val profileTitle: String,
    val profileStep: String,
    val profileCard: String,
    val profileBody: String,
    val addressLabel: String,
    val houseLabel: String,
    val houseHint: String,
    val localityLabel: String,
    val districtLabel: String,
    val stateLabel: String,
    val pincodeLabel: String,
    val useMyLocation: String,
    val useMyLocationHint: String,
    val locatingAddress: String,
    val locationFilled: String,
    val locationDenied: String,
    val locationUnavailable: String,
    val securityTitle: String,
    val securityStep: String,
    val securityCard: String,
    val securityBody: String,
    val pinLabel: String,
    val pinHint: String,
    val confirmPinLabel: String,
    val photoTitle: String,
    val photoHint: String,
    val photoReady: String,
    val photoRequired: String,
    val photoGallery: String,
    val nameRequired: String,
    val genderRequired: String,
    val dobRequired: String,
    val occupationRequired: String,
    val ageTooYoung: String,
    val mobileRequired: String,
    val addressRequired: String,
    val pinRequired: String,
    val pinMismatch: String,
)

private fun formExtraCopy(language: AppLanguage): FormExtraCopy =
    if (language == AppLanguage.HI) FormExtraCopy(
        contactTitle = "संपर्क विवरण",
        contactStep = "चरण 2 / 4: संपर्क विवरण (50%)",
        contactCard = "मोबाइल और ईमेल",
        contactBody = "उत्सव अपडेट और सहायता के लिए एक सक्रिय मोबाइल नंबर लिखें।",
        mobileLabel = "मोबाइल नंबर",
        mobileHint = "10 अंकों का भारतीय मोबाइल नंबर।",
        emailLabel = "ईमेल (वैकल्पिक)",
        emailHint = "पास और सूचनाएँ भेजने के लिए।",
        profileTitle = "पता",
        profileStep = "चरण 3 / 4: पता (75%)",
        profileCard = "पता",
        profileBody = "उत्सव पास के लिए अपना पता लिखें।",
        addressLabel = "पता",
        houseLabel = "मकान / गली / पता पंक्ति",
        houseHint = "लोकेशन से भरा जा सकता है, फिर ठीक करें।",
        localityLabel = "गाँव / वार्ड / इलाका",
        districtLabel = "ज़िला",
        stateLabel = "राज्य",
        pincodeLabel = "पिनकोड",
        useMyLocation = "वर्तमान लोकेशन से पता भरें",
        useMyLocationHint = "सटीक पता के लिए डिवाइस लोकेशन की अनुमति दें।",
        locatingAddress = "लोकेशन और पता खोजा जा रहा है…",
        locationFilled = "पता लोकेशन से भरा गया। जाँच कर ठीक करें।",
        locationDenied = "लोकेशन अनुमति नहीं मिली। पता स्वयं लिखें।",
        locationUnavailable = "लोकेशन नहीं मिली। पता स्वयं लिखें।",
        securityTitle = "सुरक्षा",
        securityStep = "चरण 4 / 4: सुरक्षा (100%)",
        securityCard = "ऐप पिन",
        securityBody = "4 से 6 अंकों का पिन बनाएँ। खाता बनाने से पहले यह चरण पूरा करें।",
        pinLabel = "पिन",
        pinHint = "कम से कम 4 अंक।",
        confirmPinLabel = "पिन की पुष्टि",
        photoTitle = "प्रोफ़ाइल फ़ोटो",
        photoHint = "गैलरी से फ़ोटो चुनें। फ़ोटो वैकल्पिक है और इसी उपकरण पर रहती है।",
        photoReady = "फ़ोटो जुड़ गई। बदलने के लिए फिर टैप करें।",
        photoRequired = "आगे बढ़ने से पहले प्रोफ़ाइल फ़ोटो जोड़ें।",
        photoGallery = "गैलरी से चुनें",
        nameRequired = "पूरा नाम लिखें।",
        genderRequired = "लिंग चुनें।",
        dobRequired = "जन्म तिथि चुनें।",
        occupationRequired = "व्यवसाय चुनें।",
        ageTooYoung = "स्वतंत्र पास के लिए न्यूनतम आयु 18 वर्ष है।",
        mobileRequired = "10 अंकों का मोबाइल नंबर लिखें।",
        addressRequired = "पता लिखें या लोकेशन से भरें।",
        pinRequired = "कम से कम 4 अंकों का पिन बनाएँ।",
        pinMismatch = "पिन मेल नहीं खाते।",
    ) else FormExtraCopy(
        contactTitle = "Contact Details",
        contactStep = "STEP 2 OF 4: CONTACT DETAILS (50%)",
        contactCard = "Mobile and email",
        contactBody = "Add an active mobile number so celebration updates and help can reach you.",
        mobileLabel = "MOBILE NUMBER",
        mobileHint = "10-digit Indian mobile number.",
        emailLabel = "EMAIL (OPTIONAL)",
        emailHint = "Used only to send the pass and notices.",
        profileTitle = "Address",
        profileStep = "STEP 3 OF 4: ADDRESS (75%)",
        profileCard = "Address",
        profileBody = "Add the address for your participant pass.",
        addressLabel = "ADDRESS",
        houseLabel = "HOUSE / STREET / ADDRESS LINE",
        houseHint = "Filled from location when possible. Edit if needed.",
        localityLabel = "VILLAGE / WARD / LOCALITY",
        districtLabel = "DISTRICT",
        stateLabel = "STATE",
        pincodeLabel = "PINCODE",
        useMyLocation = "Fill address from current location",
        useMyLocationHint = "Allow location so the address matches where you are.",
        locatingAddress = "Finding location and address…",
        locationFilled = "Address filled from location. Check and correct it.",
        locationDenied = "Location permission denied. Enter the address yourself.",
        locationUnavailable = "Location unavailable. Enter the address yourself.",
        securityTitle = "Security",
        securityStep = "STEP 4 OF 4: SECURITY (100%)",
        securityCard = "App PIN",
        securityBody = "Create a 4 to 6 digit PIN. The account is created only after this step.",
        pinLabel = "PIN",
        pinHint = "At least 4 digits.",
        confirmPinLabel = "CONFIRM PIN",
        photoTitle = "PROFILE PHOTO",
        photoHint = "Pick a photo from the gallery. Photos stay on this device and are optional.",
        photoReady = "Photo added. Tap again to change it.",
        photoRequired = "Add a profile photo before continuing.",
        photoGallery = "Pick from gallery",
        nameRequired = "Enter your full name.",
        genderRequired = "Select a gender.",
        dobRequired = "Select your date of birth.",
        occupationRequired = "Select an occupation.",
        ageTooYoung = "Minimum age is 18 for an independent pass.",
        mobileRequired = "Enter a 10-digit mobile number.",
        addressRequired = "Enter an address or fill it from location.",
        pinRequired = "Create a PIN of at least 4 digits.",
        pinMismatch = "PIN values do not match.",
    )

private data class AddressResult(
    val line: String? = null,
    val locality: String? = null,
    val district: String? = null,
    val state: String? = null,
    val pincode: String? = null,
    val found: Boolean = false,
)

private fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
}

private suspend fun resolveDeviceAddress(context: Context): AddressResult {
    val location = withTimeoutOrNull(12_000) { awaitLocation(context) }
        ?: return AddressResult()
    val address = geocodeLocation(context, location)
    if (address == null) {
        return AddressResult(
            line = "Lat %.5f, Lon %.5f".format(location.latitude, location.longitude),
        )
    }
    val line = address.getAddressLine(0)
        ?: listOfNotNull(address.featureName, address.thoroughfare, address.subLocality)
            .joinToString(", ")
            .ifBlank { null }
    val locality = address.subLocality ?: address.locality
    return AddressResult(
        line = line,
        locality = locality,
        district = address.subAdminArea ?: address.locality,
        state = address.adminArea,
        pincode = address.postalCode,
        found = true,
    )
}

private suspend fun awaitLocation(context: Context): Location? {
    if (!hasLocationPermission(context)) return null
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val last = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER,
    ).mapNotNull { provider ->
        runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
    }.maxByOrNull { it.time }
    val freshEnough = last != null && System.currentTimeMillis() - last.time < 10 * 60 * 1000
    if (freshEnough) return last
    val provider = when {
        manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        else -> return last
    }
    return suspendCancellableCoroutine { cont ->
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                runCatching { manager.removeUpdates(this) }
                if (cont.isActive) cont.resume(location)
            }
        }
        try {
            manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
            cont.invokeOnCancellation { runCatching { manager.removeUpdates(listener) } }
        } catch (_: SecurityException) {
            if (cont.isActive) cont.resume(last)
        }
    }
}

private suspend fun geocodeLocation(context: Context, location: Location): android.location.Address? {
    if (!Geocoder.isPresent()) return null
    val geocoder = Geocoder(context, Locale("en", "IN"))
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        suspendCancellableCoroutine { cont ->
            geocoder.getFromLocation(location.latitude, location.longitude, 1) { list ->
                if (cont.isActive) cont.resume(list.firstOrNull())
            }
        }
    } else {
        withContext(Dispatchers.IO) {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()
        }
    }
}

private fun parseDobToUtcMillis(dob: String): Long? {
    val parts = dob.split("/").map { it.trim() }
    if (parts.size != 3) return null
    val day = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val year = parts[2].toIntOrNull() ?: return null
    if (month !in 1..12 || day !in 1..31) return null
    val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
    calendar.clear()
    calendar.set(java.util.Calendar.YEAR, year)
    calendar.set(java.util.Calendar.MONTH, month - 1)
    calendar.set(java.util.Calendar.DAY_OF_MONTH, day)
    return calendar.timeInMillis
}

private fun isoDob(dob: String): String? {
    val parts = dob.split("/").map { it.trim() }
    if (parts.size != 3) return null
    val day = parts[0]
    val month = parts[1]
    val year = parts[2]
    if (day.length != 2 || month.length != 2 || year.length != 4) return null
    return "$year-$month-$day"
}

private fun ageYearsFromDob(dob: String): Int {
    val birth = parseDobToUtcMillis(dob) ?: return 0
    val born = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
    born.timeInMillis = birth
    val today = java.util.Calendar.getInstance()
    var years = today.get(java.util.Calendar.YEAR) - born.get(java.util.Calendar.YEAR)
    val birthdayPassed = today.get(java.util.Calendar.MONTH) > born.get(java.util.Calendar.MONTH) ||
        (
            today.get(java.util.Calendar.MONTH) == born.get(java.util.Calendar.MONTH) &&
                today.get(java.util.Calendar.DAY_OF_MONTH) >= born.get(java.util.Calendar.DAY_OF_MONTH)
            )
    if (!birthdayPassed) years -= 1
    return years.coerceAtLeast(0)
}
