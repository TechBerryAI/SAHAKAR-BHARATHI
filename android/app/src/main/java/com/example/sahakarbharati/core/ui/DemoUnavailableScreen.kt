package com.example.sahakarbharati.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sahakarbharati.core.copy.LocalCopy
import com.example.sahakarbharati.ui.theme.RathYatraColors

@Composable
fun DemoUnavailableScreen(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showBottomBar: Boolean = false,
    selectedTab: String? = null,
    onSelectTab: (String) -> Unit = {},
    body: String? = null,
) {
    ApplySystemBarIcons(lightIcons = false)
    val copy = LocalCopy.current
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = RathYatraColors.Ivory,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                StackTopBar(title = title, onBack = onBack)
            }
        },
        bottomBar = {
            if (showBottomBar && selectedTab != null) {
                RathBottomBar(selectedKey = selectedTab, onSelect = onSelectTab)
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconTile(
                icon = Icons.Outlined.Info,
                container = RathYatraColors.PrimaryFixed,
                tint = RathYatraColors.BurntSaffron,
            )
            Text(
                text = copy.comingSoon,
                style = MaterialTheme.typography.headlineSmall,
                color = RathYatraColors.Navy,
            )
            Text(
                text = body ?: copy.comingSoonBody,
                style = MaterialTheme.typography.bodyMedium,
                color = RathYatraColors.OnSurfaceVariant,
            )
        }
    }
}
