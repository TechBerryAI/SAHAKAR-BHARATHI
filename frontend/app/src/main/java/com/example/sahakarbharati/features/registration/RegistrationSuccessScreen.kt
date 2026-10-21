package com.example.sahakarbharati.features.registration

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.VerifiedUser
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.sahakarbharati.core.copy.LocalCopy
import com.example.sahakarbharati.core.ui.ApplySystemBarIcons
import com.example.sahakarbharati.core.ui.OutlinedIvoryButton
import com.example.sahakarbharati.core.ui.PulseDot
import com.example.sahakarbharati.core.ui.SaffronButton
import com.example.sahakarbharati.core.ui.StatusChip
import com.example.sahakarbharati.core.ui.WhiteCard
import com.example.sahakarbharati.ui.theme.RathYatraColors

@Composable
fun RegistrationSuccessScreen(
    onGoHome: () -> Unit,
    onViewPass: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ApplySystemBarIcons(lightIcons = false)
    val copy = LocalCopy.current
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RathYatraColors.Ivory)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.size(112.dp).background(RathYatraColors.Success.copy(alpha = 0.12f), CircleShape))
            Box(
                Modifier
                    .size(72.dp)
                    .background(RathYatraColors.Success, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Check, contentDescription = copy.successTitle, tint = RathYatraColors.White, modifier = Modifier.size(36.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        StatusChip(
            text = copy.govLine,
            container = RathYatraColors.GoldPale.copy(alpha = 0.45f),
            content = RathYatraColors.GoldDark,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            copy.successTitle,
            style = MaterialTheme.typography.headlineLarge,
            color = RathYatraColors.OnSurface,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
        )
        Text(
            copy.successBody,
            style = MaterialTheme.typography.bodyMedium,
            color = RathYatraColors.OnSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
        )

        WhiteCard(Modifier.fillMaxWidth()) {
            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(RathYatraColors.SurfaceContainerLow)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Badge, contentDescription = null, tint = RathYatraColors.Navy)
                        Column {
                            Text(copy.docket, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.OnSurfaceVariant)
                            Text(copy.docketSub, style = MaterialTheme.typography.titleSmall)
                        }
                    }
                    StatusChip(copy.active, container = RathYatraColors.Success.copy(alpha = 0.12f), content = RathYatraColors.Success, leadingDot = RathYatraColors.Success)
                }
                CredentialRow(copy.participantId, copy.participantIdValue, action = {
                    IconButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(copy.participantIdValue))
                            copied = true
                        },
                    ) {
                        Icon(
                            if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                            contentDescription = copy.participantId,
                        )
                    }
                })
                CredentialRow(copy.occupationLabel, copy.occupationValue) {
                    Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = RathYatraColors.Success)
                }
                CredentialRow(copy.regionLabel, copy.regionValue) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = RathYatraColors.Navy)
                }
                CredentialRow(copy.passStatus, copy.passReady) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PulseDot(RathYatraColors.Saffron)
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(RathYatraColors.SurfaceContainerLow)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Outlined.QrCode2, contentDescription = copy.barcodeTitle, modifier = Modifier.size(36.dp))
                    Column {
                        Text(copy.barcodeTitle, style = MaterialTheme.typography.titleSmall)
                        Text(copy.barcodeBody, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SaffronButton(
            text = copy.goHome,
            onClick = onGoHome,
            trailingIcon = Icons.AutoMirrored.Outlined.ArrowForward,
        )
        Spacer(Modifier.height(10.dp))
        OutlinedIvoryButton(
            text = copy.viewPass,
            onClick = onViewPass,
            leadingIcon = Icons.Outlined.Badge,
        )
        Row(
            Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = RathYatraColors.Outline)
            Text(
                copy.successNote,
                style = MaterialTheme.typography.bodySmall,
                color = RathYatraColors.OnSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            copy.successFooter,
            style = MaterialTheme.typography.labelSmall,
            color = RathYatraColors.Outline,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CredentialRow(
    label: String,
    value: String,
    action: @Composable () -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.End)
            action()
        }
    }
}
