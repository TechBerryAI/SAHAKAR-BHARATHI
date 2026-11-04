package com.example.sahakarbharati.core.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.sahakarbharati.core.copy.LocalCopy
import com.example.sahakarbharati.ui.navigation.RathYatraDestinations
import com.example.sahakarbharati.ui.theme.RathYatraColors

private val WebSaffron = Color(0xFFE9872A)
private val WebSoftWhite = Color(0xFFFFF8F3)

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
        SahakarEmblem(emblemSize = 40.dp)
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
    contentColor: Color = RathYatraColors.Navy,
    titleSize: TextUnit = TextUnit.Unspecified,
    titleWeight: FontWeight = FontWeight.Bold,
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
                tint = contentColor,
            )
        }
        Text(
            text = title,
            style = if (titleSize == TextUnit.Unspecified) {
                MaterialTheme.typography.headlineSmall
            } else {
                MaterialTheme.typography.titleLarge.copy(fontSize = titleSize, letterSpacing = 0.15.sp)
            },
            color = contentColor,
            fontWeight = titleWeight,
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
    showAdmin: Boolean = false,
) {
    val copy = LocalCopy.current
    val muted = Color(0xFF5C665F)
    val items = buildList {
        add(Triple(RathYatraDestinations.HOME, copy.tabHome, Icons.Filled.Home))
        add(Triple(RathYatraDestinations.PROFILE, copy.tabProfile, Icons.Filled.Person))
        if (showAdmin) {
            add(Triple(RathYatraDestinations.ADMIN, "Admin", Icons.Filled.Groups))
        }
    }
    Column(
        modifier
            .fillMaxWidth()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFD4CFC6)),
        )
        Row(Modifier.fillMaxWidth().height(64.dp)) {
            items.forEach { (key, label, icon) ->
                val selected = selectedKey == key ||
                    (key == RathYatraDestinations.ADMIN && selectedKey in setOf(
                        RathYatraDestinations.ADMIN_USER,
                        RathYatraDestinations.ADMIN_PLACES,
                        RathYatraDestinations.ADMIN_PLACE,
                    ))
                val tint = if (selected) RathYatraColors.Saffron else muted
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelect(key) }
                        .semantics { contentDescription = label },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = label,
                        color = tint,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun SignedInScaffold(
    title: String,
    selectedTab: String,
    showAdmin: Boolean,
    onSelectTab: (String) -> Unit,
    modifier: Modifier = Modifier,
    showLanguage: Boolean = false,
    onBack: (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    ApplySystemBarIcons(lightIcons = true)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = WebSoftWhite,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(
                Modifier
                    .background(WebSaffron)
                    .windowInsetsPadding(WindowInsets.statusBars),
            ) {
                if (onBack != null) {
                    StackTopBar(
                        title = title,
                        onBack = onBack,
                        contentColor = Color.White,
                        titleSize = 18.sp,
                        titleWeight = FontWeight.Medium,
                    )
                } else {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(start = 4.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                            SahakarEmblem(emblemSize = 26.dp)
                        }
                        Text(
                            text = title,
                            modifier = Modifier.weight(1f),
                            fontSize = 18.sp,
                            letterSpacing = 0.15.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (showLanguage) {
                            LanguageChips(onDarkBackground = true)
                        }
                    }
                }
            }
        },
        bottomBar = {
            RathBottomBar(
                selectedKey = selectedTab,
                onSelect = onSelectTab,
                showAdmin = showAdmin,
            )
        },
        content = content,
    )
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
