package com.example.sahakarbharati.features.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sahakarbharati.R
import com.example.sahakarbharati.core.copy.AppLanguage
import com.example.sahakarbharati.core.copy.LocalCopy
import com.example.sahakarbharati.core.copy.LocalLanguage
import com.example.sahakarbharati.core.ui.ApplySystemBarIcons
import com.example.sahakarbharati.core.ui.LanguageChips
import com.example.sahakarbharati.core.ui.SaffronButton

private val WelcomeNavy = Color(0xFF163A78)
private val WelcomeSaffron = Color(0xFFB85A0B)

@Composable
fun SplashStartScreen(
    onGetStarted: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ApplySystemBarIcons(lightIcons = false)
    val copy = LocalCopy.current
    val hindi = AppLanguage.usesHindiCopy(LocalLanguage.current)
    val honour = if (hindi) "भारत के अन्न नायकों का सम्मान" else "Honouring India's food heroes"
    val welcome = if (hindi) "सहकार भारती में आपका स्वागत है" else "Welcome to Sahakar Bharati"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFF08A24).copy(alpha = 0.12f),
                            Color.White.copy(alpha = 0f),
                        ),
                    ),
                ),
        )
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
            Spacer(Modifier.weight(1f, fill = true))
            Image(
                painter = painterResource(R.drawable.ic_sahakar_seal),
                contentDescription = null,
                modifier = Modifier.size(148.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.height(28.dp))
            Text(
                text = honour,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp,
                color = WelcomeSaffron,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            FitOneLine(
                text = welcome,
                color = WelcomeNavy,
                fontWeight = FontWeight.Bold,
                maxSize = 34.sp,
                minSize = 18.sp,
            )
            Spacer(Modifier.height(8.dp))
            FitOneLine(
                text = copy.subtitle,
                color = WelcomeSaffron,
                fontWeight = FontWeight.SemiBold,
                maxSize = 17.sp,
                minSize = 12.sp,
            )
            Spacer(Modifier.weight(1f, fill = true))
            SaffronButton(
                text = copy.getStarted,
                onClick = onGetStarted,
                trailingIcon = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = "Register Now",
            )
            Row(
                modifier = Modifier
                    .clickable(onClick = onSignIn)
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = WelcomeNavy,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = copy.signInOfficial,
                    color = WelcomeNavy,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun FitOneLine(
    text: String,
    color: Color,
    fontWeight: FontWeight,
    maxSize: TextUnit,
    minSize: TextUnit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val measurer = rememberTextMeasurer()
        val width = constraints.maxWidth
        var size = maxSize.value
        while (size > minSize.value) {
            val measured = measurer.measure(
                text = AnnotatedString(text),
                style = TextStyle(fontSize = size.sp, fontWeight = fontWeight),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
            if (measured.size.width <= width - 2) break
            size -= 0.5f
        }
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            color = color,
            fontSize = size.sp,
            fontWeight = fontWeight,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
    }
}
