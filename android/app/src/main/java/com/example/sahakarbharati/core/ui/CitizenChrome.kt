package com.example.sahakarbharati.core.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Agriculture
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.example.sahakarbharati.core.copy.RathYatraCopy
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.sahakarbharati.core.copy.LocalCopy
import com.example.sahakarbharati.ui.navigation.RathYatraDestinations
import com.example.sahakarbharati.ui.theme.RathYatraColors

@Composable
fun ApplySystemBarIcons(lightIcons: Boolean) {
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !lightIcons
            isAppearanceLightNavigationBars = !lightIcons
        }
    }
}

/**
 * Opaque band behind the status bar for edge-to-edge screens whose content scrolls
 * under it; without it, scrolled text collides with the clock and battery icons.
 */
@Composable
fun StatusBarScrim(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsTopHeight(WindowInsets.statusBars)
            .background(color),
    )
}

/** Localised label for an occupation code stored by the API ("Farmer", "Fisherman", …). */
fun occupationLabel(copy: RathYatraCopy, code: String?): String? = when (code) {
    null, "" -> null
    "Farmer" -> copy.farmer
    "Fisherman" -> copy.fisherman
    "Both" -> copy.both
    "Allied" -> copy.allied
    else -> code
}

@Composable
fun IvoryScreenColumn(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    ApplySystemBarIcons(lightIcons = false)
    Column(
        modifier = modifier
            .background(RathYatraColors.Ivory)
            .windowInsetsPadding(WindowInsets.statusBars),
        content = content,
    )
}

@Composable
fun CitizenTopBar(
    title: String,
    modifier: Modifier = Modifier,
    photoPath: String? = null,
    // Null hides the role switch; the organiser console is not part of the participant app.
    onRoleClick: (() -> Unit)? = null,
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(RathYatraColors.Ivory)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SacredWheelSeal(size = 40.dp)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
        ) {
            Text(
                text = LocalCopy.current.topBarKicker,
                style = MaterialTheme.typography.labelSmall,
                color = RathYatraColors.GoldDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = RathYatraColors.Navy,
                fontWeight = FontWeight.Bold,
            )
        }
        if (onRoleClick != null) {
            IconButton(onClick = onRoleClick, modifier = Modifier.semantics { contentDescription = "Role" }) {
                Icon(Icons.Outlined.SwapHoriz, contentDescription = null, tint = RathYatraColors.Navy)
            }
        }
        // No unread badge: there is no notification service yet, so nothing can be unread.
        IconButton(
            onClick = onNotificationsClick,
            modifier = Modifier.semantics { contentDescription = "Notifications" },
        ) {
            Icon(Icons.Outlined.Notifications, contentDescription = null, tint = RathYatraColors.Navy)
        }
        IconButton(onClick = onProfileClick, modifier = Modifier.semantics { contentDescription = "Profile" }) {
            if (!photoPath.isNullOrBlank()) {
                ProfilePhoto(
                    photoPath = photoPath,
                    initials = "SB",
                    size = 32.dp,
                )
            } else {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(RathYatraColors.Navy),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.AccountCircle,
                        contentDescription = null,
                        tint = RathYatraColors.Ivory,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun StackTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onSosClick: (() -> Unit)? = null,
    onProfileClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.semantics { contentDescription = "Navigate back" },
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = null,
                tint = RathYatraColors.Navy,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = RathYatraColors.Navy,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        trailing?.invoke()
        if (onSosClick != null) {
            Box(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .clip(CircleShape)
                    .background(RathYatraColors.Emergency)
                    .semantics { contentDescription = "Help" },
            ) {
                androidx.compose.material3.TextButton(onClick = onSosClick) {
                    Text("SOS", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        if (onProfileClick != null) {
            IconButton(onClick = onProfileClick, modifier = Modifier.semantics { contentDescription = "Profile" }) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(RathYatraColors.Navy),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.AccountCircle,
                        contentDescription = null,
                        tint = RathYatraColors.Ivory,
                    )
                }
            }
        }
    }
}

data class RathTab(
    val key: String,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun RathBottomBar(
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val copy = LocalCopy.current
    val items = listOf(
        Triple(RathYatraDestinations.HOME, copy.tabHome, Icons.Outlined.Agriculture),
        Triple(RathYatraDestinations.TRACKING, copy.tabYatra, Icons.Outlined.Navigation),
        Triple(RathYatraDestinations.QR_ATTENDANCE, copy.tabAttendance, Icons.Outlined.Verified),
        Triple(RathYatraDestinations.BENEFITS, copy.tabBenefits, Icons.Outlined.VolunteerActivism),
        Triple(RathYatraDestinations.PROFILE, copy.tabProfile, Icons.Outlined.AccountCircle),
    )
    NavigationBar(
        modifier = modifier.fillMaxWidth(),
        containerColor = RathYatraColors.Ivory,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets.navigationBars,
    ) {
        items.forEach { (key, label, icon) ->
            val selected = selectedKey == key ||
                (key == RathYatraDestinations.QR_ATTENDANCE && selectedKey == RathYatraDestinations.QR_PASS)
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(key) },
                icon = {
                    Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp))
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = RathYatraColors.Saffron,
                    selectedTextColor = RathYatraColors.Saffron,
                    unselectedIconColor = RathYatraColors.OnSurfaceVariantCool,
                    unselectedTextColor = RathYatraColors.OnSurfaceVariantCool,
                    indicatorColor = Color.Transparent,
                ),
            )
        }
    }
}

@Composable
fun DemoAlertDialog(
    title: String,
    body: String,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        title = { Text(title) },
        text = { Text(body) },
        containerColor = RathYatraColors.White,
    )
}
