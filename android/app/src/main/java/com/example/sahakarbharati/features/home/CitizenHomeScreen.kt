package com.example.sahakarbharati.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.IntrinsicSize
import com.example.sahakarbharati.core.copy.LocalCopy
import com.example.sahakarbharati.core.copy.LocalLanguage
import com.example.sahakarbharati.core.copy.formatMobile
import com.example.sahakarbharati.core.copy.participantCopy
import com.example.sahakarbharati.core.copy.personInitials
import com.example.sahakarbharati.core.copy.regionLabel
import com.example.sahakarbharati.core.copy.statusLabel
import com.example.sahakarbharati.core.ui.InitialAvatar
import com.example.sahakarbharati.core.ui.SignedInScaffold
import com.example.sahakarbharati.core.ui.StatusChip
import com.example.sahakarbharati.core.ui.UiState
import com.example.sahakarbharati.core.ui.WhiteCard
import com.example.sahakarbharati.core.ui.occupationLabel
import com.example.sahakarbharati.data.model.isStaffRole
import com.example.sahakarbharati.data.rememberAppSession
import com.example.sahakarbharati.data.sahakarViewModel
import com.example.sahakarbharati.ui.navigation.RathYatraDestinations
import com.example.sahakarbharati.ui.theme.RathYatraColors

@Composable
fun CitizenHomeScreen(
    onOpenAdmin: () -> Unit,
    onOpenBooths: () -> Unit,
    onOpenAttendance: () -> Unit,
    onSelectTab: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val copy = LocalCopy.current
    val text = participantCopy(LocalLanguage.current)
    val session = rememberAppSession()
    val homeViewModel: HomeViewModel = sahakarViewModel()
    val profile = (homeViewModel.state as? UiState.Success)?.data ?: session.cachedProfile()
    val staff = isStaffRole(profile?.role)
    LaunchedEffect(Unit) { homeViewModel.load() }

    val name = profile?.fullName?.ifBlank { session.name }.orEmpty().ifBlank { session.name }
    val first = name.trim().split(Regex("\\s+")).firstOrNull().orEmpty()
    val greeting = if (first.isBlank()) copy.greeting else "${copy.greeting}, $first"
    val region = regionLabel(profile?.district ?: session.district.ifBlank { null }, profile?.state ?: session.state.ifBlank { null })
    val occupation = occupationLabel(copy, profile?.occupation ?: session.occupation.ifBlank { null })
    val mobile = profile?.mobile?.ifBlank { session.mobile } ?: session.mobile
    val error = (homeViewModel.state as? UiState.Error)?.message
    val loading = homeViewModel.state is UiState.Loading && profile == null

    SignedInScaffold(
        title = copy.title,
        selectedTab = RathYatraDestinations.HOME,
        showAdmin = staff,
        onSelectTab = onSelectTab,
        showLanguage = true,
        modifier = modifier,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!error.isNullOrBlank() && profile == null) {
                Text(error, color = RathYatraColors.Emergency, style = MaterialTheme.typography.bodyMedium)
            }
            if (loading) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RathYatraColors.Saffron)
                }
            } else {
                WhiteCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            InitialAvatar(personInitials(name), size = 56.dp, fontSize = 18.sp)
                            Column {
                                Text(
                                    text.registeredParticipant.uppercase(),
                                    color = RathYatraColors.SaffronPressed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.4.sp,
                                )
                                Text(
                                    greeting,
                                    color = Color(0xFF17263F),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 30.sp,
                                )
                            }
                        }
                        if (occupation != null || region.isNotBlank()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (occupation != null) {
                                    StatusChip(occupation, container = Color(0x1A0F3D2E), content = Color(0xFF0F3D2E))
                                }
                                if (region.isNotBlank()) {
                                    StatusChip(region, container = Color(0x24E9872A), content = RathYatraColors.SaffronPressed)
                                }
                            }
                        }
                    }
                }
                Surface(
                    onClick = onOpenAttendance,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = Color.Transparent,
                    shadowElevation = 6.dp,
                ) {
                    Row(
                        Modifier
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF1D4D8F), Color(0xFF163A78), Color(0xFF102A58)),
                                ),
                            )
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Outlined.QrCodeScanner,
                                contentDescription = null,
                                tint = Color(0xFFFFD89A),
                                modifier = Modifier.size(26.dp),
                            )
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(copy.scanTab, color = Color(0xFFFFF8EE), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                            Text(copy.pinBody, color = Color(0xD1FFF8EE), fontSize = 13.sp, lineHeight = 18.sp)
                        }
                        Icon(
                            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                        )
                    }
                }
                if (staff) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        HomeShortcut(
                            title = "Registered users",
                            body = "Open the directory to read registration details.",
                            onClick = onOpenAdmin,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                        HomeShortcut(
                            title = "Booths",
                            body = "Create a booth, print its QR, and see who checked in.",
                            onClick = onOpenBooths,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
                WhiteCard(onClick = { onSelectTab(RathYatraDestinations.PROFILE) }) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text.account.uppercase(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.4.sp,
                                color = RathYatraColors.DeepGreenSoft,
                            )
                            Text(text.yourProfile, color = Color(0xFF17263F), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                text.profileStatus.replace("{status}", statusLabel(text, profile?.status)),
                                fontSize = 14.sp,
                                color = RathYatraColors.OnSurfaceVariant,
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFFB7A48A),
                        )
                    }
                }
                WhiteCard {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text.mobile.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.4.sp,
                            color = RathYatraColors.DeepGreenSoft,
                        )
                        Text(
                            if (mobile.isBlank()) text.mobileOnFile else formatMobile(mobile),
                            color = Color(0xFF17263F),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(text.mobileOnFileBody, fontSize = 14.sp, color = RathYatraColors.OnSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeShortcut(
    title: String,
    body: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WhiteCard(modifier = modifier, onClick = onClick) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "ADMIN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                color = RathYatraColors.SaffronPressed,
            )
            Text(title, color = Color(0xFF17263F), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp)
            Text(body, fontSize = 12.sp, lineHeight = 16.sp, color = RathYatraColors.OnSurfaceVariant)
        }
    }
}
