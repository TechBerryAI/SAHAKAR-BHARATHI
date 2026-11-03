package com.example.sahakarbharati.features.attendance

import android.Manifest
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.sahakarbharati.core.copy.LocalCopy
import com.example.sahakarbharati.core.copy.formatMobile
import com.example.sahakarbharati.core.copy.regionLabel
import com.example.sahakarbharati.core.ui.GhostNavyButton
import com.example.sahakarbharati.core.ui.SaffronButton
import com.example.sahakarbharati.core.ui.SignedInScaffold
import com.example.sahakarbharati.core.ui.WhiteCard
import com.example.sahakarbharati.data.BoothCode
import com.example.sahakarbharati.data.model.isStaffRole
import com.example.sahakarbharati.data.rememberAppSession
import com.example.sahakarbharati.data.remote.AttendanceRecordDto
import com.example.sahakarbharati.data.sahakarViewModel
import com.example.sahakarbharati.ui.navigation.RathYatraDestinations
import com.example.sahakarbharati.ui.theme.RathYatraColors
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.Executors

private val visitTime = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH)
private val india = ZoneId.of("Asia/Kolkata")

@Composable
fun AttendanceScreen(
    onBack: () -> Unit,
    onSelectTab: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val copy = LocalCopy.current
    val session = rememberAppSession()
    val viewModel: AttendanceViewModel = sahakarViewModel()
    val ui = viewModel.state
    var code by remember { mutableStateOf("") }
    var lockedCode by remember { mutableStateOf("") }
    var lockedUntil by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) { viewModel.load() }

    fun submit(raw: String, source: String) {
        val parsed = if (source == "qr") BoothCode.fromQr(raw) else BoothCode.fromManual(raw)
        if (parsed == null || ui.submitting) return
        val now = SystemClock.elapsedRealtime()
        if (parsed == lockedCode && now < lockedUntil) return
        lockedCode = parsed
        lockedUntil = now + 4_000
        viewModel.checkIn(parsed, source)
    }

    SignedInScaffold(
        title = copy.attendanceTitle,
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
            ui.latest?.let { visit ->
                VisitResultCard(visit, already = visit.alreadyRecorded)
            }
            ui.error?.takeIf { it.isNotBlank() }?.let { message ->
                Text(message, color = RathYatraColors.Emergency, style = MaterialTheme.typography.bodyMedium)
            }
            WhiteCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(copy.scannerActive, color = RathYatraColors.Navy, fontWeight = FontWeight.Medium, fontSize = 18.sp)
                    Text(copy.reticleHint, color = RathYatraColors.Navy.copy(alpha = 0.7f), fontSize = 14.sp)
                    BoothCamera(
                        enabled = !ui.submitting,
                        onCode = { submit(it, "qr") },
                    )
                }
            }
            WhiteCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(copy.pinTitle, color = RathYatraColors.Navy, fontWeight = FontWeight.Medium, fontSize = 18.sp)
                    Text(copy.pinBody, color = RathYatraColors.Navy.copy(alpha = 0.7f), fontSize = 14.sp)
                    OutlinedTextField(
                        value = code,
                        onValueChange = { next -> code = next.filter(Char::isDigit).take(6) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(copy.pinLabel) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    if (ui.submitting) {
                        CircularProgressIndicator(
                            color = RathYatraColors.Saffron,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    } else {
                        SaffronButton(
                            text = copy.submitPin,
                            onClick = {
                                submit(code, "code")
                            },
                        )
                    }
                    Text(copy.pinNote, color = RathYatraColors.Navy.copy(alpha = 0.7f), fontSize = 13.sp)
                }
            }
            WhiteCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(copy.historyTitle, color = RathYatraColors.Navy, fontWeight = FontWeight.Medium, fontSize = 18.sp)
                    if (ui.loading && ui.history.isEmpty()) {
                        CircularProgressIndicator(color = RathYatraColors.Saffron, modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else if (ui.history.isEmpty()) {
                        Text(copy.noVisitsYet, color = RathYatraColors.Navy.copy(alpha = 0.7f), fontSize = 14.sp)
                    } else {
                        ui.history.forEach { visit ->
                            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(visit.placeName, color = RathYatraColors.Navy, fontWeight = FontWeight.Medium)
                                Text(formatVisitTime(visit), color = RathYatraColors.Navy.copy(alpha = 0.7f), fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VisitResultCard(visit: AttendanceRecordDto, already: Boolean) {
    val copy = LocalCopy.current
    val region = regionLabel(visit.district, visit.state)
    WhiteCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                if (already) copy.alreadyHere else copy.markedHere,
                color = RathYatraColors.Navy,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
            )
            Text(visit.placeName, color = RathYatraColors.Saffron, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            Text(formatVisitTime(visit), color = RathYatraColors.Navy.copy(alpha = 0.7f), fontSize = 14.sp)
            Text(copy.recordedDetails, color = RathYatraColors.Navy.copy(alpha = 0.7f), fontSize = 13.sp)
            Text(visit.fullName, color = RathYatraColors.Navy, fontWeight = FontWeight.Medium)
            Text(formatMobile(visit.mobile), color = RathYatraColors.Navy, fontSize = 14.sp)
            if (region.isNotBlank()) {
                Text(region, color = RathYatraColors.Navy.copy(alpha = 0.7f), fontSize = 14.sp)
            }
            Text(visit.loginId, color = RathYatraColors.Navy.copy(alpha = 0.7f), fontSize = 13.sp)
        }
    }
}

@Composable
private fun BoothCamera(enabled: Boolean, onCode: (String) -> Unit) {
    val copy = LocalCopy.current
    val context = LocalContext.current
    val hasCamera = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    when {
        !hasCamera -> Text(copy.cameraOff, color = RathYatraColors.Navy.copy(alpha = 0.7f), fontSize = 14.sp)
        !granted -> GhostNavyButton(text = copy.allowCamera, onClick = { launcher.launch(Manifest.permission.CAMERA) })
        else -> CameraPreview(enabled = enabled, onCode = onCode)
    }
}

@Composable
private fun CameraPreview(enabled: Boolean, onCode: (String) -> Unit) {
    val copy = LocalCopy.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnCode by rememberUpdatedState(onCode)
    val enabledState by rememberUpdatedState(enabled)
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }
    val tapScanner = remember { BarcodeScanning.getClient() }
    val provider = remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var scanning by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<String?>(null) }
    DisposableEffect(scanner) {
        onDispose {
            provider.value?.unbindAll()
            executor.shutdown()
            scanner.close()
            tapScanner.close()
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AndroidView(
            factory = { viewContext ->
                val view = PreviewView(viewContext)
                previewView = view
                val future = ProcessCameraProvider.getInstance(viewContext)
                future.addListener(
                    {
                        val cameraProvider = future.get()
                        provider.value = cameraProvider
                        val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
                        val mainExecutor = ContextCompat.getMainExecutor(viewContext)
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        analysis.setAnalyzer(executor) { image ->
                            val media = image.image
                            if (media == null || !enabledState) {
                                image.close()
                                return@setAnalyzer
                            }
                            val input = InputImage.fromMediaImage(media, image.imageInfo.rotationDegrees)
                            scanner.process(input)
                                .addOnSuccessListener(mainExecutor) { codes ->
                                    val raw = codes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }?.rawValue
                                    if (!raw.isNullOrBlank()) currentOnCode(raw)
                                }
                                .addOnCompleteListener { image.close() }
                        }
                        runCatching {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                analysis,
                            )
                        }
                    },
                    ContextCompat.getMainExecutor(viewContext),
                )
                view
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            onRelease = { provider.value?.unbindAll() },
        )
        if (scanning) {
            CircularProgressIndicator(
                color = RathYatraColors.Saffron,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        } else {
            SaffronButton(
                text = copy.scanQr,
                onClick = {
                    if (!enabled || scanning) return@SaffronButton
                    val view = previewView
                    val bitmap = view?.bitmap
                    if (view == null || bitmap == null) {
                        hint = copy.scanMiss
                        return@SaffronButton
                    }
                    scanning = true
                    hint = null
                    val mainExecutor = ContextCompat.getMainExecutor(view.context)
                    tapScanner.process(InputImage.fromBitmap(bitmap, 0))
                        .addOnSuccessListener(mainExecutor) { codes ->
                            val raw = codes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }?.rawValue
                            scanning = false
                            if (raw.isNullOrBlank() || BoothCode.fromQr(raw) == null) {
                                hint = copy.scanMiss
                            } else {
                                hint = null
                                onCode(raw)
                            }
                        }
                        .addOnFailureListener(mainExecutor) {
                            scanning = false
                            hint = copy.scanMiss
                        }
                },
            )
        }
        hint?.let { message ->
            Text(message, color = RathYatraColors.Emergency, fontSize = 14.sp)
        }
    }
}

private fun formatVisitTime(visit: AttendanceRecordDto): String {
    val instant = runCatching { Instant.parse(visit.checkedInAt) }.getOrNull() ?: return visit.attendanceDate
    return visitTime.format(instant.atZone(india))
}
