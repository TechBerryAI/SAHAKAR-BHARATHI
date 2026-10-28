package com.example.sahakarbharati.features.home

import androidx.compose.foundation.layout.FlowRow
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.example.sahakarbharati.core.ui.occupationLabel
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Sos
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Traffic
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sahakarbharati.R
import com.example.sahakarbharati.core.copy.LocalCopy
import com.example.sahakarbharati.core.copy.LocalLanguage
import com.example.sahakarbharati.core.copy.AppLanguage
import com.example.sahakarbharati.ui.navigation.RathYatraDestinations
import com.example.sahakarbharati.core.ui.ApplySystemBarIcons
import com.example.sahakarbharati.core.ui.ProfilePhoto
import com.example.sahakarbharati.core.ui.UiState
import com.example.sahakarbharati.data.rememberAppSession
import com.example.sahakarbharati.data.sahakarViewModel
import com.example.sahakarbharati.features.home.HomeViewModel
import com.example.sahakarbharati.core.ui.CitizenTopBar
import com.example.sahakarbharati.core.ui.DemoAlertDialog
import com.example.sahakarbharati.core.ui.RathBottomBar
import com.example.sahakarbharati.core.ui.SectionHeader
import com.example.sahakarbharati.core.ui.StatusChip
import com.example.sahakarbharati.core.ui.WhiteCard
import com.example.sahakarbharati.ui.theme.RathYatraColors

@Composable
fun CitizenHomeScreen(
    onOpenTracking: () -> Unit,
    onOpenQr: () -> Unit,
    onOpenPass: () -> Unit,
    onOpenBenefits: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenNotifications: () -> Unit,
    onSelectTab: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ApplySystemBarIcons(lightIcons = false)
    val copy = LocalCopy.current
    val hindi = LocalLanguage.current == AppLanguage.HI
    val session = rememberAppSession()
    val homeViewModel: HomeViewModel = sahakarViewModel()
    var dialog by remember { mutableStateOf<String?>(null) }
    val profile = (homeViewModel.state as? UiState.Success)?.data ?: session.cachedProfile()
    val displayName = profile?.fullName?.ifBlank { session.name } ?: session.name
    val displayId = profile?.loginId?.ifBlank { session.loginId } ?: session.loginId
    val displayRole = occupationLabel(copy, profile?.occupation ?: session.occupation) ?: copy.roleChip
    val context = LocalContext.current
    val greeting = if (displayName.isBlank()) {
        copy.greeting
    } else {
        val first = displayName.trim().split(Regex("\\s+")).first()
        if (hindi) "जय किसान, $first" else "Jai Kisan, $first"
    }
    val initials = displayName.split(" ").filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "SB" }
    LaunchedEffect(Unit) { homeViewModel.load() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = RathYatraColors.Ivory,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                CitizenTopBar(
                    title = copy.homeTitle,
                    photoPath = session.displayPhotoPath,
                    onNotificationsClick = onOpenNotifications,
                    onProfileClick = onOpenProfile,
                )
            }
        },
        bottomBar = {
            RathBottomBar(selectedKey = RathYatraDestinations.HOME, onSelect = onSelectTab)
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
            WhiteCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfilePhoto(
                            photoPath = session.displayPhotoPath,
                            initials = initials,
                            size = 48.dp,
                            onClick = onOpenProfile,
                        )
                        Column(Modifier.weight(1f)) {
                            Text(greeting, style = MaterialTheme.typography.headlineSmall, color = RathYatraColors.Navy)
                            StatusChip(displayRole, container = RathYatraColors.Success.copy(alpha = 0.12f), content = RathYatraColors.Success)
                        }
                    }
                    // Wraps onto a second line with a long region or large font instead of clipping.
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StatusChip(
                            if (displayId.isBlank()) copy.homeId else displayId,
                            container = RathYatraColors.SurfaceContainerLow,
                            content = RathYatraColors.Navy,
                        )
                        // Only show the region when the participant gave one; an empty "—" chip looks broken.
                        profile?.region?.takeIf { it.isNotBlank() }?.let { region ->
                            StatusChip(
                                region,
                                container = RathYatraColors.SurfaceContainerLow,
                                content = RathYatraColors.OnSurfaceVariant,
                            )
                        }
                        StatusChip(copy.ePass, container = RathYatraColors.PrimaryFixed, content = RathYatraColors.BurntSaffron)
                    }
                    Text(
                        copy.sessionLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = RathYatraColors.OnSurfaceVariant,
                    )
                }
            }

            WhiteCard {
                Column {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(168.dp),
                    ) {
                        Image(
                            painterResource(R.drawable.img_home_hero),
                            contentDescription = copy.heroTitle,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, RathYatraColors.Navy.copy(alpha = 0.85f)),
                                    ),
                                ),
                        )
                        Row(
                            Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            StatusChip(copy.liveProcession, container = RathYatraColors.Navy.copy(alpha = 0.7f), content = Color.White)
                        }
                        StatusChip(
                            copy.locationChip,
                            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                            container = Color.White.copy(alpha = 0.9f),
                            content = RathYatraColors.Navy,
                        )
                        Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                            Text(copy.heroKicker, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
                            Text(copy.heroTitle, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                            Text(copy.heroSub, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Metric(copy.metricCrowd, copy.metricCrowdVal, copy.metricCrowdCap, Modifier.weight(1f))
                            Metric(copy.metricWeather, copy.metricWeatherVal, copy.metricWeatherCap, Modifier.weight(1f), Icons.Outlined.WbSunny)
                            Metric(copy.metricTravel, copy.metricTravelVal, copy.metricTravelCap, Modifier.weight(1f))
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(RathYatraColors.SurfaceContainerLow, RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(copy.upcomingRitual, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.OnSurfaceVariant)
                                Text(copy.upcomingRitualName, style = MaterialTheme.typography.titleSmall)
                            }
                            Text(copy.upcomingRitualTime, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.Saffron)
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(RathYatraColors.Saffron)
                                .clickable(onClick = onOpenTracking)
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(copy.liveRouteCta, style = MaterialTheme.typography.titleSmall, color = RathYatraColors.NavyDeep, modifier = Modifier.weight(1f))
                            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = RathYatraColors.NavyDeep)
                        }
                    }
                }
            }

            SectionHeader(copy.coreServices, trailing = copy.quickAccess)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ServiceTile(copy.tileTrack, copy.tileTrackBody, Icons.Outlined.Navigation, copy.tileTrackChip, RathYatraColors.OnSurfaceVariant, Modifier.weight(1f), onOpenTracking)
                ServiceTile(copy.tileQr, copy.tileQrBody, Icons.Outlined.QrCodeScanner, copy.tileQrChip, RathYatraColors.Navy, Modifier.weight(1f), onOpenQr)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ServiceTile(copy.tileBenefits, copy.tileBenefitsBody, Icons.Outlined.Savings, copy.tileBenefitsChip, RathYatraColors.GoldDark, Modifier.weight(1f), onOpenBenefits)
                ServiceTile(copy.tileVolunteer, copy.tileVolunteerBody, Icons.Outlined.VolunteerActivism, copy.tileVolunteerChip, RathYatraColors.Success, Modifier.weight(1f), onOpenBenefits)
            }

            WhiteCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(copy.trajectory, style = MaterialTheme.typography.titleSmall)
                        Text(copy.trajectorySub, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.OnSurfaceVariant)
                    }
                    LinearProgressIndicator(
                        progress = { 0f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = RathYatraColors.Saffron,
                        trackColor = RathYatraColors.SurfaceContainerHighest,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(copy.startPoint, style = MaterialTheme.typography.labelSmall)
                        Text(copy.currentPoint, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.Saffron)
                        Text(copy.endPoint, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(copy.advisories, style = MaterialTheme.typography.headlineSmall)
                Text(copy.viewAll, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.Saffron, modifier = Modifier.clickable { dialog = "advisories" })
            }
            Advisory(Icons.Outlined.Traffic, copy.adv1Source, copy.adv1Time, copy.adv1Title, copy.adv1Body)
            Advisory(Icons.Outlined.LocalHospital, copy.adv2Source, copy.adv2Time, copy.adv2Title, copy.adv2Body)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(copy.streamTitle, style = MaterialTheme.typography.titleSmall)
                StatusChip(copy.streamLive, container = RathYatraColors.SurfaceContainerLow, content = RathYatraColors.OnSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StreamThumb(R.drawable.img_stream_bada_danda, copy.stream1, Modifier.weight(1f))
                StreamThumb(R.drawable.img_stream_gundicha, copy.stream2, Modifier.weight(1f))
            }

            WhiteCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(copy.emergencyTitle, style = MaterialTheme.typography.titleSmall, color = RathYatraColors.Emergency)
                            Text(copy.emergencyToll, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.OnSurfaceVariant)
                        }
                    }
                    Text(
                        copy.emergencyBody,
                        style = MaterialTheme.typography.bodySmall,
                        color = RathYatraColors.OnSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Open the dialer with the number filled in; ACTION_DIAL needs no permission
                        // and the person still confirms the call.
                        EmergencyButton(copy.policeSos, Icons.Outlined.Sos, Modifier.weight(1f)) {
                            dialNumber(context, EMERGENCY_NUMBER) { dialog = "sos" }
                        }
                        EmergencyButton(copy.helpline, Icons.Outlined.SupportAgent, Modifier.weight(1f)) {
                            dialNumber(context, KISAN_CALL_CENTRE) { dialog = "help" }
                        }
                    }
                }
            }
        }
    }

    when (dialog) {
        "sos", "help" -> DemoAlertDialog(copy.sosTitle, copy.sosBody, { dialog = null })
        "advisories" -> DemoAlertDialog(copy.advisories, copy.comingSoonBody, { dialog = null })
    }
}

private const val EMERGENCY_NUMBER = "112"
private const val KISAN_CALL_CENTRE = "18001801551"

private fun dialNumber(context: Context, number: String, onUnavailable: () -> Unit) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        onUnavailable() // e.g. a tablet without a phone app: show the help text instead
    }
}

@Composable
private fun Metric(label: String, value: String, caption: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Column(modifier) {
        // Icon sits beside the label so all three metrics share one baseline.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (icon != null) Icon(icon, null, tint = RathYatraColors.Saffron, modifier = Modifier.size(14.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.OnSurfaceVariant)
        }
        // Shrinks instead of breaking a word across lines at large system font sizes.
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = MaterialTheme.typography.titleSmall.fontSize),
        )
        Text(caption, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.OnSurfaceVariant)
    }
}

@Composable
private fun ServiceTile(
    title: String,
    body: String,
    icon: ImageVector,
    chip: String,
    chipColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    WhiteCard(modifier = modifier, onClick = onClick) {
        Column(Modifier.padding(14.dp).height(120.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Icon(icon, contentDescription = title, tint = RathYatraColors.Navy)
                StatusChip(chip, container = chipColor.copy(alpha = 0.12f), content = chipColor)
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(body, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Advisory(icon: ImageVector, source: String, time: String, title: String, body: String) {
    WhiteCard {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, contentDescription = null, tint = RathYatraColors.Navy)
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(source, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.OnSurfaceVariant)
                    Text(time, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.Outline)
                }
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(body, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StreamThumb(res: Int, label: String, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(14.dp)).height(96.dp)) {
        // The photos carry their own place caption, so no overlay text is drawn on top of it.
        Image(painterResource(res), contentDescription = label, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

@Composable
private fun EmergencyButton(text: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(RathYatraColors.Emergency.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = text, tint = RathYatraColors.Emergency)
        Text(text, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.Emergency)
    }
}
