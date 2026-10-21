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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Female
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Male
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Transgender
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.sahakarbharati.core.copy.AppLanguage
import com.example.sahakarbharati.data.AppSession
import com.example.sahakarbharati.data.DemoPhotos
import com.example.sahakarbharati.core.copy.LocalCopy
import com.example.sahakarbharati.core.copy.LocalLanguage
import com.example.sahakarbharati.data.rememberAppSession
import com.example.sahakarbharati.core.copy.RathYatraCopy
import com.example.sahakarbharati.core.ui.ApplySystemBarIcons
import com.example.sahakarbharati.core.ui.DemoPhotoChoices
import com.example.sahakarbharati.core.ui.SaffronButton
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

    var step by rememberSaveable { mutableStateOf(0) }
    var showSos by rememberSaveable { mutableStateOf(false) }
    var showDobPicker by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("Kiran Verma") }
    var dob by rememberSaveable { mutableStateOf("14 / 08 / 1988") }
    var gender by rememberSaveable { mutableStateOf("Male") }
    var occupation by rememberSaveable { mutableStateOf("Farmer") }
    var mobile by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var aadhaar by rememberSaveable { mutableStateOf("") }
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
    var identityConfirmed by rememberSaveable { mutableStateOf(false) }
    var otpSent by rememberSaveable { mutableStateOf(false) }
    var issuedOtp by rememberSaveable { mutableStateOf("") }
    var otpInput by rememberSaveable { mutableStateOf("") }
    var aadhaarVerified by rememberSaveable { mutableStateOf(false) }
    var aadhaarStatus by rememberSaveable { mutableStateOf("") }
    var photoPath by rememberSaveable { mutableStateOf(DemoPhotos.BOY) }

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
                photoPath.isBlank() -> extra.photoRequired
                else -> ""
            }
            1 -> when {
                mobile.filter(Char::isDigit).length != 10 -> extra.mobileRequired
                else -> ""
            }
            2 -> when {
                !isValidAadhaar(aadhaar) -> extra.aadhaarInvalid
                !identityConfirmed -> extra.aadhaarConfirmRequired
                !aadhaarVerified -> extra.aadhaarVerifyRequired
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
            session.completeRegistration(
                name = name,
                mobile = mobile,
                pin = pin,
                loginId = AppSession.DEFAULT_LOGIN_ID,
                photoPath = photoPath,
            )
            onContinue()
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
                    extra = extra,
                    name = name,
                    onName = { name = it },
                    photoPath = photoPath,
                    onPickPhoto = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onSelectDemoPhoto = { photoPath = it },
                    gender = gender,
                    onGender = {
                        gender = it
                        if (photoPath.isBlank() || DemoPhotos.isDemo(photoPath)) {
                            photoPath = DemoPhotos.forGender(it)
                        }
                    },
                    dob = dob,
                    onOpenDob = { showDobPicker = true },
                    ageText = ageText,
                    ageYears = ageYears,
                    occupation = occupation,
                    onOccupation = { occupation = it },
                )
                1 -> ContactStep(
                    extra = extra,
                    mobile = mobile,
                    onMobile = { mobile = it.filter(Char::isDigit).take(10) },
                    email = email,
                    onEmail = { email = it },
                )
                2 -> AddressStep(
                    extra = extra,
                    name = name,
                    dob = dob,
                    mobile = mobile,
                    aadhaar = aadhaar,
                    onAadhaar = { raw ->
                        val digits = raw.filter(Char::isDigit).take(12)
                        if (digits != aadhaar) {
                            aadhaar = digits
                            identityConfirmed = false
                            otpSent = false
                            issuedOtp = ""
                            otpInput = ""
                            aadhaarVerified = false
                            aadhaarStatus = ""
                        }
                    },
                    identityConfirmed = identityConfirmed,
                    onIdentityConfirmed = { identityConfirmed = it },
                    otpSent = otpSent,
                    issuedOtp = issuedOtp,
                    otpInput = otpInput,
                    onOtpInput = { otpInput = it.filter(Char::isDigit).take(6) },
                    aadhaarVerified = aadhaarVerified,
                    aadhaarStatus = aadhaarStatus,
                    onSendOtp = {
                        when {
                            !isValidAadhaar(aadhaar) -> aadhaarStatus = extra.aadhaarInvalid
                            !identityConfirmed -> aadhaarStatus = extra.aadhaarConfirmRequired
                            mobile.filter(Char::isDigit).length != 10 -> aadhaarStatus = extra.mobileRequired
                            else -> {
                                issuedOtp = (100000..999999).random().toString()
                                otpSent = true
                                otpInput = ""
                                aadhaarVerified = false
                                aadhaarStatus = extra.otpSent.format(maskMobile(mobile))
                            }
                        }
                    },
                    onConfirmOtp = {
                        if (otpInput.isNotEmpty() && otpInput == issuedOtp) {
                            aadhaarVerified = true
                            aadhaarStatus = extra.aadhaarVerified
                        } else {
                            aadhaarVerified = false
                            aadhaarStatus = extra.otpWrong
                        }
                    },
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
            SaffronButton(
                text = if (step == 3) copy.createAccount else copy.continueNext,
                onClick = { advance() },
                trailingIcon = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = if (step == 3) copy.createAccount else copy.continueNext,
            )
        }
    }
    if (showSos) {
        com.example.sahakarbharati.core.ui.DemoAlertDialog(
            title = copy.sosTitle,
            body = copy.sosBody,
            onDismiss = { showSos = false },
        )
    }
    if (showDobPicker) {
        BirthdayPickerDialog(
            initialDob = dob,
            confirmLabel = if (language == AppLanguage.HI) "ठीक है" else "OK",
            dismissLabel = if (language == AppLanguage.HI) "रद्द करें" else "Cancel",
            onConfirm = { millis ->
                dob = formatDob(millis)
                showDobPicker = false
            },
            onDismiss = { showDobPicker = false },
        )
    }
}

@Composable
private fun ColumnScope.PersonalStep(
    copy: RathYatraCopy,
    extra: FormExtraCopy,
    name: String,
    onName: (String) -> Unit,
    photoPath: String,
    onPickPhoto: () -> Unit,
    onSelectDemoPhoto: (String) -> Unit,
    gender: String,
    onGender: (String) -> Unit,
    dob: String,
    onOpenDob: () -> Unit,
    ageText: String,
    ageYears: Int,
    occupation: String,
    onOccupation: (String) -> Unit,
) {
    WhiteCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(copy.formCardTitle, style = MaterialTheme.typography.titleSmall, color = RathYatraColors.Navy)
            Text(copy.formCardBody, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
        }
    }
    WhiteCard {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(extra.photoTitle, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.OnSurfaceVariant)
            DemoPhotoChoices(
                selectedPath = photoPath,
                boyLabel = extra.photoBoy,
                girlLabel = extra.photoGirl,
                onSelect = onSelectDemoPhoto,
                size = 88.dp,
            )
            Text(
                extra.photoHint,
                style = MaterialTheme.typography.bodySmall,
                color = RathYatraColors.OnSurfaceVariant,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.clickable(onClick = onPickPhoto),
            ) {
                Icon(
                    Icons.Outlined.AddAPhoto,
                    contentDescription = extra.photoGallery,
                    tint = RathYatraColors.Navy,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    extra.photoGallery,
                    style = MaterialTheme.typography.bodySmall,
                    color = RathYatraColors.Navy,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
    WhiteCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(copy.legalName, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.OnSurfaceVariant)
                StatusChip(copy.asInId, container = RathYatraColors.SurfaceContainerLow, content = RathYatraColors.Navy)
            }
            OutlinedTextField(
                value = name,
                onValueChange = onName,
                modifier = Modifier.fillMaxWidth(),
                shape = FieldShape,
                trailingIcon = {
                    Icon(Icons.Outlined.Check, contentDescription = copy.legalName, tint = RathYatraColors.Navy)
                },
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
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WhiteCard(Modifier.weight(1.2f)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(copy.dob, style = MaterialTheme.typography.labelSmall)
                OutlinedTextField(
                    value = dob,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = FieldShape,
                    trailingIcon = {
                        IconButton(onClick = onOpenDob) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = "Date of birth")
                        }
                    },
                    colors = fieldColors(),
                )
                Text(copy.ageMin, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
            }
        }
        WhiteCard(Modifier.weight(0.9f)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(copy.calculatedAge, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                    Icon(Icons.Outlined.Info, null, Modifier.size(14.dp), RathYatraColors.GoldDark)
                }
                StatusChip(ageText, container = RathYatraColors.GoldPale, content = RathYatraColors.GoldDark)
                if (ageYears >= 18) {
                    Text(copy.eligible, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.Success)
                    Text(copy.eligibleNote, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
                }
            }
        }
    }
    WhiteCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(copy.occupation, style = MaterialTheme.typography.labelSmall)
            Text(copy.occupationHint, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
            val occupations = listOf(
                Triple("Farmer", copy.farmer, copy.farmerDesc),
                Triple("Fisherman", copy.fisherman, copy.fishermanDesc),
                Triple("Both", copy.both, copy.bothDesc),
                Triple("Allied", copy.allied, copy.alliedDesc),
            )
            occupations.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (key, title, body) ->
                        val selected = occupation == key
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) RathYatraColors.Saffron else RathYatraColors.SurfaceContainerHighest,
                                    shape = RoundedCornerShape(12.dp),
                                )
                                .clickable { onOccupation(key) }
                                .padding(12.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (selected) {
                                    Box(Modifier.size(8.dp).background(RathYatraColors.Saffron, CircleShape))
                                    Spacer(Modifier.size(6.dp))
                                }
                                Text(title, style = MaterialTheme.typography.titleSmall)
                            }
                            Text(body, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.ContactStep(
    extra: FormExtraCopy,
    mobile: String,
    onMobile: (String) -> Unit,
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
    dob: String,
    mobile: String,
    aadhaar: String,
    onAadhaar: (String) -> Unit,
    identityConfirmed: Boolean,
    onIdentityConfirmed: (Boolean) -> Unit,
    otpSent: Boolean,
    issuedOtp: String,
    otpInput: String,
    onOtpInput: (String) -> Unit,
    aadhaarVerified: Boolean,
    aadhaarStatus: String,
    onSendOtp: () -> Unit,
    onConfirmOtp: () -> Unit,
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
    WhiteCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(extra.profileCard, style = MaterialTheme.typography.titleSmall, color = RathYatraColors.Navy)
            Text(extra.profileBody, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
        }
    }
    WhiteCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(extra.aadhaarLabel, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.OnSurfaceVariant)
            OutlinedTextField(
                value = formatAadhaar(aadhaar),
                onValueChange = onAadhaar,
                modifier = Modifier.fillMaxWidth(),
                shape = FieldShape,
                colors = fieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                trailingIcon = {
                    if (aadhaarVerified) {
                        Icon(Icons.Outlined.Verified, contentDescription = extra.aadhaarVerified, tint = RathYatraColors.Success)
                    }
                },
            )
            Text(extra.aadhaarHint, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
            if (aadhaar.length == 12) {
                Text(
                    if (isValidAadhaar(aadhaar)) extra.aadhaarChecksumOk else extra.aadhaarInvalid,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isValidAadhaar(aadhaar)) RathYatraColors.Success else RathYatraColors.Emergency,
                )
            }
            Text(
                extra.aadhaarMatchLine.format(name, dob, maskMobile(mobile)),
                style = MaterialTheme.typography.bodySmall,
                color = RathYatraColors.OnSurface,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = identityConfirmed,
                    onCheckedChange = onIdentityConfirmed,
                )
                Text(extra.aadhaarConfirm, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurface)
            }
            SaffronButton(
                text = if (otpSent) extra.resendOtp else extra.sendAadhaarOtp,
                onClick = onSendOtp,
                contentDescription = extra.sendAadhaarOtp,
            )
            if (otpSent && !aadhaarVerified) {
                Text(
                    extra.otpDemoHint.format(issuedOtp, maskMobile(mobile)),
                    style = MaterialTheme.typography.bodySmall,
                    color = RathYatraColors.Navy,
                )
                OutlinedTextField(
                    value = otpInput,
                    onValueChange = onOtpInput,
                    modifier = Modifier.fillMaxWidth(),
                    shape = FieldShape,
                    colors = fieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    placeholder = { Text(extra.otpPlaceholder) },
                )
                SaffronButton(
                    text = extra.confirmOtp,
                    onClick = onConfirmOtp,
                    contentDescription = extra.confirmOtp,
                )
            }
            if (aadhaarStatus.isNotEmpty()) {
                Text(
                    aadhaarStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (aadhaarVerified) RathYatraColors.Success else RathYatraColors.Navy,
                )
            }
        }
    }
    WhiteCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(extra.addressLabel, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.OnSurfaceVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(RathYatraColors.SecondaryPale)
                    .clickable(enabled = !locating, onClick = onUseLocation)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (locating) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = RathYatraColors.Navy, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Outlined.MyLocation, contentDescription = extra.useMyLocation, tint = RathYatraColors.Navy)
                }
                Column(Modifier.weight(1f)) {
                    Text(extra.useMyLocation, style = MaterialTheme.typography.titleSmall, color = RathYatraColors.Navy)
                    Text(extra.useMyLocationHint, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
                }
            }
            if (locationNote.isNotEmpty()) {
                Text(locationNote, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.Navy)
            }
        }
    }
    LabeledField(extra.houseLabel, addressLine, onAddressLine, extra.houseHint)
    LabeledField(extra.localityLabel, locality, onLocality)
    LabeledField(extra.districtLabel, district, onDistrict)
    LabeledField(extra.stateLabel, stateName, onState)
    LabeledField(extra.pincodeLabel, pincode, onPincode, keyboardType = KeyboardType.Number)
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
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String? = null,
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
                colors = fieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                prefix = prefix?.let { { Text(it, color = RathYatraColors.OnSurfaceVariant) } },
                singleLine = true,
            )
            if (hint != null) {
                Text(hint, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
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
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = RathYatraColors.Saffron,
    unfocusedBorderColor = RathYatraColors.Gold,
    focusedContainerColor = RathYatraColors.White,
    unfocusedContainerColor = RathYatraColors.White,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthdayPickerDialog(
    initialDob: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val today = remember { System.currentTimeMillis() }
    val currentYear = remember {
        java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = parseDobToUtcMillis(initialDob) ?: defaultDobMillis(),
        yearRange = IntRange(currentYear - 120, currentYear),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= today
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { datePickerState.selectedDateMillis?.let(onConfirm) }) {
                Text(confirmLabel, color = RathYatraColors.Navy)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissLabel, color = RathYatraColors.OnSurfaceVariant)
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
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
    val aadhaarLabel: String,
    val aadhaarHint: String,
    val aadhaarChecksumOk: String,
    val aadhaarInvalid: String,
    val aadhaarMatchLine: String,
    val aadhaarConfirm: String,
    val aadhaarConfirmRequired: String,
    val aadhaarVerifyRequired: String,
    val sendAadhaarOtp: String,
    val resendOtp: String,
    val otpSent: String,
    val otpDemoHint: String,
    val otpPlaceholder: String,
    val confirmOtp: String,
    val otpWrong: String,
    val aadhaarVerified: String,
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
    val photoBoy: String,
    val photoGirl: String,
    val photoGallery: String,
    val nameRequired: String,
    val mobileRequired: String,
    val aadhaarRequired: String,
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
        profileTitle = "पहचान और पता",
        profileStep = "चरण 3 / 4: पहचान और पता (75%)",
        profileCard = "आधार और पता",
        profileBody = "पहले आधार संख्या की जाँच होगी, फिर नाम और जन्म तिथि की पुष्टि, फिर आपके मोबाइल पर OTP। यह UIDAI e-KYC नहीं है।",
        aadhaarLabel = "आधार संख्या",
        aadhaarHint = "12 अंक। संख्या वैध होनी चाहिए। UIDAI से व्यक्ति-मिलान इस ऐप में नहीं जुड़ा है।",
        aadhaarChecksumOk = "आधार संख्या प्रारूप सही है।",
        aadhaarInvalid = "यह आधार संख्या वैध नहीं है। अंक जाँचें।",
        aadhaarMatchLine = "इस फ़ॉर्म पर नाम %s, जन्म %s, मोबाइल %s है। पुष्टि करें कि यही आपके आधार पर है।",
        aadhaarConfirm = "मैं पुष्टि करता/करती हूँ कि यह आधार इसी व्यक्ति का है।",
        aadhaarConfirmRequired = "आधार इसी नाम और जन्म तिथि का है, यह चुनें।",
        aadhaarVerifyRequired = "आगे बढ़ने से पहले आधार OTP से सत्यापित करें।",
        sendAadhaarOtp = "मोबाइल पर OTP भेजें",
        resendOtp = "OTP फिर भेजें",
        otpSent = "OTP %s पर भेजा गया।",
        otpDemoHint = "इस डिवाइस जाँच के लिए कोड %s है। मोबाइल %s।",
        otpPlaceholder = "6 अंकों का OTP",
        confirmOtp = "OTP सत्यापित करें",
        otpWrong = "OTP गलत है। फिर कोशिश करें।",
        aadhaarVerified = "आधार संख्या वैध है और पंजीकृत मोबाइल से पुष्टि हो गई।",
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
        photoHint = "डेमो के लिए लड़का या लड़की चुनें। गैलरी से भी फ़ोटो ले सकते हैं।",
        photoReady = "फ़ोटो जुड़ गई। बदलने के लिए फिर टैप करें।",
        photoRequired = "आगे बढ़ने से पहले प्रोफ़ाइल फ़ोटो जोड़ें।",
        photoBoy = "लड़का",
        photoGirl = "लड़की",
        photoGallery = "गैलरी से चुनें",
        nameRequired = "पूरा नाम लिखें।",
        mobileRequired = "10 अंकों का मोबाइल नंबर लिखें।",
        aadhaarRequired = "12 अंकों का आधार नंबर लिखें।",
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
        profileTitle = "Identity & Address",
        profileStep = "STEP 3 OF 4: IDENTITY & ADDRESS (75%)",
        profileCard = "Aadhaar and address",
        profileBody = "The Aadhaar number is checked first, then you confirm the name and date of birth, then an OTP is sent to your registered mobile. This is not UIDAI e-KYC.",
        aadhaarLabel = "AADHAAR NUMBER",
        aadhaarHint = "12 digits. The number must pass the official checksum. UIDAI person-match is not connected in this app.",
        aadhaarChecksumOk = "Aadhaar number format is valid.",
        aadhaarInvalid = "This Aadhaar number is not valid. Check the digits.",
        aadhaarMatchLine = "This form has name %s, birth %s, mobile %s. Confirm these match your Aadhaar card.",
        aadhaarConfirm = "I confirm this Aadhaar belongs to this person.",
        aadhaarConfirmRequired = "Confirm that this Aadhaar matches the name and date of birth.",
        aadhaarVerifyRequired = "Verify Aadhaar with the mobile OTP before continuing.",
        sendAadhaarOtp = "Send OTP to mobile",
        resendOtp = "Resend OTP",
        otpSent = "OTP sent to %s.",
        otpDemoHint = "On-device check code is %s for mobile %s.",
        otpPlaceholder = "6-digit OTP",
        confirmOtp = "Verify OTP",
        otpWrong = "OTP is incorrect. Try again.",
        aadhaarVerified = "Aadhaar number is valid and confirmed on the registered mobile.",
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
        photoHint = "Pick the boy or girl photo for this demo, or choose one from the gallery.",
        photoReady = "Photo added. Tap again to change it.",
        photoRequired = "Add a profile photo before continuing.",
        photoBoy = "Boy",
        photoGirl = "Girl",
        photoGallery = "Pick from gallery",
        nameRequired = "Enter your full name.",
        mobileRequired = "Enter a 10-digit mobile number.",
        aadhaarRequired = "Enter a 12-digit Aadhaar number.",
        addressRequired = "Enter an address or fill it from location.",
        pinRequired = "Create a PIN of at least 4 digits.",
        pinMismatch = "PIN values do not match.",
    )

private fun formatAadhaar(digits: String): String {
    val clean = digits.filter(Char::isDigit).take(12)
    return clean.chunked(4).joinToString(" ")
}

private fun maskMobile(mobile: String): String {
    val digits = mobile.filter(Char::isDigit)
    if (digits.length < 4) return "+91 ----------"
    return "+91 ******${digits.takeLast(4)}"
}

private val VerhoeffD = arrayOf(
    intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
    intArrayOf(1, 2, 3, 4, 0, 6, 7, 8, 9, 5),
    intArrayOf(2, 3, 4, 0, 1, 7, 8, 9, 5, 6),
    intArrayOf(3, 4, 0, 1, 2, 8, 9, 5, 6, 7),
    intArrayOf(4, 0, 1, 2, 3, 9, 5, 6, 7, 8),
    intArrayOf(5, 9, 8, 7, 6, 0, 4, 3, 2, 1),
    intArrayOf(6, 5, 9, 8, 7, 1, 0, 4, 3, 2),
    intArrayOf(7, 6, 5, 9, 8, 2, 1, 0, 4, 3),
    intArrayOf(8, 7, 6, 5, 9, 3, 2, 1, 0, 4),
    intArrayOf(9, 8, 7, 6, 5, 4, 3, 2, 1, 0),
)

private val VerhoeffP = arrayOf(
    intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
    intArrayOf(1, 5, 7, 6, 2, 8, 3, 0, 9, 4),
    intArrayOf(5, 8, 0, 3, 7, 9, 6, 1, 4, 2),
    intArrayOf(8, 9, 1, 6, 0, 4, 3, 5, 2, 7),
    intArrayOf(9, 4, 5, 3, 1, 2, 6, 8, 7, 0),
    intArrayOf(4, 2, 8, 6, 5, 7, 3, 9, 0, 1),
    intArrayOf(2, 7, 9, 3, 8, 0, 6, 4, 1, 5),
    intArrayOf(7, 0, 4, 6, 9, 1, 3, 2, 5, 8),
)

private fun isValidAadhaar(raw: String): Boolean {
    val digits = raw.filter(Char::isDigit)
    if (digits.length != 12) return false
    if (digits[0] == '0' || digits[0] == '1') return false
    var checksum = 0
    digits.reversed().forEachIndexed { index, char ->
        checksum = VerhoeffD[checksum][VerhoeffP[index % 8][char - '0']]
    }
    return checksum == 0
}

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

private fun defaultDobMillis(): Long {
    val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
    calendar.clear()
    calendar.set(1988, java.util.Calendar.AUGUST, 14)
    return calendar.timeInMillis
}

private fun formatDob(utcMillis: Long): String {
    val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
    calendar.timeInMillis = utcMillis
    val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
    val month = calendar.get(java.util.Calendar.MONTH) + 1
    val year = calendar.get(java.util.Calendar.YEAR)
    return "%02d / %02d / %04d".format(day, month, year)
}

private fun ageYearsFromDob(dob: String): Int {
    val birth = parseDobToUtcMillis(dob) ?: return 36
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
