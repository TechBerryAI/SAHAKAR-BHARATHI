package com.example.sahakarbharati.features.attendance

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Outline
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
    val hostView = LocalView.current
    SideEffect {
        hostView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
    }
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
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ui.error?.takeIf { it.isNotBlank() }?.let { message ->
                Text(message, color = RathYatraColors.Emergency, style = MaterialTheme.typography.bodyMedium)
            }
            WhiteCard {
                Column(
                    Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(copy.scannerActive, color = RathYatraColors.Navy, fontWeight = FontWeight.Medium, fontSize = 18.sp)
                        Text(
                            "Scan the QR on the booth banner, or type the 6-digit code printed under it.",
                            color = RathYatraColors.Navy.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                        )
                    }
                    BoothCamera(
                        enabled = !ui.submitting,
                        onCode = { submit(it, "qr") },
                        onDigits = {},
                    )
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
                            onClick = { submit(code, "code") },
                        )
                    }
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
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
                Spacer(Modifier.height(24.dp))
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
    val currentOnDigits by rememberUpdatedState(onDigits)
    val enabledState by rememberUpdatedState(enabled)
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }
    val provider = remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var frameReady by remember { mutableStateOf(false) }
    var latestRaw by remember { mutableStateOf<String?>(null) }
    var seenCode by remember { mutableStateOf<String?>(null) }
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
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val previewShape = RoundedCornerShape(16.dp)
        Box(
            Modifier
                .width(168.dp)
                .height(224.dp)
                .graphicsLayer { clip = true; shape = previewShape }
                .clip(previewShape),
        ) {
        AndroidView(
            factory = { viewContext ->
                val frame = FrameLayout(viewContext).apply {
                    clipChildren = true
                    clipToPadding = true
                }
                val view = PreviewView(viewContext).apply {
                    // A surface preview stays black inside Compose. Texture mode stays inside this frame.
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FIT_CENTER
                    outlineProvider = object : ViewOutlineProvider() {
                        override fun getOutline(view: View, outline: Outline) {
                            val radius = 16f * view.resources.displayMetrics.density
                            outline.setRoundRect(0, 0, view.width, view.height, radius)
                        }
                    }
                    clipToOutline = true
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
                frame.addView(view)
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
                                            if (!raw.isNullOrBlank() && digits != null) {
                                                hint = null
                                                currentOnDigits(digits)
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
                frame
            },
            modifier = Modifier.fillMaxSize(),
            onRelease = { provider.value?.unbindAll() },
        )
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
