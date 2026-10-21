package com.example.sahakarbharati.features.attendance

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.sahakarbharati.core.copy.LocalCopy
import com.example.sahakarbharati.ui.navigation.RathYatraDestinations
import com.example.sahakarbharati.data.rememberAppSession
import com.example.sahakarbharati.core.ui.ApplySystemBarIcons
import com.example.sahakarbharati.core.ui.CitizenTopBar
import com.example.sahakarbharati.core.ui.DemoAlertDialog
import com.example.sahakarbharati.core.ui.PulseDot
import com.example.sahakarbharati.core.ui.RathBottomBar
import com.example.sahakarbharati.core.ui.SaffronButton
import com.example.sahakarbharati.core.ui.StatusChip
import com.example.sahakarbharati.core.ui.WhiteCard
import com.example.sahakarbharati.ui.theme.PillShape
import com.example.sahakarbharati.ui.theme.RathYatraColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrAttendanceScreen(
    showPassInitially: Boolean,
    onSelectTab: (String) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ApplySystemBarIcons(lightIcons = false)
    val copy = LocalCopy.current
    val session = rememberAppSession()
    var showPass by rememberSaveable { mutableStateOf(showPassInitially) }
    var torchOn by rememberSaveable { mutableStateOf(false) }
    var showPin by rememberSaveable { mutableStateOf(false) }
    var pin by rememberSaveable { mutableStateOf("") }
    var dialog by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = RathYatraColors.Ivory,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                CitizenTopBar(
                    title = copy.attendanceTitle,
                    photoPath = session.displayPhotoPath,
                    onRoleClick = { dialog = "role" },
                    onNotificationsClick = onOpenNotifications,
                    onProfileClick = onOpenProfile,
                )
            }
        },
        bottomBar = {
            RathBottomBar(selectedKey = RathYatraDestinations.QR_ATTENDANCE, onSelect = onSelectTab)
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PulseDot(RathYatraColors.Saffron)
                    Text(copy.scannerActive, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.Saffron, fontWeight = FontWeight.Bold)
                }
                Row {
                    IconButton(onClick = { torchOn = !torchOn }) {
                        Icon(
                            if (torchOn) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                            contentDescription = if (torchOn) "Torch on" else "Torch off",
                        )
                    }
                    IconButton(onClick = { /* visual only */ }) {
                        Icon(Icons.Outlined.Cameraswitch, contentDescription = "Flip camera, visual only")
                    }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(PillShape)
                    .background(RathYatraColors.SurfaceContainerLow)
                    .padding(4.dp),
            ) {
                SegmentTab(copy.scanTab, Icons.Outlined.QrCodeScanner, !showPass, Modifier.weight(1f)) { showPass = false }
                SegmentTab(copy.passTab, Icons.Outlined.QrCode2, showPass, Modifier.weight(1f)) { showPass = true }
            }

            if (showPass) {
                WhiteCard {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(copy.pilgrimName, style = MaterialTheme.typography.headlineSmall, color = RathYatraColors.Navy)
                        Text(copy.pilgrimReg, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
                        StatusChip(copy.verified, container = RathYatraColors.Success.copy(alpha = 0.12f), content = RathYatraColors.Success)
                        Box(
                            Modifier
                                .size(180.dp)
                                .background(RathYatraColors.Ivory, RoundedCornerShape(12.dp))
                                .border(1.dp, RathYatraColors.Navy, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            SampleQrGraphic()
                        }
                        Text(copy.passHint, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(RathYatraColors.NavyDeep),
                ) {
                    StatusChip(
                        copy.counterChip,
                        modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
                        container = RathYatraColors.Navy.copy(alpha = 0.7f),
                        content = Color.White,
                    )
                    Canvas(Modifier.fillMaxSize()) {
                        val pad = size.minDimension * 0.18f
                        val len = size.minDimension * 0.12f
                        val stroke = 8f
                        val c = RathYatraColors.SaffronBright
                        val corners = listOf(
                            Offset(pad, pad) to listOf(Offset(pad + len, pad), Offset(pad, pad + len)),
                            Offset(size.width - pad, pad) to listOf(Offset(size.width - pad - len, pad), Offset(size.width - pad, pad + len)),
                            Offset(pad, size.height - pad) to listOf(Offset(pad + len, size.height - pad), Offset(pad, size.height - pad - len)),
                            Offset(size.width - pad, size.height - pad) to listOf(Offset(size.width - pad - len, size.height - pad), Offset(size.width - pad, size.height - pad - len)),
                        )
                        corners.forEach { (origin, arms) ->
                            arms.forEach { end ->
                                drawLine(c, origin, end, stroke, StrokeCap.Round)
                            }
                        }
                        drawLine(
                            c,
                            Offset(size.width / 2f, pad + 20f),
                            Offset(size.width / 2f, size.height - pad - 20f),
                            3f,
                        )
                        drawLine(
                            c,
                            Offset(pad + 20f, size.height / 2f),
                            Offset(size.width - pad - 20f, size.height / 2f),
                            3f,
                        )
                    }
                    Text(
                        copy.reticleHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            WhiteCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = RathYatraColors.Success)
                            Text(copy.lastRegistered, style = MaterialTheme.typography.titleSmall)
                        }
                        StatusChip(copy.recorded, container = RathYatraColors.Success.copy(alpha = 0.12f), content = RathYatraColors.Success)
                    }
                    Text(copy.lastPlace, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
                    StatusChip(copy.lastChip, container = RathYatraColors.GoldPale.copy(alpha = 0.5f), content = RathYatraColors.GoldDark)
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(copy.historyTitle, style = MaterialTheme.typography.titleSmall)
                Text(copy.historyCount, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.OnSurfaceVariant)
            }
            HistoryRow(Icons.Outlined.Flag, copy.hist1, copy.hist1Sub)
            HistoryRow(Icons.Outlined.DirectionsBus, copy.hist2, copy.hist2Sub)

            WhiteCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.AutoMirrored.Outlined.Help, contentDescription = null, tint = RathYatraColors.Navy)
                        Text(copy.scanHelp, style = MaterialTheme.typography.titleSmall)
                    }
                    Text(
                        copy.scanHelpBody,
                        style = MaterialTheme.typography.bodySmall,
                        color = RathYatraColors.OnSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusChip(copy.enterPin, container = RathYatraColors.PrimaryFixed, content = RathYatraColors.BurntSaffron)
                        StatusChip(copy.report, container = RathYatraColors.SurfaceContainerLow, content = RathYatraColors.Navy)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(
                            copy.enterPin,
                            style = MaterialTheme.typography.labelLarge,
                            color = RathYatraColors.Navy,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showPin = true }
                                .padding(8.dp),
                        )
                        Text(
                            copy.report,
                            style = MaterialTheme.typography.labelLarge,
                            color = RathYatraColors.Emergency,
                            modifier = Modifier.clickable { dialog = "report" }.padding(8.dp),
                        )
                    }
                }
            }
        }
    }

    if (showPin) {
        ModalBottomSheet(
            onDismissRequest = { showPin = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = RathYatraColors.Ivory,
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(copy.pinTitle, style = MaterialTheme.typography.headlineSmall)
                Text(copy.pinBody, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6) pin = it.filter { ch -> ch.isDigit() } },
                    label = { Text(copy.pinLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    leadingIcon = { Icon(Icons.Outlined.Pin, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                )
                SaffronButton(text = copy.submitPin, onClick = { showPin = false; pin = "" })
                Text(copy.pinNote, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    when (dialog) {
        "role" -> DemoAlertDialog(copy.adminTitle, copy.adminBody, { dialog = null })
        "report" -> DemoAlertDialog(copy.report, copy.comingSoonBody, { dialog = null })
    }
}

@Composable
private fun SegmentTab(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .clip(PillShape)
            .background(if (selected) RathYatraColors.SaffronBright else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) RathYatraColors.NavyDeep else RathYatraColors.OnSurfaceVariant, modifier = Modifier.size(16.dp))
        Text(" $label", style = MaterialTheme.typography.labelLarge, color = if (selected) RathYatraColors.NavyDeep else RathYatraColors.OnSurfaceVariant)
    }
}

@Composable
private fun HistoryRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    WhiteCard {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(RathYatraColors.SurfaceContainerLow), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = RathYatraColors.Navy)
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
            }
            StatusChip("Verified", container = RathYatraColors.Success.copy(alpha = 0.12f), content = RathYatraColors.Success)
        }
    }
}

@Composable
private fun SampleQrGraphic() {
    Canvas(Modifier.size(140.dp)) {
        val cells = 7
        val cell = size.minDimension / cells
        for (x in 0 until cells) {
            for (y in 0 until cells) {
                val dark = (x + y) % 2 == 0 || (x < 2 && y < 2) || (x > cells - 3 && y < 2) || (x < 2 && y > cells - 3)
                if (dark) {
                    drawRect(
                        color = RathYatraColors.Navy,
                        topLeft = Offset(x * cell, y * cell),
                        size = androidx.compose.ui.geometry.Size(cell * 0.92f, cell * 0.92f),
                    )
                }
            }
        }
    }
}
