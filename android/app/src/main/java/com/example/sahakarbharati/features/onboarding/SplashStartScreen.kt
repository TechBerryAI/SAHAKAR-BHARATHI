package com.example.sahakarbharati.features.onboarding

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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sahakarbharati.core.copy.LocalCopy
import com.example.sahakarbharati.core.ui.ApplySystemBarIcons
import com.example.sahakarbharati.core.ui.LanguageChips
import com.example.sahakarbharati.core.ui.SaffronButton
import com.example.sahakarbharati.core.ui.StartSeal
import com.example.sahakarbharati.ui.theme.RathYatraColors

@Composable
fun SplashStartScreen(
    onGetStarted: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ApplySystemBarIcons(lightIcons = false)
    val copy = LocalCopy.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RathYatraColors.ScreenBrush)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x29F0A846), Color.Transparent),
                        center = Offset(size.width / 2f, size.height * 0.18f),
                        radius = size.minDimension * 0.72f,
                    ),
                )
            },
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 22.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LanguageChips(onDarkBackground = false)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            StartSeal(size = 148.dp)
            Spacer(Modifier.height(18.dp))
            Text(
                text = copy.honourLine,
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 0.4.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = RathYatraColors.HonourGold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = copy.title,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 34.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = RathYatraColors.TitleNavy,
                textAlign = TextAlign.Center,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 18.sp, maxFontSize = 34.sp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = copy.subtitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = RathYatraColors.SubtitleBurnt,
                textAlign = TextAlign.Center,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 17.sp),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))
            SaffronButton(
                text = copy.getStarted,
                onClick = onGetStarted,
                trailingIcon = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = "Get Started",
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .clickable(role = Role.Button, onClick = onSignIn)
                    .padding(horizontal = 4.dp, vertical = 6.dp)
                    .semantics { contentDescription = "Sign In" },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "→",
                    color = RathYatraColors.TitleNavy,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Text(
                    text = copy.signInOfficial,
                    color = RathYatraColors.TitleNavy,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
            }
        }
    }
    }
}
