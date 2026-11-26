package com.example.sahakarbharati.features.yatra

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sahakarbharati.core.copy.ui
import com.example.sahakarbharati.core.copy.uiKnown
import com.example.sahakarbharati.core.ui.GhostNavyButton
import com.example.sahakarbharati.core.ui.SaffronButton
import com.example.sahakarbharati.core.ui.SignedInScaffold
import com.example.sahakarbharati.core.ui.WhiteCard
import com.example.sahakarbharati.data.model.isStaffRole
import com.example.sahakarbharati.data.rememberAppSession
import com.example.sahakarbharati.data.remote.YatraSummaryDto
import com.example.sahakarbharati.data.sahakarViewModel
import com.example.sahakarbharati.ui.navigation.RathYatraDestinations
import com.example.sahakarbharati.ui.theme.RathYatraColors

@Composable
fun YatraListScreen(
    onOpenYatra: (String) -> Unit,
    onBack: () -> Unit,
    onSelectTab: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val session = rememberAppSession()
    val viewModel: YatraListViewModel = sahakarViewModel()
    val ui = viewModel.state
    LaunchedEffect(Unit) { viewModel.load() }

    SignedInScaffold(
        title = "Rath Yatras",
        selectedTab = RathYatraDestinations.HOME,
        showAdmin = isStaffRole(session.role()),
        onSelectTab = onSelectTab,
        onBack = onBack,
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
            ui.error?.takeIf { it.isNotBlank() }?.let { message ->
                Text(uiKnown(message), color = RathYatraColors.Emergency, style = MaterialTheme.typography.bodyMedium)
            }
            if (ui.loading) {
                CircularProgressIndicator(color = RathYatraColors.Saffron)
            } else if (ui.items.isEmpty() && ui.error == null) {
                Text("No published Yatras yet.", color = RathYatraColors.OnSurfaceVariant)
            }
            ui.items.forEach { yatra ->
                YatraRow(yatra, onClick = { onOpenYatra(yatra.id) })
            }
            if (ui.hasMore) {
                GhostNavyButton(
                    text = if (ui.loadingMore) ui("pleaseWait") else ui("loadMore"),
                    onClick = { if (!ui.loadingMore) viewModel.loadMore() },
                )
            }
        }
    }
}

@Composable
private fun YatraRow(yatra: YatraSummaryDto, onClick: () -> Unit) {
    WhiteCard(onClick = onClick) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(yatra.name, color = RathYatraColors.Navy, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Text(yatra.district.name, color = RathYatraColors.OnSurfaceVariant, fontSize = 14.sp)
                Text(
                    "${yatraWhen(yatra.startsAt)} to ${yatraWhen(yatra.endsAt)}",
                    color = RathYatraColors.OnSurfaceVariant,
                    fontSize = 13.sp,
                )
                Text(yatraStatusLabel(yatra.status), color = RathYatraColors.DeepGreen, fontSize = 13.sp)
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = RathYatraColors.OnSurfaceVariant)
        }
    }
}

@Composable
fun YatraDetailScreen(
    yatraId: String,
    onBack: () -> Unit,
    onScanCounter: () -> Unit,
    onSendPosition: () -> Unit,
    onSelectTab: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val session = rememberAppSession()
    val viewModel: YatraDetailViewModel = sahakarViewModel()
    val ui = viewModel.state
    LaunchedEffect(yatraId) { viewModel.load(yatraId) }
    val yatra = ui.yatra

    SignedInScaffold(
        title = yatra?.name ?: "Rath Yatra",
        selectedTab = RathYatraDestinations.HOME,
        showAdmin = isStaffRole(session.role()),
        onSelectTab = onSelectTab,
        onBack = onBack,
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
            ui.error?.takeIf { it.isNotBlank() }?.let { message ->
                Text(uiKnown(message), color = RathYatraColors.Emergency, style = MaterialTheme.typography.bodyMedium)
            }
            if (ui.loading && yatra?.id != yatraId) {
                CircularProgressIndicator(color = RathYatraColors.Saffron)
            }
            if (yatra != null && yatra.id == yatraId) {
                WhiteCard {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(yatra.district.name, color = RathYatraColors.DeepGreenSoft, fontSize = 13.sp)
                        Text(yatraStatusLabel(yatra.status), color = RathYatraColors.Navy, fontWeight = FontWeight.Medium)
                        Text(
                            "${yatraWhen(yatra.startsAt)} to ${yatraWhen(yatra.endsAt)}",
                            color = RathYatraColors.OnSurfaceVariant,
                        )
                        yatra.description?.takeIf { it.isNotBlank() }?.let { description ->
                            Text(description, color = RathYatraColors.OnSurface)
                        }
                        SaffronButton(text = "Scan counter", onClick = onScanCounter)
                        GhostNavyButton(text = "Send position", onClick = onSendPosition)
                    }
                }
                Text("Stops", color = RathYatraColors.Navy, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                if (yatra.stops.isEmpty()) {
                    Text("No stops have been published.", color = RathYatraColors.OnSurfaceVariant)
                }
                yatra.stops.forEach { stop ->
                    WhiteCard {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${stop.sequence}. ${stop.name}", color = RathYatraColors.Navy, fontWeight = FontWeight.Medium)
                            stop.address?.takeIf { it.isNotBlank() }?.let { address ->
                                Text(address, color = RathYatraColors.OnSurfaceVariant, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
