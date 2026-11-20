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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sahakarbharati.core.copy.ui
import com.example.sahakarbharati.core.copy.uiKnown
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
    val accountEpoch by session.epoch
    val viewModel: YatraListViewModel = sahakarViewModel()
    val listState = viewModel.state
    LaunchedEffect(accountEpoch) { viewModel.load() }

    SignedInScaffold(
        title = ui("yatrasTitle"),
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
            if (listState.loading && listState.items.isEmpty()) {
                CircularProgressIndicator(color = RathYatraColors.Saffron)
            }
            listState.error?.takeIf { it.isNotBlank() && listState.items.isEmpty() }?.let { message ->
                Text(uiKnown(message), color = RathYatraColors.Emergency, style = MaterialTheme.typography.bodyMedium)
                SaffronButton(text = ui("retry"), onClick = viewModel::load)
            }
            if (!listState.loading && listState.items.isEmpty() && listState.error == null) {
                Text(ui("noPublishedYatras"), color = RathYatraColors.OnSurfaceVariant)
            }
            listState.items.forEach { yatra ->
                YatraRow(yatra, onClick = { onOpenYatra(yatra.id) })
            }
            listState.nextPageError?.takeIf { it.isNotBlank() }?.let { message ->
                Text(uiKnown(message), color = RathYatraColors.Emergency, style = MaterialTheme.typography.bodyMedium)
                SaffronButton(text = ui("retry"), onClick = viewModel::loadMore)
            }
            if (listState.hasMore && listState.nextPageError == null) {
                val moreLabel = if (listState.loadingMore) ui("pleaseWait") else ui("yatrasLoadMore")
                SaffronButton(text = moreLabel, onClick = viewModel::loadMore)
            }
        }
    }
}

@Composable
private fun YatraRow(yatra: YatraSummaryDto, onClick: () -> Unit) {
    val whenLabel = yatraDateRange(yatra.startsAt, yatra.endsAt)
    val status = yatraStatusLabel(yatra.status)
    WhiteCard(onClick = onClick, semanticsLabel = yatraRowLabel(yatra)) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(yatra.name, color = RathYatraColors.Navy, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Text(yatra.district.name, color = RathYatraColors.OnSurfaceVariant, fontSize = 14.sp)
                Text(whenLabel, color = RathYatraColors.OnSurfaceVariant, fontSize = 13.sp)
                Text(status, color = RathYatraColors.DeepGreen, fontSize = 13.sp)
            }
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = RathYatraColors.OnSurfaceVariant,
            )
        }
    }
}

@Composable
fun YatraDetailScreen(
    yatraId: String,
    onBack: () -> Unit,
    onSelectTab: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val session = rememberAppSession()
    val accountEpoch by session.epoch
    val viewModel: YatraDetailViewModel = sahakarViewModel()
    val detailState = viewModel.state
    LaunchedEffect(accountEpoch, yatraId) { viewModel.load(yatraId) }
    val yatra = detailState.yatra?.takeIf { it.id == yatraId }

    SignedInScaffold(
        title = yatra?.name ?: ui("yatrasTitle"),
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
            if (detailState.loading && yatra == null) {
                CircularProgressIndicator(color = RathYatraColors.Saffron)
            }
            detailState.error?.takeIf { it.isNotBlank() && yatra == null }?.let { message ->
                Text(uiKnown(message), color = RathYatraColors.Emergency, style = MaterialTheme.typography.bodyMedium)
                SaffronButton(text = ui("retry"), onClick = { viewModel.load(yatraId) })
            }
            if (yatra != null) {
                WhiteCard {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(yatra.district.name, color = RathYatraColors.DeepGreenSoft, fontSize = 13.sp)
                        Text(ui("yatraCode"), color = RathYatraColors.OnSurfaceVariant, fontSize = 13.sp)
                        Text(yatra.publicCode, color = RathYatraColors.Navy, fontWeight = FontWeight.Medium)
                        Text(yatraStatusLabel(yatra.status), color = RathYatraColors.Navy, fontWeight = FontWeight.Medium)
                        Text(
                            yatraDateRange(yatra.startsAt, yatra.endsAt),
                            color = RathYatraColors.OnSurfaceVariant,
                        )
                        yatra.description?.takeIf { it.isNotBlank() }?.let { description ->
                            Text(description, color = RathYatraColors.OnSurface)
                        }
                    }
                }
                Text(ui("stopsHeading"), color = RathYatraColors.Navy, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                if (yatra.stops.isEmpty()) {
                    Text(ui("noPublishedStops"), color = RathYatraColors.OnSurfaceVariant)
                }
                yatra.stops.forEach { stop ->
                    WhiteCard {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${stop.sequence}. ${stop.name}", color = RathYatraColors.Navy, fontWeight = FontWeight.Medium)
                            stop.address?.takeIf { it.isNotBlank() }?.let { address ->
                                Text(address, color = RathYatraColors.OnSurfaceVariant, fontSize = 14.sp)
                            }
                            stopTimeLabel(stop)?.let { times ->
                                Text(times, color = RathYatraColors.OnSurfaceVariant, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
