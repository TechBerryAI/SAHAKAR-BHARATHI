package com.example.sahakarbharati.features.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Agriculture
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Emergency
import androidx.compose.material.icons.outlined.Festival
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.SatelliteAlt
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.sahakarbharati.R
import com.example.sahakarbharati.core.copy.LocalCopy
import com.example.sahakarbharati.core.ui.ApplySystemBarIcons
import com.example.sahakarbharati.core.ui.IconTile
import com.example.sahakarbharati.core.ui.LanguageChips
import com.example.sahakarbharati.core.ui.PulseDot
import com.example.sahakarbharati.core.ui.SacredWheelSeal
import com.example.sahakarbharati.core.ui.SaffronButton
import com.example.sahakarbharati.core.ui.SectionHeader
import com.example.sahakarbharati.core.ui.StatusChip
import com.example.sahakarbharati.core.ui.WhiteActionButton
import com.example.sahakarbharati.core.ui.WhiteCard
import com.example.sahakarbharati.ui.theme.RathYatraColors

@Composable
fun WelcomeDiscoveryScreen(
    onGetStarted: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ApplySystemBarIcons(lightIcons = true)
    val copy = LocalCopy.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RathYatraColors.Ivory)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(rememberScrollState()),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(RathYatraColors.NavyHero),
        ) {
            Column(Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_welcome_hero),
                        contentDescription = copy.heroTitle,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        RathYatraColors.NavyHero.copy(alpha = 0.45f),
                                        RathYatraColors.NavyHero.copy(alpha = 0.82f),
                                        RathYatraColors.NavyHero,
                                    ),
                                ),
                            ),
                    )
                    LanguageChips(
                        dark = false,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                    )
                    Column(
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                    ) {
                        SacredWheelSeal(size = 56.dp)
                        Spacer(Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            StatusChip(
                                text = copy.nationalPortal,
                                container = RathYatraColors.Gold.copy(alpha = 0.2f),
                                content = RathYatraColors.GoldPale,
                            )
                            PulseDot(RathYatraColors.Saffron)
                            Text(
                                copy.nationwide,
                                style = MaterialTheme.typography.labelSmall,
                                color = RathYatraColors.GoldMuted,
                            )
                        }
                        Text(
                            text = copy.platformTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                        )
                        Text(
                            text = copy.platformAgency,
                            style = MaterialTheme.typography.labelSmall,
                            color = RathYatraColors.SecondaryPale,
                        )
                        Text(
                            text = copy.platformBody,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(top = 10.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        WhiteActionButton(
                            text = copy.createAccount,
                            onClick = onGetStarted,
                            trailingIcon = Icons.AutoMirrored.Outlined.ArrowForward,
                            filled = true,
                        )
                        Spacer(Modifier.height(10.dp))
                        WhiteActionButton(
                            text = copy.signIn,
                            onClick = onSignIn,
                            leadingIcon = Icons.AutoMirrored.Outlined.Login,
                        )
                    }
                }
            }
        }

        WhiteCard(Modifier.padding(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier
                            .background(RathYatraColors.SecondaryPale, RoundedCornerShape(12.dp))
                            .padding(10.dp),
                    ) {
                        Icon(Icons.Outlined.Festival, contentDescription = null, tint = RathYatraColors.Navy)
                    }
                    Column {
                        Text(
                            copy.editionLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = RathYatraColors.GoldDark,
                        )
                        Text(
                            copy.festivalTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            color = RathYatraColors.OnSurface,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = RathYatraColors.Saffron,
                                modifier = Modifier.padding(end = 4.dp),
                            )
                            Text(
                                copy.festivalDate,
                                style = MaterialTheme.typography.bodySmall,
                                color = RathYatraColors.OnSurfaceVariant,
                            )
                        }
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(RathYatraColors.SurfaceContainerLow, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PulseDot(RathYatraColors.Saffron)
                        Text(copy.editionRibbon, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.OnSurfaceVariant)
                    }
                    StatusChip(
                        text = copy.editionChip,
                        container = RathYatraColors.White,
                        content = RathYatraColors.BurntSaffron,
                    )
                }
            }
        }

        Column(Modifier.padding(horizontal = 16.dp)) {
            SectionHeader(
                title = copy.servicesTitle,
                trailing = copy.servicesTrail,
                trailingIcon = Icons.Outlined.Verified,
            )
            Spacer(Modifier.height(12.dp))
            ServiceInfoCard(copy.serviceTrack, copy.serviceTrackChip, copy.serviceTrackBody, Icons.Outlined.SatelliteAlt, RathYatraColors.PrimaryFixed, RathYatraColors.BurntSaffron)
            Spacer(Modifier.height(10.dp))
            ServiceInfoCard(copy.serviceAccess, copy.serviceAccessChip, copy.serviceAccessBody, Icons.Outlined.QrCodeScanner, RathYatraColors.SecondaryPale, RathYatraColors.Navy)
            Spacer(Modifier.height(10.dp))
            ServiceInfoCard(copy.serviceUpdates, copy.serviceUpdatesChip, copy.serviceUpdatesBody, Icons.Outlined.Campaign, RathYatraColors.GoldPale, RathYatraColors.GoldDark)
        }

        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = copy.createAccountTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = RathYatraColors.OnSurface,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = copy.createAccountBody,
                style = MaterialTheme.typography.bodyMedium,
                color = RathYatraColors.OnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            )

            WhiteCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .background(RathYatraColors.SecondaryPale, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = RathYatraColors.Navy)
                    }
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                copy.trustTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = RathYatraColors.Navy,
                                fontWeight = FontWeight.Bold,
                            )
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = RathYatraColors.Success,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(start = 4.dp),
                            )
                        }
                        Text(
                            copy.trustBody,
                            style = MaterialTheme.typography.bodySmall,
                            color = RathYatraColors.OnSurfaceVariant,
                        )
                    }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(copy.privileges, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.GoldDark)
                Text(copy.featuresCount, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.OnSurfaceVariant)
            }

            PrivilegeRow(Icons.Outlined.Agriculture, copy.priv1Title, copy.priv1Body)
            PrivilegeRow(Icons.Outlined.Navigation, copy.priv2Title, copy.priv2Body)
            PrivilegeRow(Icons.Outlined.QrCode2, copy.priv3Title, copy.priv3Body)
            PrivilegeRow(Icons.Outlined.AccountBalanceWallet, copy.priv4Title, copy.priv4Body)
            PrivilegeRow(Icons.Outlined.Emergency, copy.priv5Title, copy.priv5Body)

            WhiteCard(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Row(
                    Modifier
                        .background(RathYatraColors.SurfaceContainerLow)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    IconTile(Icons.Outlined.Badge, RathYatraColors.White, RathYatraColors.Navy)
                    Column(Modifier.weight(1f)) {
                        Text(copy.kycTitle, style = MaterialTheme.typography.titleSmall)
                        Text(copy.kycBody, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
                    }
                    StatusChip(copy.kycTime, container = RathYatraColors.PrimaryFixed, content = RathYatraColors.BurntSaffron)
                }
            }

            SaffronButton(
                text = copy.createAccount,
                onClick = onGetStarted,
                trailingIcon = Icons.AutoMirrored.Outlined.ArrowForward,
            )
            TextButton(onClick = onSignIn) {
                Text(copy.alreadyRegistered, color = RathYatraColors.Navy)
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Outlined.Gavel, contentDescription = null, tint = RathYatraColors.OnSurfaceVariant)
            Text(
                copy.footerTitle,
                style = MaterialTheme.typography.labelSmall,
                color = RathYatraColors.OnSurfaceVariant,
            )
            Text(
                copy.footerSub,
                style = MaterialTheme.typography.bodySmall,
                color = RathYatraColors.Outline,
            )
        }
    }
}

@Composable
private fun ServiceInfoCard(
    title: String,
    chip: String,
    body: String,
    icon: ImageVector,
    tile: Color,
    tint: Color,
) {
    WhiteCard {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.background(tile, RoundedCornerShape(12.dp)).padding(10.dp)) {
                Icon(icon, contentDescription = null, tint = tint)
            }
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(title, style = MaterialTheme.typography.titleSmall, color = RathYatraColors.OnSurface)
                    StatusChip(chip, container = tile.copy(alpha = 0.7f), content = tint)
                }
                Text(body, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PrivilegeRow(icon: ImageVector, title: String, body: String) {
    WhiteCard(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(icon, RathYatraColors.SecondaryPale, RathYatraColors.Navy)
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, color = RathYatraColors.OnSurface)
                Text(body, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
            }
        }
    }
}
