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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
            .background(RathYatraColors.NavyDeep),
    ) {
        Box(
            Modifier
                .align(Alignment.Center)
                .size(460.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            RathYatraColors.SaffronBright.copy(alpha = 0.34f),
                            RathYatraColors.Gold.copy(alpha = 0.10f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 22.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(end = 12.dp)
                        .background(
                            RathYatraColors.Navy.copy(alpha = 0.72f),
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                LanguageChips()
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            Modifier
                                .size(196.dp)
                                .clip(CircleShape)
                                .background(RathYatraColors.SaffronBright.copy(alpha = 0.16f)),
                        )
                        Image(
                            painter = painterResource(R.drawable.ic_sahakar_emblem),
                            contentDescription = null,
                            modifier = Modifier.size(156.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                    Spacer(Modifier.height(28.dp))
                    Text(
                        text = copy.honourLine,
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 1.6.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = RathYatraColors.GoldPale,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier
                            .width(56.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        RathYatraColors.GoldPale,
                                        Color.Transparent,
                                    ),
                                ),
                            ),
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = copy.title,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 36.sp,
                            lineHeight = 42.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = RathYatraColors.Ivory,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = copy.subtitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 24.sp),
                        color = RathYatraColors.GoldMuted,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = copy.tagline,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
                        color = RathYatraColors.Ivory.copy(alpha = 0.82f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
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
            Spacer(Modifier.height(8.dp))
        }
    }
}
