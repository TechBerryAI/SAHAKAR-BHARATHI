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
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.sahakarbharati.core.ui.SaffronButton
import com.example.sahakarbharati.ui.theme.RathYatraColors

@Composable
fun WelcomeDiscoveryScreen(
    onGetStarted: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ApplySystemBarIcons(lightIcons = false)
    val copy = LocalCopy.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RathYatraColors.Ivory)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 22.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            LanguageChips()
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    RathYatraColors.PrimaryFixed,
                                    RathYatraColors.Ivory,
                                ),
                            ),
                        ),
                )
                Image(
                    painter = painterResource(R.drawable.ic_sahakar_emblem),
                    contentDescription = null,
                    modifier = Modifier.size(78.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = copy.title,
                style = MaterialTheme.typography.headlineMedium,
                color = RathYatraColors.Navy,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = copy.subtitle,
                style = MaterialTheme.typography.titleMedium,
                color = RathYatraColors.BurntSaffron,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = copy.tagline,
                style = MaterialTheme.typography.bodyLarge,
                color = RathYatraColors.OnSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(RathYatraColors.White)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                AboutRow(Icons.Outlined.Navigation, copy.tileTrack, copy.liveRouteCta)
                HorizontalDivider(color = RathYatraColors.SurfaceContainer)
                AboutRow(Icons.Outlined.QrCode2, copy.attendanceTitle, copy.tileQrBody)
                HorizontalDivider(color = RathYatraColors.SurfaceContainer)
                AboutRow(Icons.Outlined.SupportAgent, copy.emergencyTitle, copy.emergencyBody)
            }
            Spacer(Modifier.height(16.dp))
        }

        SaffronButton(
            text = copy.createAccount,
            onClick = onGetStarted,
            trailingIcon = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = "Create Account",
        )
        TextButton(
            onClick = onSignIn,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = copy.alreadyRegistered,
                color = RathYatraColors.Navy,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AboutRow(icon: ImageVector, title: String, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconTile(
            icon = icon,
            container = RathYatraColors.PrimaryFixed,
            tint = RathYatraColors.BurntSaffron,
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = RathYatraColors.Navy,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = RathYatraColors.OnSurfaceVariant,
            )
        }
    }
}
