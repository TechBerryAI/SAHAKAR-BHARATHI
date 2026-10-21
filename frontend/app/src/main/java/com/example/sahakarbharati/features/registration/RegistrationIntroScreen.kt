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
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Emergency
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Agriculture
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.sahakarbharati.core.copy.LocalCopy
import com.example.sahakarbharati.core.ui.ApplySystemBarIcons
import com.example.sahakarbharati.core.ui.IconTile
import com.example.sahakarbharati.core.ui.SacredWheel
import com.example.sahakarbharati.core.ui.SaffronButton
import com.example.sahakarbharati.core.ui.StackTopBar
import com.example.sahakarbharati.core.ui.StatusChip
import com.example.sahakarbharati.core.ui.WhiteCard
import com.example.sahakarbharati.ui.theme.RathYatraColors

@Composable
fun RegistrationIntroScreen(
    onBack: () -> Unit,
    onCreateAccount: () -> Unit,
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
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                StackTopBar(title = "", onBack = onBack)
            }
            StatusChip(
                text = copy.officialPortal,
                container = RathYatraColors.SurfaceContainer,
                content = RathYatraColors.Navy,
                leadingDot = null,
            )
            Spacer(Modifier.weight(1f))
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SacredWheel(
                size = 72.dp,
                tint = RathYatraColors.Gold,
                hubColor = RathYatraColors.Ivory,
                spokeColor = RathYatraColors.BurntSaffron,
                showTemple = false,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = copy.createAccountTitle,
                style = MaterialTheme.typography.displayMedium,
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
                                modifier = Modifier.size(16.dp).padding(start = 4.dp),
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
        }

        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            SaffronButton(
                text = copy.createAccount,
                onClick = onCreateAccount,
                trailingIcon = Icons.AutoMirrored.Outlined.ArrowForward,
            )
            TextButton(onClick = onSignIn) {
                Text(copy.alreadyRegistered, color = RathYatraColors.Navy)
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
