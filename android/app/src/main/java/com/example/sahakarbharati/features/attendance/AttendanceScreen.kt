package com.example.sahakarbharati.features.attendance

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.RectF
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.view.transform.CoordinateTransform
import androidx.camera.view.transform.ImageProxyTransformFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
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
                .imePadding(),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
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
                        onDigits = { code = it },
                    )
                    if (ui.submitting) {
                        CircularProgressIndicator(
                            color = RathYatraColors.Saffron,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    } else {
                        SaffronButton(
                            text = copy.submitPin,
                            onClick = { submit(code, "code") },
                        )
                    }
                }
            }
            ui.latest?.let { visit ->
                VisitResultCard(visit, already = visit.alreadyRecorded)
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
private fun BoothCamera(enabled: Boolean, onCode: (String) -> Unit, onDigits: (String) -> Unit) {
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
        else -> CameraPreview(enabled = enabled, onCode = onCode, onDigits = onDigits)
    }
}

@Composable
private fun CameraPreview(enabled: Boolean, onCode: (String) -> Unit, onDigits: (String) -> Unit) {
    val copy = LocalCopy.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnCode by rememberUpdatedState(onCode)
    val currentOnDigits by rememberUpdatedState(onDigits)
    val enabledState by rememberUpdatedState(enabled)
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }
    val provider = remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var frameReady by remember { mutableStateOf(false) }
    var latestRaw by remember { mutableStateOf<String?>(null) }
    var seenCode by remember { mutableStateOf<String?>(null) }
    var codeAnchor by remember { mutableStateOf<RectF?>(null) }
    val previewView = remember { mutableStateOf<PreviewView?>(null) }
    var foreignQr by remember { mutableStateOf(false) }
    var cameraFailed by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<String?>(null) }
    DisposableEffect(scanner) {
        onDispose {
            provider.value?.unbindAll()
            executor.shutdown()
            scanner.close()
        }
    }
    if (cameraFailed) {
        Text(copy.cameraOff, color = RathYatraColors.Navy.copy(alpha = 0.7f), fontSize = 14.sp)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(240.dp),
        ) {
        AndroidView(
            factory = { viewContext ->
                val view = PreviewView(viewContext).apply {
                    // The check-in page scrolls. The default surface preview stays black there.
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                previewView.value = view
                val future = ProcessCameraProvider.getInstance(viewContext)
                val mainExecutor = ContextCompat.getMainExecutor(viewContext)
                future.addListener(
                    {
                        view.post {
                            val cameraProvider = runCatching { future.get() }.getOrNull()
                            if (cameraProvider == null) {
                                cameraFailed = true
                                return@post
                            }
                            provider.value = cameraProvider
                            val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
                            val analysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                            analysis.setAnalyzer(executor) { image ->
                                val media = image.image
                                if (media == null || !enabledState) {
                                    image.close()
                                    return@setAnalyzer
                                }
                                try {
                                    val input = InputImage.fromMediaImage(media, image.imageInfo.rotationDegrees)
                                    scanner.process(input)
                                        .addOnSuccessListener(mainExecutor) { codes ->
                                            frameReady = true
                                            val barcode = codes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }
                                            val raw = barcode?.rawValue
                                            latestRaw = raw
                                            val digits = raw?.let { BoothCode.fromQr(it) }
                                            seenCode = digits
                                            foreignQr = !raw.isNullOrBlank() && digits == null
                                            codeAnchor = qrAnchor(previewView.value, image, barcode?.boundingBox)
                                            if (!raw.isNullOrBlank() && digits != null) {
                                                hint = null
                                                currentOnDigits(digits)
                                                currentOnCode(raw)
                                            }
                                        }
                                        .addOnFailureListener(mainExecutor) {
                                            frameReady = true
                                        }
                                        .addOnCompleteListener { image.close() }
                                } catch (_: RuntimeException) {
                                    image.close()
                                }
                            }
                            val bound = bindCamera(cameraProvider, lifecycleOwner, preview, analysis, CameraSelector.DEFAULT_BACK_CAMERA) ||
                                bindCamera(cameraProvider, lifecycleOwner, preview, analysis, CameraSelector.DEFAULT_FRONT_CAMERA)
                            if (!bound) cameraFailed = true
                        }
                    },
                    mainExecutor,
                )
                view
            },
            modifier = Modifier.fillMaxSize(),
            onRelease = { provider.value?.unbindAll() },
        )
        val anchor = codeAnchor
        val digits = seenCode
        if (anchor != null && digits != null && anchor.width() > 0f) {
            val density = LocalDensity.current
            Column(
                Modifier
                    .offset { IntOffset(anchor.left.toInt(), anchor.bottom.toInt()) }
                    .width(with(density) { anchor.width().toDp() })
                    .background(Color.White)
                    .padding(top = 2.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    digits,
                    color = RathYatraColors.Navy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = 2.sp,
                )
            }
        }
        }
        if (foreignQr && seenCode == null) {
            Text(copy.scanMiss, color = RathYatraColors.Emergency, fontSize = 14.sp)
        }
        SaffronButton(
            text = copy.scanQr,
            onClick = {
                if (!enabled) return@SaffronButton
                if (!frameReady) {
                    hint = copy.scanStarting
                    return@SaffronButton
                }
                val raw = latestRaw
                if (raw.isNullOrBlank() || BoothCode.fromQr(raw) == null) {
                    hint = copy.scanMiss
                } else {
                    hint = null
                    onCode(raw)
                }
            },
        )
        hint?.let { message ->
            Text(message, color = RathYatraColors.Emergency, fontSize = 14.sp)
        }
    }
}

private fun qrAnchor(view: PreviewView?, image: ImageProxy, box: android.graphics.Rect?): RectF? {
    if (view == null || box == null) return null
    val target = view.outputTransform ?: return null
    val source = ImageProxyTransformFactory().apply { isUsingRotationDegrees = true }.getOutputTransform(image) ?: return null
    val mapped = RectF(box)
    CoordinateTransform(source, target).mapRect(mapped)
    if (mapped.width() < 1f || mapped.height() < 1f) return null
    return mapped
}

private fun bindCamera(
    cameraProvider: ProcessCameraProvider,
    lifecycleOwner: LifecycleOwner,
    preview: Preview,
    analysis: ImageAnalysis,
    selector: CameraSelector,
): Boolean {
    val available = runCatching { cameraProvider.hasCamera(selector) }.getOrDefault(false)
    if (!available) return false
    return runCatching {
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
        true
    }.isSuccess
}

private fun formatVisitTime(visit: AttendanceRecordDto): String {
    val instant = runCatching { Instant.parse(visit.checkedInAt) }.getOrNull() ?: return visit.attendanceDate
    return visitTime.format(instant.atZone(india))
}
