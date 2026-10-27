package com.example.sahakarbharati.features.onboarding

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.Agriculture
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SetMeal
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sahakarbharati.R
import com.example.sahakarbharati.core.copy.LocalCopy
import com.example.sahakarbharati.core.ui.ApplySystemBarIcons
import com.example.sahakarbharati.core.ui.GhostNavyButton
import com.example.sahakarbharati.core.ui.IconTile
import com.example.sahakarbharati.core.ui.LanguageChips
import com.example.sahakarbharati.core.ui.PulseDot
import com.example.sahakarbharati.core.ui.SaffronButton
import com.example.sahakarbharati.ui.theme.RathYatraColors

@Composable
fun SplashStartScreen(
    onGetStarted: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ApplySystemBarIcons(lightIcons = true)
    val copy = LocalCopy.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RathYatraColors.Navy),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            RathYatraColors.SaffronBright.copy(alpha = 0.22f),
                            RathYatraColors.Navy.copy(alpha = 0.10f),
                            RathYatraColors.Navy,
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            RathYatraColors.NavyDeep.copy(alpha = 0.7f),
                            RoundedCornerShape(50),
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PulseDot(RathYatraColors.SaffronBright)
                    Text(
                        text = copy.portalChip,
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.6.sp),
                        color = RathYatraColors.GoldPale,
                    )
                }
                LanguageChips()
            }

            Spacer(Modifier.height(36.dp))
            Image(
                painter = painterResource(R.drawable.ic_sahakar_emblem),
                contentDescription = null,
                modifier = Modifier.size(156.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.height(28.dp))

            Text(
                text = copy.honourLine,
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 1.4.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = RathYatraColors.GoldPale,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = copy.title,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 34.sp,
                    lineHeight = 42.sp,
                    letterSpacing = 0.4.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = RathYatraColors.Ivory,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = copy.subtitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    lineHeight = 24.sp,
                    letterSpacing = 0.2.sp,
                ),
                color = RathYatraColors.GoldMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = copy.tagline,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    letterSpacing = 0.15.sp,
                ),
                color = RathYatraColors.SurfaceContainerHigh.copy(alpha = 0.92f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            Spacer(Modifier.height(24.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SplashStatCard(copy.statFarmers, copy.statFarmersSub, Icons.Outlined.Agriculture, Modifier.weight(1f))
                    SplashStatCard(copy.statFishers, copy.statFishersSub, Icons.Outlined.SetMeal, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SplashStatCard(copy.statHelp, copy.statHelpSub, Icons.Outlined.SupportAgent, Modifier.weight(1f))
                    SplashStatCard(copy.statPass, copy.statPassSub, Icons.Outlined.Badge, Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(28.dp))
            SaffronButton(
                text = copy.getStarted,
                onClick = onGetStarted,
                trailingIcon = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = "Get Started",
            )
            Spacer(Modifier.height(12.dp))
            GhostNavyButton(
                text = copy.signInOfficial,
                onClick = onSignIn,
                leadingIcon = Icons.AutoMirrored.Outlined.Login,
                contentDescription = "Sign In",
            )

            Spacer(Modifier.height(28.dp))
            Icon(
                Icons.Outlined.Lock,
                contentDescription = null,
                tint = RathYatraColors.GoldPale.copy(alpha = 0.8f),
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = copy.digitalIndia,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp, letterSpacing = 0.15.sp),
                color = RathYatraColors.GoldPale.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = copy.govLine,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp, letterSpacing = 0.15.sp),
                color = RathYatraColors.SurfaceContainerHigh.copy(alpha = 0.78f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = copy.ministryLine,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp, letterSpacing = 0.1.sp),
                color = RathYatraColors.SurfaceContainerHigh.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SplashStatCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier
            .background(RathYatraColors.NavyDeep.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        // On narrow tiles (small phones, large font sizes) the icon goes above the text,
        // giving the title the full tile width so words wrap whole instead of being cut.
        val stacked = maxWidth / LocalDensity.current.fontScale < 140.dp
        val iconTile: @Composable () -> Unit = {
            IconTile(
                icon = icon,
                container = RathYatraColors.Ivory.copy(alpha = 0.08f),
                tint = RathYatraColors.SaffronBright,
            )
        }
        val texts: @Composable () -> Unit = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.2.sp),
                    color = RathYatraColors.Ivory,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                    color = RathYatraColors.SurfaceContainerHigh.copy(alpha = 0.75f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                iconTile()
                texts()
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                iconTile()
                texts()
            }
        }
    }
}
