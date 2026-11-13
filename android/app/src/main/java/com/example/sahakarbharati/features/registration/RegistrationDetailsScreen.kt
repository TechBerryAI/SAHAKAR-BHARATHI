package com.example.sahakarbharati.features.registration

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Female
import androidx.compose.material.icons.outlined.Male
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Transgender
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sahakarbharati.core.copy.LocalCopy
import com.example.sahakarbharati.core.ui.ApplySystemBarIcons
import com.example.sahakarbharati.core.ui.SaffronButton
import com.example.sahakarbharati.core.ui.StackTopBar
import com.example.sahakarbharati.core.ui.WhiteCard
import com.example.sahakarbharati.ui.theme.RathYatraColors
import java.util.Calendar
import kotlin.math.roundToInt

private val Cream = Color(0xFFFFF8EF)
private val Ink = Color(0xFF17263F)
private val DeepGreen = Color(0xFF0F3D2E)
private val Muted = Color(0xFF6F675D)
private val FieldInk = Color(0xFF1A1C19)
private val BorderGold = Color(0xFFE2C48A)
private val RequiredRed = Color(0xFFC93B2B)
private val Saffron = Color(0xFFE9872A)
private val SaffronText = Color(0xFFB85D12)
private val TrackIdle = Color(0xFF8A8176)

private val EnglishMonths = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)
private val HindiMonths = listOf(
    "जनवरी", "फ़रवरी", "मार्च", "अप्रैल", "मई", "जून",
    "जुलाई", "अगस्त", "सितंबर", "अक्टूबर", "नवंबर", "दिसंबर",
)

@Composable
fun RegistrationDetailsScreen(
    hindi: Boolean,
    name: String,
    onName: (String) -> Unit,
    gender: String,
    onGender: (String) -> Unit,
    dob: String,
    onDob: (String) -> Unit,
    occupation: String,
    onOccupation: (String) -> Unit,
    mobile: String,
    email: String,
    onEmail: (String) -> Unit,
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
    pin: String,
    onPin: (String) -> Unit,
    confirmPin: String,
    onConfirmPin: (String) -> Unit,
    error: String,
    loading: Boolean,
    onBack: () -> Unit,
    onSos: () -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ApplySystemBarIcons(lightIcons = false)
    val copy = LocalCopy.current
    val parts = dob.split("/")
    val day = parts.getOrNull(0).orEmpty()
    val month = parts.getOrNull(1).orEmpty()
    val year = parts.getOrNull(2).orEmpty()
    val months = if (hindi) HindiMonths else EnglishMonths
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val years = (0 until 90).map { (currentYear - 10 - it).toString() }
    val emailOk = email.isBlank() || Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email.trim())
    val checks = listOf(
        listOf(name.trim().length >= 2, gender.isNotBlank(), adultDob(dob), occupation.isNotBlank()),
        listOf(mobile.length == 10, emailOk),
        listOf(district.isNotBlank(), stateName.isNotBlank(), pincode.length == 6),
        listOf(pin.length in 4..6 && pin.all(Char::isDigit), confirmPin.isNotEmpty() && confirmPin == pin),
    )
    val fractions = checks.map { row -> row.count { it } / row.size.toFloat() }
    val done = fractions.count { it == 1f }
    val percent = ((fractions.sum() / fractions.size) * 100).roundToInt()
    val progress = if (hindi) {
        "$done / 4 खंड पूर्ण ($percent%)"
    } else {
        "$done OF 4 SECTIONS COMPLETE ($percent%)"
    }
    val tracks = if (hindi) {
        listOf("1 व्यक्तिगत", "2 संपर्क", "3 पता", "4 सुरक्षा")
    } else {
        listOf("1 Personal", "2 Contact", "3 Address", "4 Security")
    }
    val active = fractions.indexOfFirst { it < 1f }.let { if (it < 0) tracks.lastIndex else it }

    Column(
        modifier
            .fillMaxSize()
            .background(Cream)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .windowInsetsPadding(WindowInsets.ime),
    ) {
        Column(Modifier.background(Cream).windowInsetsPadding(WindowInsets.statusBars)) {
            StackTopBar(
                title = if (hindi) "पंजीकरण" else "Registration",
                onBack = onBack,
                onSosClick = onSos,
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(Brush.horizontalGradient(listOf(Saffron, Color(0xFFF6C15B)))),
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (error.isNotBlank()) {
                Text(error, color = RathYatraColors.Emergency, fontSize = 14.sp)
            }
            WhiteCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(13.dp))
                                .background(Color(0xFFFFF1DF))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(Saffron))
                            Text(progress, color = SaffronText, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Outlined.Shield, contentDescription = null, tint = Ink, modifier = Modifier.size(14.dp))
                            Text(copy.govtPortal, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        tracks.forEachIndexed { index, label ->
                            val filled = fractions[index]
                            val color = when {
                                index == active && filled < 1f -> Saffron
                                filled == 1f -> Ink
                                else -> TrackIdle
                            }
                            Column(
                                Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(99.dp))
                                        .background(Color(0xFFEFE6D8)),
                                ) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth(filled.coerceIn(0f, 1f))
                                            .height(6.dp)
                                            .background(Saffron),
                                    )
                                }
                                Text(
                                    label,
                                    fontSize = 11.sp,
                                    color = color,
                                    fontWeight = if (index == active || filled == 1f) FontWeight.SemiBold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }

            SectionTitle(1, copy.formTitle, fractions[0] == 1f)
            WhiteCard {
                Column(Modifier.padding(16.dp)) {
                    WebField(
                        label = copy.legalName,
                        value = name,
                        onValue = onName,
                        required = true,
                        hint = if (hindi) "आधार कार्ड के अनुसार नाम" else "Name as per Aadhaar card",
                    )
                }
            }
            WhiteCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RequiredLabel(copy.gender, required = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GenderButton(copy.male, Icons.Outlined.Male, gender == "Male", Modifier.weight(1f)) {
                            onGender(if (gender == "Male") "" else "Male")
                        }
                        GenderButton(copy.female, Icons.Outlined.Female, gender == "Female", Modifier.weight(1f)) {
                            onGender(if (gender == "Female") "" else "Female")
                        }
                        GenderButton(copy.other, Icons.Outlined.Transgender, gender == "Other", Modifier.weight(1f)) {
                            onGender(if (gender == "Other") "" else "Other")
                        }
                    }
                }
            }
            WhiteCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RequiredLabel(copy.dob, required = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        WebSelect(
                            placeholder = if (hindi) "दिन" else "Day",
                            selected = day,
                            options = (1..31).map { "%02d".format(it) },
                            onSelect = { onDob(joinDob(it, month, year)) },
                            modifier = Modifier.weight(0.88f),
                        )
                        WebSelect(
                            placeholder = if (hindi) "माह" else "Month",
                            selected = month,
                            options = (1..12).map { "%02d".format(it) },
                            labels = months,
                            onSelect = { onDob(joinDob(day, it, year)) },
                            modifier = Modifier.weight(1.82f),
                        )
                        WebSelect(
                            placeholder = if (hindi) "वर्ष" else "Year",
                            selected = year,
                            options = years,
                            onSelect = { onDob(joinDob(day, month, it)) },
                            modifier = Modifier.weight(1.04f),
                        )
                    }
                    Text(copy.ageMin, fontSize = 12.sp, color = Muted)
                }
            }
            WhiteCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RequiredLabel(copy.occupation, required = true)
                    Text(copy.occupationHint, fontSize = 12.sp, color = Muted)
                    WebSelect(
                        placeholder = if (hindi) "चुनें" else "Select",
                        selected = occupation,
                        options = listOf("Farmer", "Fisherman", "Both", "Allied"),
                        labels = listOf(copy.farmer, copy.fisherman, copy.both, copy.allied),
                        onSelect = onOccupation,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            SectionTitle(2, if (hindi) "संपर्क विवरण" else "Contact Details", fractions[1] == 1f)
            WhiteCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RequiredLabel(if (hindi) "मोबाइल नंबर" else "MOBILE NUMBER", required = false)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .border(1.dp, BorderGold, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("+91", color = Color(0xFF5C665F), fontSize = 16.sp)
                        Text(mobile, color = FieldInk, fontSize = 16.sp)
                    }
                    Text(
                        if (hindi) "यह नंबर ओटीपी से सत्यापित है।" else "This number is verified with OTP.",
                        fontSize = 12.sp,
                        color = Muted,
                    )
                }
            }
            WhiteCard {
                Column(Modifier.padding(16.dp)) {
                    WebField(
                        label = if (hindi) "ईमेल (वैकल्पिक)" else "EMAIL (OPTIONAL)",
                        value = email,
                        onValue = onEmail,
                        keyboard = KeyboardType.Email,
                        hint = if (hindi) "केवल पास और सूचनाएँ भेजने के लिए।" else "Used only to send the pass and notices.",
                    )
                }
            }

            SectionTitle(3, if (hindi) "पता" else "Address", fractions[2] == 1f)
            WhiteCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (hindi) "अपने प्रतिभागी पास के लिए पता लिखें।" else "Add the address for your participant pass.",
                        fontSize = 13.sp,
                        color = Muted,
                    )
                    WebField(
                        label = if (hindi) "मकान / गली / पता पंक्ति" else "HOUSE / STREET / ADDRESS LINE",
                        value = addressLine,
                        onValue = onAddressLine,
                    )
                    WebField(
                        label = if (hindi) "गाँव / वार्ड / इलाका" else "VILLAGE / WARD / LOCALITY",
                        value = locality,
                        onValue = onLocality,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.weight(1f)) {
                            WebField(
                                label = if (hindi) "ज़िला" else "DISTRICT",
                                value = district,
                                onValue = onDistrict,
                                required = true,
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            WebField(
                                label = if (hindi) "राज्य" else "STATE",
                                value = stateName,
                                onValue = onState,
                                required = true,
                            )
                        }
                    }
                    WebField(
                        label = if (hindi) "पिनकोड" else "PINCODE",
                        value = pincode,
                        onValue = { onPincode(it.filter(Char::isDigit).take(6)) },
                        required = true,
                        keyboard = KeyboardType.Number,
                    )
                }
            }

            SectionTitle(4, if (hindi) "सुरक्षा" else "Security", fractions[3] == 1f)
            WhiteCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        if (hindi) "ऐप पिन" else "App PIN",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink,
                    )
                    Text(
                        if (hindi) "4 से 6 अंकों का पिन बनाएँ। खाता इस चरण के बाद ही बनेगा।" else "Create a 4 to 6 digit PIN. The account is created only after this step.",
                        fontSize = 13.sp,
                        color = Muted,
                    )
                }
            }
            WhiteCard {
                Column(Modifier.padding(16.dp)) {
                    WebField(
                        label = if (hindi) "पिन" else "PIN",
                        value = pin,
                        onValue = { onPin(it.filter(Char::isDigit).take(6)) },
                        required = true,
                        password = true,
                        keyboard = KeyboardType.NumberPassword,
                        hint = if (hindi) "कम से कम 4 अंक।" else "At least 4 digits.",
                    )
                }
            }
            WhiteCard {
                Column(Modifier.padding(16.dp)) {
                    WebField(
                        label = if (hindi) "पिन की पुष्टि" else "CONFIRM PIN",
                        value = confirmPin,
                        onValue = { onConfirmPin(it.filter(Char::isDigit).take(6)) },
                        required = true,
                        password = true,
                        keyboard = KeyboardType.NumberPassword,
                    )
                }
            }
        }
        Column(Modifier.padding(16.dp)) {
            if (loading) {
                CircularProgressIndicator(
                    Modifier.size(28.dp).align(Alignment.CenterHorizontally),
                    color = Saffron,
                    strokeWidth = 3.dp,
                )
            } else {
                SaffronButton(
                    text = copy.createAccount,
                    onClick = onCreate,
                    trailingIcon = Icons.AutoMirrored.Outlined.ArrowForward,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(index: Int, title: String, done: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(top = 8.dp),
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (done) Saffron else Color(0xFFFFF1DF)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$index",
                color = if (done) Color.White else SaffronText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(title, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RequiredLabel(text: String, required: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text.uppercase(),
            color = DeepGreen,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.4.sp,
        )
        if (required) {
            Text(
                "*",
                color = RequiredRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 3.dp),
            )
        }
    }
}

@Composable
private fun WebField(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    required: Boolean = false,
    hint: String? = null,
    keyboard: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
) {
    var revealed by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        RequiredLabel(label, required)
        Box(Modifier.fillMaxWidth()) {
            BasicTextField(
                value = value,
                onValueChange = onValue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .border(1.dp, BorderGold, RoundedCornerShape(12.dp))
                    .padding(start = 14.dp, end = if (password) 44.dp else 14.dp),
                textStyle = TextStyle(color = FieldInk, fontSize = 16.sp),
                singleLine = true,
                visualTransformation = if (password && !revealed) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = keyboard),
                cursorBrush = SolidColor(Saffron),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) { inner() }
                },
            )
            if (password) {
                Icon(
                    imageVector = if (revealed) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (revealed) "Hide" else "Show",
                    tint = Muted,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 6.dp)
                        .size(40.dp)
                        .clickable { revealed = !revealed }
                        .padding(9.dp),
                )
            }
        }
        if (!hint.isNullOrBlank()) {
            Text(hint, fontSize = 12.sp, color = Muted)
        }
    }
}

@Composable
private fun WebSelect(
    placeholder: String,
    selected: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    labels: List<String> = options,
    onSelect: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val index = options.indexOf(selected)
    val shown = if (index >= 0) labels.getOrElse(index) { selected } else ""
    Box(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, BorderGold, RoundedCornerShape(12.dp))
                .background(Color.White)
                .clickable { open = true }
                .padding(start = 8.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = shown.ifBlank { placeholder },
                modifier = Modifier.weight(1f),
                color = if (shown.isBlank()) Muted else FieldInk,
                fontSize = if (shown.isBlank()) 13.sp else 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = Ink,
                modifier = Modifier.size(22.dp),
            )
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.heightIn(max = 320.dp),
        ) {
            options.forEachIndexed { optionIndex, option ->
                DropdownMenuItem(
                    text = { Text(labels.getOrElse(optionIndex) { option }, color = Ink) },
                    onClick = {
                        onSelect(option)
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
private fun GenderButton(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val tint = if (selected) SaffronText else Ink
    Row(
        modifier
            .height(40.dp)
            .clip(RoundedCornerShape(999.dp))
            .border(1.dp, if (selected) Saffron else Color(0xFFEFE6D8), RoundedCornerShape(999.dp))
            .background(if (selected) Cream else Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Box(Modifier.width(4.dp))
        Text(label, color = tint, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
