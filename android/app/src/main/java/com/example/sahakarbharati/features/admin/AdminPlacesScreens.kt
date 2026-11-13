package com.example.sahakarbharati.features.admin

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.print.PrintHelper
import com.example.sahakarbharati.core.copy.formatMobile
import com.example.sahakarbharati.core.copy.personInitials
import com.example.sahakarbharati.core.copy.regionLabel
import com.example.sahakarbharati.core.ui.GhostNavyButton
import com.example.sahakarbharati.core.ui.InitialAvatar
import com.example.sahakarbharati.core.ui.SaffronButton
import com.example.sahakarbharati.core.ui.SignedInScaffold
import com.example.sahakarbharati.core.ui.WhiteCard
import com.example.sahakarbharati.data.BoothCode
import com.example.sahakarbharati.data.model.isStaffRole
import com.example.sahakarbharati.data.rememberAppSession
import com.example.sahakarbharati.data.remote.AttendanceRecordDto
import com.example.sahakarbharati.data.remote.PlaceDto
import com.example.sahakarbharati.data.sahakarViewModel
import com.example.sahakarbharati.features.attendance.formatVisitTime
import com.example.sahakarbharati.ui.navigation.RathYatraDestinations
import com.example.sahakarbharati.ui.theme.PillShape
import com.example.sahakarbharati.ui.theme.RathYatraColors
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun AdminSectionSwitch(
    places: Boolean,
    onPeople: () -> Unit,
    onBooths: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xB8FFFCF8))
            .border(1.dp, RathYatraColors.GoldLine, RoundedCornerShape(999.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SectionPill("People", selected = !places, onClick = onPeople, modifier = Modifier.weight(1f))
        SectionPill("Booths", selected = places, onClick = onBooths, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SectionPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) Color(0xFF163A78) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color(0xFFFFF8EE) else RathYatraColors.OnSurfaceVariant,
        )
    }
}

@Composable
fun AdminPlacesScreen(
    onOpenPlace: (String) -> Unit,
    onOpenPeople: () -> Unit,
    onSelectTab: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val session = rememberAppSession()
    val viewModel: AdminPlacesViewModel = sahakarViewModel()
    SignedInScaffold(
        title = "Booths",
        selectedTab = RathYatraDestinations.ADMIN_PLACES,
        showAdmin = isStaffRole(session.role()),
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
            AdminSectionSwitch(places = true, onPeople = onOpenPeople, onBooths = {})
            OutlinedTextField(
                value = viewModel.name,
                onValueChange = viewModel::onNameChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Booth name") },
                placeholder = { Text("Pune stall") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
            SaffronButton(
                text = if (viewModel.saving) "Please wait…" else "Create booth",
                onClick = { if (!viewModel.saving) viewModel.create() },
            )
            viewModel.error?.let { message ->
                Text(message, color = RathYatraColors.Emergency, style = MaterialTheme.typography.bodyMedium)
            }
            if (viewModel.loading) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RathYatraColors.Saffron)
                }
            } else if (viewModel.places.isEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("No booths yet.", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Create one, then print its QR for the placard.",
                        color = RathYatraColors.OnSurfaceVariant,
                    )
                }
            } else {
                viewModel.places.forEach { place ->
                    PlaceRow(place, onClick = { onOpenPlace(place.id) })
                }
            }
        }
    }
}

@Composable
private fun PlaceRow(place: PlaceDto, onClick: () -> Unit) {
    WhiteCard(onClick = onClick) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InitialAvatar(personInitials(place.name), size = 40.dp, fontSize = 13.sp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(place.name, fontWeight = FontWeight.SemiBold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        place.code,
                        modifier = Modifier
                            .background(Color(0x14163A78), PillShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        color = Color(0xFF163A78),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                    )
                    Text(
                        if (place.active) "Accepting visits" else "Not accepting visits",
                        color = if (place.active) RathYatraColors.DeepGreen else RathYatraColors.OnSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = RathYatraColors.OnSurfaceVariant,
            )
        }
    }
}

@Composable
fun AdminPlaceScreen(
    placeId: String,
    onBack: () -> Unit,
    onOpenPeople: () -> Unit,
    onSelectTab: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val session = rememberAppSession()
    val context = LocalContext.current
    val viewModel: AdminPlaceViewModel = sahakarViewModel()
    LaunchedEffect(placeId) { viewModel.load(placeId) }
    val place = viewModel.place
    var qr by remember(place?.code) { mutableStateOf<Bitmap?>(null) }
    var qrFailed by remember(place?.code) { mutableStateOf(false) }
    LaunchedEffect(place?.code) {
        val code = place?.code ?: return@LaunchedEffect
        val bitmap = withContext(Dispatchers.Default) {
            runCatching { boothQrBitmap(BoothCode.text(code)) }.getOrNull()
        }
        qr = bitmap
        qrFailed = bitmap == null
    }

    SignedInScaffold(
        title = place?.name ?: "Booth",
        selectedTab = RathYatraDestinations.ADMIN_PLACE,
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
            AdminSectionSwitch(places = true, onPeople = onOpenPeople, onBooths = {})
            viewModel.error?.let { message ->
                Text(message, color = RathYatraColors.Emergency, style = MaterialTheme.typography.bodyMedium)
            }
            if (viewModel.loading && place == null) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RathYatraColors.Saffron)
                }
            } else if (place != null) {
                WhiteCard {
                    Column(
                        Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(place.name, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = RathYatraColors.Navy)
                        qr?.let { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Booth QR code",
                                modifier = Modifier.size(240.dp).background(Color.White),
                            )
                        }
                        if (qrFailed) {
                            Text(
                                "QR could not be drawn.",
                                color = RathYatraColors.Emergency,
                                textAlign = TextAlign.Center,
                            )
                        }
                        Text(
                            place.code,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp,
                            color = RathYatraColors.Navy,
                        )
                        Text(
                            "Scan this QR, or enter the code under it.",
                            color = RathYatraColors.OnSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                val image = qr
                SaffronButton(
                    text = "Print QR",
                    onClick = {
                        val placard = runCatching { boothPlacard(place.name, place.code) }.getOrNull() ?: image
                        if (placard == null) return@SaffronButton
                        PrintHelper(context).apply {
                            scaleMode = PrintHelper.SCALE_MODE_FIT
                        }.printBitmap(place.name, placard)
                    },
                )
                GhostNavyButton(
                    text = when {
                        viewModel.busy -> "Please wait…"
                        place.active -> "Stop accepting visits"
                        else -> "Accept visits again"
                    },
                    onClick = { if (!viewModel.busy) viewModel.toggleActive() },
                )
                Text("Who was here", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = RathYatraColors.Navy)
                Text(
                    "${viewModel.total} ${if (viewModel.total == 1) "visit" else "visits"}",
                    color = RathYatraColors.OnSurfaceVariant,
                )
                if (viewModel.visits.isEmpty()) {
                    Text("No one has checked in here yet.", color = RathYatraColors.OnSurfaceVariant)
                } else {
                    viewModel.visits.forEach { visit ->
                        VisitRow(visit)
                    }
                    if (viewModel.visits.size < viewModel.total) {
                        TextButton(
                            onClick = viewModel::loadMore,
                            enabled = !viewModel.busy,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (viewModel.busy) "Please wait…" else "Load more")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VisitRow(visit: AttendanceRecordDto) {
    val place = regionLabel(visit.district, visit.state)
    WhiteCard {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(visit.fullName, fontWeight = FontWeight.SemiBold)
            Text(
                buildString {
                    append(formatMobile(visit.mobile))
                    if (place.isNotBlank()) append(" · $place")
                },
                style = MaterialTheme.typography.bodySmall,
                color = RathYatraColors.OnSurfaceVariant,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(visit.loginId, style = MaterialTheme.typography.labelMedium)
                Text(
                    formatVisitTime(visit.checkedInAt, visit.attendanceDate),
                    style = MaterialTheme.typography.labelMedium,
                    color = RathYatraColors.OnSurfaceVariant,
                )
            }
        }
    }
}

private fun boothQrBitmap(text: String, size: Int = 640): Bitmap {
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(x, y, if (matrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE)
        }
    }
    return bitmap
}

/** Printed placard: the QR and the 6-digit code travel together. */
private fun boothPlacard(name: String, code: String): Bitmap {
    val qrSize = 640
    val matrix = QRCodeWriter().encode(BoothCode.text(code), BarcodeFormat.QR_CODE, qrSize, qrSize)
    val width = 760
    val qrTop = 120
    val height = qrTop + qrSize + 160
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(AndroidColor.WHITE)
    val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.BLACK
        textAlign = Paint.Align.CENTER
        textSize = 48f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    canvas.drawText(name, width / 2f, 72f, namePaint)
    val left = (width - qrSize) / 2
    for (x in 0 until qrSize) {
        for (y in 0 until qrSize) {
            if (matrix.get(x, y)) bitmap.setPixel(left + x, qrTop + y, AndroidColor.BLACK)
        }
    }
    val codePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.BLACK
        textAlign = Paint.Align.CENTER
        textSize = 84f
        letterSpacing = 0.2f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }
    canvas.drawText(code, width / 2f, (qrTop + qrSize + 110).toFloat(), codePaint)
    return bitmap
}
