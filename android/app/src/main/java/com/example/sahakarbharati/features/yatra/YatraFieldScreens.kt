package com.example.sahakarbharati.features.yatra

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.sahakarbharati.core.copy.uiKnown
import com.example.sahakarbharati.core.ui.SaffronButton
import com.example.sahakarbharati.core.ui.SignedInScaffold
import com.example.sahakarbharati.core.ui.WhiteCard
import com.example.sahakarbharati.data.CounterQr
import com.example.sahakarbharati.data.model.isStaffRole
import com.example.sahakarbharati.data.rememberAppSession
import com.example.sahakarbharati.data.sahakarViewModel
import com.example.sahakarbharati.features.attendance.BoothCamera
import com.example.sahakarbharati.ui.navigation.RathYatraDestinations
import com.example.sahakarbharati.ui.theme.RathYatraColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun YatraAttendanceScreen(
    yatraId: String,
    onBack: () -> Unit,
    onSelectTab: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val session = rememberAppSession()
    val viewModel: YatraAttendanceViewModel = sahakarViewModel()
    val outcome = viewModel.outcome
    SignedInScaffold(
        title = "Counter attendance",
        selectedTab = RathYatraDestinations.HOME,
        showAdmin = isStaffRole(session.role()),
        onSelectTab = onSelectTab,
        onBack = onBack,
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
            WhiteCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Point at the counter QR. A booth QR is not a counter.",
                        color = RathYatraColors.Navy,
                        fontWeight = FontWeight.Medium,
                    )
                    BoothCamera(
                        enabled = outcome !is AttendanceOutcome.Submitting,
                        onCode = { raw -> viewModel.scan(yatraId, raw) },
                        onDigits = {},
                        accept = CounterQr::fromQr,
                    )
                    if (outcome is AttendanceOutcome.Submitting) {
                        CircularProgressIndicator(color = RathYatraColors.Saffron)
                    }
                }
            }
            outcomeMessage(outcome)?.let { message ->
                Text(uiKnown(message), color = outcomeColor(outcome), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

private fun outcomeMessage(outcome: AttendanceOutcome?): String? = when (outcome) {
    null, AttendanceOutcome.Submitting -> null
    AttendanceOutcome.Success -> "You are marked present at this counter."
    AttendanceOutcome.Duplicate -> "Already marked present at this counter today."
    is AttendanceOutcome.Invalid -> outcome.message
    is AttendanceOutcome.Failed -> outcome.message
}

private fun outcomeColor(outcome: AttendanceOutcome?) = when (outcome) {
    AttendanceOutcome.Success, AttendanceOutcome.Duplicate -> RathYatraColors.DeepGreen
    else -> RathYatraColors.Emergency
}

@Composable
fun YatraTrackingScreen(
    yatraId: String,
    onBack: () -> Unit,
    onSelectTab: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val session = rememberAppSession()
    val context = LocalContext.current
    val viewModel: YatraTrackingViewModel = sahakarViewModel()
    var pasted by remember { mutableStateOf("") }
    var locationNote by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        locationNote = if (granted) null else "Location permission denied."
    }
    SignedInScaffold(
        title = "Yatra position",
        selectedTab = RathYatraDestinations.HOME,
        showAdmin = isStaffRole(session.role()),
        onSelectTab = onSelectTab,
        onBack = onBack,
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
            WhiteCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "This code is the tracker assignment. It is not your sign-in.",
                        color = RathYatraColors.Navy,
                        fontWeight = FontWeight.Medium,
                    )
                    if (!viewModel.hasCredential()) {
                        OutlinedTextField(
                            value = pasted,
                            onValueChange = { pasted = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Tracker code") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        )
                        SaffronButton(text = "Save tracker code", onClick = { viewModel.saveToken(pasted) })
                    } else {
                        Text("Tracker code is saved on this phone.", color = RathYatraColors.DeepGreen)
                    }
                    Text(
                        "Waiting to send: ${viewModel.pending}",
                        color = RathYatraColors.OnSurfaceVariant,
                    )
                    SaffronButton(
                        text = if (viewModel.sending) "Sending…" else "Send current location",
                        onClick = {
                            if (viewModel.sending) return@SaffronButton
                            val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                            if (fine != PackageManager.PERMISSION_GRANTED) {
                                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                return@SaffronButton
                            }
                            val fix = lastLocation(context)
                            if (fix == null) {
                                locationNote = "Location unavailable. Try again outdoors."
                                return@SaffronButton
                            }
                            locationNote = null
                            viewModel.capture(
                                yatraId = yatraId,
                                latitude = fix.latitude,
                                longitude = fix.longitude,
                                accuracyM = fix.accuracy.toDouble(),
                                mocked = isMockLocation(fix),
                                capturedAt = capturedAtUtc(fix.time),
                            )
                        },
                    )
                    viewModel.message?.let { message ->
                        Text(uiKnown(message), color = RathYatraColors.Navy)
                    }
                    locationNote?.let { note ->
                        Text(note, color = RathYatraColors.Emergency)
                    }
                }
            }
        }
    }
}

private fun lastLocation(context: Context): Location? {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) return null
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
        .maxByOrNull { it.time }
}

private fun isMockLocation(location: Location): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        location.isMock
    } else {
        @Suppress("DEPRECATION")
        location.isFromMockProvider
    }
}

private fun capturedAtUtc(epochMillis: Long): String {
    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    format.timeZone = TimeZone.getTimeZone("UTC")
    return format.format(Date(epochMillis))
}
