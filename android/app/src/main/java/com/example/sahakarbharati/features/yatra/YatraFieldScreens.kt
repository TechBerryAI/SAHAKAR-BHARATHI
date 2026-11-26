package com.example.sahakarbharati.features.yatra

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import com.example.sahakarbharati.core.copy.uiKnown
import com.example.sahakarbharati.core.ui.SaffronButton
import com.example.sahakarbharati.core.ui.SignedInScaffold
import com.example.sahakarbharati.core.ui.WhiteCard
import com.example.sahakarbharati.data.CounterQr
import com.example.sahakarbharati.data.model.isStaffRole
import com.example.sahakarbharati.data.rememberAppSession
import com.example.sahakarbharati.data.repository.AttendanceFix
import com.example.sahakarbharati.data.sahakarViewModel
import com.example.sahakarbharati.features.attendance.BoothCamera
import com.example.sahakarbharati.ui.navigation.RathYatraDestinations
import com.example.sahakarbharati.ui.theme.RathYatraColors
import java.time.Instant
import java.util.concurrent.Executors

@Composable
fun YatraAttendanceScreen(
    yatraId: String,
    onBack: () -> Unit,
    onSelectTab: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val session = rememberAppSession()
    val context = LocalContext.current
    val viewModel: YatraAttendanceViewModel = sahakarViewModel()
    val outcome = viewModel.outcome
    var locationError by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.values.none { it }) {
            locationError = "Location permission is required to mark attendance at a counter."
        }
    }
    LaunchedEffect(yatraId) {
        viewModel.loadMine(yatraId)
    }
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
                        uiKnown("Point at the counter QR. A booth QR is not a counter."),
                        color = RathYatraColors.Navy,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        uiKnown("Attendance needs a fresh location fix inside the counter geofence."),
                        color = RathYatraColors.OnSurfaceVariant,
                    )
                    BoothCamera(
                        enabled = outcome !is AttendanceOutcome.Submitting,
                        onCode = { raw ->
                            locationError = null
                            ensureLocationPermission(context, permissionLauncher) {
                                requestFreshFix(context) { fix ->
                                    if (fix == null) {
                                        locationError = "Location permission is required to mark attendance at a counter."
                                    } else {
                                        viewModel.scan(yatraId, raw, fix)
                                    }
                                }
                            }
                        },
                        onDigits = {},
                        accept = CounterQr::fromQr,
                    )
                    if (outcome is AttendanceOutcome.Submitting) {
                        CircularProgressIndicator(color = RathYatraColors.Saffron)
                    }
                }
            }
            locationError?.let { message ->
                Text(uiKnown(message), color = RathYatraColors.Emergency, style = MaterialTheme.typography.bodyLarge)
            }
            outcomeMessage(outcome)?.let { message ->
                Text(uiKnown(message), color = outcomeColor(outcome), style = MaterialTheme.typography.bodyLarge)
            }
            WhiteCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        uiKnown("My attendance"),
                        color = RathYatraColors.Navy,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (viewModel.mine.isEmpty()) {
                        Text(uiKnown("No attendance recorded yet."), color = RathYatraColors.OnSurfaceVariant)
                    } else {
                        viewModel.mine.forEach { row ->
                            Text(
                                "${row.counterName} · ${row.checkedInAt}",
                                color = RathYatraColors.OnSurfaceVariant,
                            )
                        }
                    }
                    if (viewModel.mineHasMore) {
                        SaffronButton(
                            text = uiKnown("Load more"),
                            onClick = { viewModel.loadMoreMine(yatraId) },
                        )
                    }
                }
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
    var tracking by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val fine = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!fine && !coarse) {
            locationNote = "Location permission is required to track this Yatra."
        }
    }
    LaunchedEffect(Unit) {
        viewModel.refreshQueue()
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
                        uiKnown("This code is the tracker assignment. It is not your sign-in."),
                        color = RathYatraColors.Navy,
                        fontWeight = FontWeight.Medium,
                    )
                    if (!viewModel.hasCredential()) {
                        OutlinedTextField(
                            value = pasted,
                            onValueChange = { pasted = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(uiKnown("Tracker code")) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        )
                        SaffronButton(
                            text = uiKnown("Save tracker code"),
                            onClick = { viewModel.saveToken(pasted) },
                        )
                    } else {
                        Text(
                            uiKnown("Tracker code is saved on this phone."),
                            color = RathYatraColors.DeepGreen,
                        )
                        SaffronButton(
                            text = uiKnown("Remove tracker from this phone"),
                            onClick = {
                                YatraTrackingService.stop(context)
                                tracking = false
                                viewModel.removeTrackerFromPhone()
                            },
                        )
                    }
                    Text(
                        uiKnown("Waiting to send: ${viewModel.pending}"),
                        color = RathYatraColors.OnSurfaceVariant,
                    )
                    if (viewModel.drops > 0) {
                        Text(
                            uiKnown("Dropped fixes: ${viewModel.drops}"),
                            color = RathYatraColors.OnSurfaceVariant,
                        )
                    }
                    if (!locationEnabled(context)) {
                        Text(
                            uiKnown("Location is turned off on this phone."),
                            color = RathYatraColors.Emergency,
                        )
                    }
                    if (tracking) {
                        SaffronButton(
                            text = uiKnown("Stop tracking"),
                            onClick = {
                                YatraTrackingService.stop(context)
                                tracking = false
                                locationNote = "Tracking stopped."
                            },
                        )
                    } else {
                        SaffronButton(
                            text = uiKnown("Start tracking"),
                            onClick = {
                                if (!viewModel.hasCredential()) {
                                    locationNote = "Paste the tracker code from the Yatra desk."
                                    return@SaffronButton
                                }
                                ensureLocationPermission(context, permissionLauncher) {
                                    if (!locationEnabled(context)) {
                                        locationNote = "Location is turned off on this phone."
                                        return@ensureLocationPermission
                                    }
                                    locationNote = null
                                    YatraTrackingService.start(context, yatraId)
                                    tracking = true
                                    viewModel.refreshQueue()
                                }
                            },
                        )
                    }
                    viewModel.message?.let { message ->
                        Text(uiKnown(message), color = RathYatraColors.Navy)
                    }
                    locationNote?.let { note ->
                        Text(uiKnown(note), color = RathYatraColors.Emergency)
                    }
                }
            }
        }
    }
}

private fun locationEnabled(context: Context): Boolean {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return LocationManagerCompat.isLocationEnabled(manager)
}

private fun ensureLocationPermission(
    context: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    onGranted: () -> Unit,
) {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    val needNotify = Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
    if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        if (needNotify) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        launcher.launch(perms.toTypedArray())
        return
    }
    if (needNotify) {
        launcher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
        return
    }
    onGranted()
}

private fun requestFreshFix(context: Context, onResult: (AttendanceFix?) -> Unit) {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
        onResult(null)
        return
    }
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val executor = Executors.newSingleThreadExecutor()
    val signal = CancellationSignal()
    LocationManagerCompat.getCurrentLocation(
        manager,
        LocationManager.GPS_PROVIDER,
        signal,
        executor,
    ) { location ->
        if (location == null || isMockLocation(location)) {
            onResult(null)
        } else {
            onResult(
                AttendanceFix(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracyM = location.accuracy.toDouble(),
                    capturedAt = Instant.ofEpochMilli(location.time).toString(),
                ),
            )
        }
        executor.shutdown()
    }
}

private fun isMockLocation(location: Location): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        location.isMock
    } else {
        @Suppress("DEPRECATION")
        location.isFromMockProvider
    }
}
