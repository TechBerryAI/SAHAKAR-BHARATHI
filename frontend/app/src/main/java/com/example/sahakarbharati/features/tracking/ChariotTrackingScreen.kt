package com.example.sahakarbharati.features.tracking

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sahakarbharati.R
import com.example.sahakarbharati.core.copy.LocalCopy
import com.example.sahakarbharati.core.ui.ApplySystemBarIcons
import com.example.sahakarbharati.core.ui.DemoAlertDialog
import com.example.sahakarbharati.core.ui.PulseDot
import com.example.sahakarbharati.core.ui.StackTopBar
import com.example.sahakarbharati.core.ui.StatusChip
import com.example.sahakarbharati.core.ui.WhiteCard
import com.example.sahakarbharati.ui.theme.RathYatraColors

@Composable
fun ChariotTrackingScreen(
    onBack: () -> Unit,
    onProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ApplySystemBarIcons(lightIcons = false)
    val copy = LocalCopy.current
    var refreshing by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RathYatraColors.Ivory)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        StackTopBar(
            title = copy.trackTitle,
            onBack = onBack,
            onSosClick = { dialog = "sos" },
            onProfileClick = onProfile,
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PulseDot(RathYatraColors.Saffron)
                    Text(copy.telemetryLive, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.Saffron, fontWeight = FontWeight.Bold)
                    Text(copy.lastSynced, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
                }
                IconButton(
                    onClick = { refreshing = !refreshing },
                    modifier = Modifier.rotate(if (refreshing) 180f else 0f),
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = copy.telemetryLive)
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(RathYatraColors.Navy)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(copy.traversal, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.OnNavyMuted)
                    Text(copy.distanceTo, style = MaterialTheme.typography.titleSmall, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(copy.estArrival, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.OnNavyMuted)
                    Text(copy.arrivalTime, style = MaterialTheme.typography.titleSmall, color = RathYatraColors.GoldMuted)
                }
            }

            WhiteCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(copy.corridor, style = MaterialTheme.typography.titleSmall)
                        StatusChip(copy.corridorChip, container = RathYatraColors.SurfaceContainerLow, content = RathYatraColors.Navy)
                    }
                    RouteSchematic(listOf(copy.node1, copy.node2, copy.node3, copy.node4, copy.node5))
                    Box(Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(12.dp))) {
                        Image(
                            painterResource(R.drawable.img_tracking_map),
                            contentDescription = copy.mapLabel,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                        Row(
                            Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            StatusChip(copy.mapLabel, container = RathYatraColors.Navy.copy(alpha = 0.75f), content = Color.White)
                            StatusChip(copy.feedLabel, container = RathYatraColors.Navy.copy(alpha = 0.75f), content = Color.White)
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(copy.chariotSection, style = MaterialTheme.typography.headlineSmall)
                StatusChip(copy.activeCount, container = RathYatraColors.PrimaryFixed, content = RathYatraColors.BurntSaffron)
            }
            ChariotCard(copy.rath1, copy.rath1Sub, copy.rath1Spec, copy.rath1Status, RathYatraColors.Emergency, copy.rath1Loc, copy.rath1Speed, copy.rath1Next)
            ChariotCard(copy.rath2, copy.rath2Sub, copy.rath2Spec, copy.rath2Status, RathYatraColors.GoldDark, copy.rath2Loc, copy.rath2Speed, copy.rath2Next)
            ChariotCard(copy.rath3, copy.rath3Sub, copy.rath3Spec, copy.rath3Status, RathYatraColors.Success, copy.rath3Loc, copy.rath3Speed, copy.rath3Next)

            WhiteCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(copy.amenities, style = MaterialTheme.typography.titleSmall)
                        StatusChip(copy.radius, container = RathYatraColors.SurfaceContainerLow, content = RathYatraColors.OnSurfaceVariant)
                    }
                    Amenity(Icons.Outlined.WaterDrop, copy.water, copy.waterBody, copy.waterChip)
                    Amenity(Icons.Outlined.LocalHospital, copy.medical, copy.medicalBody, copy.medicalChip)
                    Amenity(Icons.Outlined.Call, copy.lost, copy.lostBody, copy.lostChip)
                }
            }

            Box(Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(16.dp))) {
                Image(
                    painterResource(R.drawable.img_tracking_corridor),
                    contentDescription = copy.photoCaption,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(RathYatraColors.Navy.copy(alpha = 0.55f))
                        .padding(10.dp),
                ) {
                    Text(copy.photoCaption, style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
            Text(copy.gpsNote, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.Outline)
        }
    }
    if (dialog == "sos") {
        DemoAlertDialog(copy.sosTitle, copy.sosBody, { dialog = null })
    }
}

@Composable
private fun RouteSchematic(labels: List<String>) {
    Column {
        Canvas(Modifier.fillMaxWidth().height(28.dp)) {
            val y = size.height / 2f
            val count = 5
            val start = 16f
            val end = size.width - 16f
            drawLine(RathYatraColors.SurfaceContainerHighest, Offset(start, y), Offset(end, y), 8f, StrokeCap.Round)
            drawLine(RathYatraColors.SaffronBright, Offset(start, y), Offset(start + (end - start) * 0.32f, y), 8f, StrokeCap.Round)
            for (i in 0 until count) {
                val x = start + (end - start) * (i / (count - 1f))
                val color = when (i) {
                    0 -> RathYatraColors.Navy
                    1 -> RathYatraColors.SaffronBright
                    else -> RathYatraColors.OutlineCool
                }
                drawCircle(color, radius = if (i == 1) 10f else 7f, center = Offset(x, y))
                drawCircle(Color.White, radius = 3.5f, center = Offset(x, y))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEach { Text(it, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.OnSurfaceVariant) }
        }
    }
}

@Composable
private fun ChariotCard(
    name: String,
    deity: String,
    spec: String,
    status: String,
    statusColor: Color,
    location: String,
    speed: String,
    extra: String,
) {
    WhiteCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(name, style = MaterialTheme.typography.titleSmall)
                    Text("($deity)", style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
                    Text(spec, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
                }
                StatusChip(status, container = statusColor.copy(alpha = 0.12f), content = statusColor, leadingDot = statusColor)
            }
            Text("Current Location  $location", style = MaterialTheme.typography.bodySmall)
            Text("Velocity  $speed", style = MaterialTheme.typography.bodySmall)
            Text(extra, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
        }
    }
}

@Composable
private fun Amenity(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String, chip: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(RathYatraColors.SurfaceContainerLow), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = RathYatraColors.Navy)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
        }
        StatusChip(chip, container = RathYatraColors.Success.copy(alpha = 0.12f), content = RathYatraColors.Success)
    }
}
