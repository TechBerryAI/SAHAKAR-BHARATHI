package com.example.sahakarbharati.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import com.example.sahakarbharati.core.copy.LocalCopy
import com.example.sahakarbharati.core.copy.LocalLanguage
import com.example.sahakarbharati.core.copy.formatMobile
import com.example.sahakarbharati.core.copy.participantCopy
import com.example.sahakarbharati.core.copy.personInitials
import com.example.sahakarbharati.core.copy.regionLabel
import com.example.sahakarbharati.core.copy.statusLabel
import com.example.sahakarbharati.core.ui.SaffronButton
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
        title = copy.tabHome,
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0F3D2E)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(personInitials(name), color = Color.White, fontWeight = FontWeight.Medium)
                            }
                            Column {
                                Text(greeting, color = Color(0xFF1A1C19), fontSize = 20.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    text.registeredParticipant,
                                    color = Color(0xFF5C665F),
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                        if (occupation != null || region.isNotBlank()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (occupation != null) {
                                    StatusChip(occupation, container = Color(0x1A0F3D2E), content = Color(0xFF0F3D2E))
                                }
                                if (region.isNotBlank()) {
                                    StatusChip(region, container = Color(0x29E9872A), content = Color(0xFFC9741F))
                                }
                            }
                        }
                    }
                }
                if (staff) {
                    WhiteCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "ADMIN",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.4.sp,
                                color = Color(0xFF1B5A44),
                            )
                            Text("Registered users", color = Color(0xFF1A1C19), fontSize = 20.sp, fontWeight = FontWeight.Medium)
                            Text(
                                "Open the directory to read registration details.",
                                fontSize = 14.sp,
                                color = Color(0xFF5C665F),
                            )
                            SaffronButton(text = "View users", onClick = onOpenAdmin)
                        }
                    }
                }
                WhiteCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text.account.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.4.sp,
                            color = Color(0xFF1B5A44),
                        )
                        Text(text.yourProfile, color = Color(0xFF1A1C19), fontSize = 20.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text.profileStatus.replace("{status}", statusLabel(text, profile?.status)),
                            fontSize = 14.sp,
                            color = Color(0xFF5C665F),
                        )
                    }
                }
                WhiteCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text.mobile.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.4.sp,
                            color = Color(0xFF1B5A44),
                        )
                        Text(
                            if (mobile.isBlank()) text.mobileOnFile else formatMobile(mobile),
                            color = Color(0xFF1A1C19),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(text.mobileOnFileBody, fontSize = 14.sp, color = Color(0xFF5C665F))
                    }
                }
            }
        }
    }
}
