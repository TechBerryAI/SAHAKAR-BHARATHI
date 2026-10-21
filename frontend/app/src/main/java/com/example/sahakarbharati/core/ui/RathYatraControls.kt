package com.example.sahakarbharati.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.sahakarbharati.core.copy.AppLanguage
import com.example.sahakarbharati.core.copy.LocalCopy
import com.example.sahakarbharati.core.copy.LocalLanguage
import com.example.sahakarbharati.core.copy.LocalSetLanguage
import com.example.sahakarbharati.ui.theme.CardShape
import com.example.sahakarbharati.ui.theme.PillShape
import com.example.sahakarbharati.ui.theme.RathYatraColors

@Composable
fun SaffronButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = null,
    contentDescription: String = text,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .semantics { this.contentDescription = contentDescription },
        shape = PillShape,
        color = RathYatraColors.SaffronBright,
        contentColor = RathYatraColors.NavyDeep,
        shadowElevation = 4.dp,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = RathYatraColors.NavyDeep,
            )
            if (trailingIcon != null) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = RathYatraColors.NavyDeep,
                )
            }
        }
    }
}

@Composable
fun GhostNavyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    contentDescription: String = text,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .semantics { this.contentDescription = contentDescription },
        shape = PillShape,
        color = RathYatraColors.NavyDeep.copy(alpha = 0.55f),
        contentColor = RathYatraColors.Ivory,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = RathYatraColors.GoldMuted,
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = RathYatraColors.Ivory,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun WhiteActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    filled: Boolean = false,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (filled) RathYatraColors.Saffron else RathYatraColors.White,
        contentColor = if (filled) RathYatraColors.NavyDeep else RathYatraColors.NavyHero,
        shadowElevation = 2.dp,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text = text, style = MaterialTheme.typography.labelLarge)
            if (trailingIcon != null) {
                Spacer(Modifier.width(8.dp))
                Icon(trailingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun OutlinedIvoryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .border(1.dp, RathYatraColors.SurfaceContainerHighest, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = RathYatraColors.White,
        contentColor = RathYatraColors.Navy,
        shadowElevation = 1.dp,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageChips(
    modifier: Modifier = Modifier,
    dark: Boolean = true,
) {
    val language = LocalLanguage.current
    val setLanguage = LocalSetLanguage.current
    val copy = LocalCopy.current
    var open by remember { mutableStateOf(false) }
    val labelColor = if (dark) RathYatraColors.GoldPale else RathYatraColors.Ivory
    val chipColor = if (dark) {
        RathYatraColors.NavyDeep.copy(alpha = 0.7f)
    } else {
        RathYatraColors.White.copy(alpha = 0.16f)
    }

    Row(
        modifier = modifier
            .clip(PillShape)
            .background(chipColor)
            .clickable { open = true }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .semantics { contentDescription = "Language ${language.englishName}" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(Icons.Outlined.Language, contentDescription = null, tint = labelColor, modifier = Modifier.size(16.dp))
        Text(
            text = language.nativeName,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(Icons.Outlined.ExpandMore, contentDescription = null, tint = labelColor, modifier = Modifier.size(16.dp))
    }

    if (open) {
        ModalBottomSheet(
            onDismissRequest = { open = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = RathYatraColors.Ivory,
        ) {
            LanguagePickerSheet(
                selected = language,
                title = copy.chooseLanguage,
                searchLabel = copy.searchLanguages,
                onSelect = {
                    setLanguage(it)
                    open = false
                },
            )
        }
    }
}

@Composable
private fun LanguagePickerSheet(
    selected: AppLanguage,
    title: String,
    searchLabel: String,
    onSelect: (AppLanguage) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val languages = remember(query) {
        val needle = query.trim()
        AppLanguage.entries.filter { language ->
            needle.isEmpty() ||
                language.nativeName.contains(needle, ignoreCase = true) ||
                language.englishName.contains(needle, ignoreCase = true)
        }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = RathYatraColors.Navy,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(searchLabel) },
            shape = RoundedCornerShape(12.dp),
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp)
                .padding(top = 8.dp, bottom = 12.dp),
        ) {
            items(languages, key = { it.key }) { language ->
                val active = language == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) RathYatraColors.PrimaryFixed else Color.Transparent)
                        .clickable { onSelect(language) }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = language.nativeName,
                            style = MaterialTheme.typography.titleMedium,
                            color = RathYatraColors.OnSurface,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        )
                        Text(
                            text = language.englishName,
                            style = MaterialTheme.typography.bodySmall,
                            color = RathYatraColors.OnSurfaceVariant,
                        )
                    }
                    if (active) {
                        Icon(Icons.Outlined.Check, contentDescription = null, tint = RathYatraColors.BurntSaffron)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusChip(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = RathYatraColors.SurfaceContainerLow,
    content: Color = RathYatraColors.OnSurfaceVariant,
    leadingDot: Color? = null,
) {
    Row(
        modifier = modifier
            .background(container, PillShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (leadingDot != null) {
            Box(
                Modifier
                    .size(7.dp)
                    .background(leadingDot, CircleShape),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    trailingIcon: ImageVector? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = RathYatraColors.OnSurface,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (trailing != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = trailing,
                    style = MaterialTheme.typography.labelSmall,
                    color = RathYatraColors.GoldDark,
                    fontWeight = FontWeight.SemiBold,
                )
                if (trailingIcon != null) {
                    Icon(
                        trailingIcon,
                        contentDescription = null,
                        tint = RathYatraColors.GoldDark,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun WhiteCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = CardShape
    val cardModifier = if (onClick != null) {
        modifier
            .clip(shape)
            .clickable(onClick = onClick)
    } else {
        modifier
    }
    Surface(
        modifier = cardModifier,
        shape = shape,
        color = RathYatraColors.White,
        shadowElevation = 2.dp,
        content = content,
    )
}

@Composable
fun IconTile(
    icon: ImageVector,
    container: Color,
    tint: Color,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .background(container, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
fun DemoDisclaimer(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = RathYatraColors.Outline,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun PulseDot(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(8.dp)
            .background(color, CircleShape),
    )
}

@Composable
fun RowScope.MetricTile(
    icon: ImageVector,
    label: String,
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(icon, contentDescription = null, tint = RathYatraColors.Saffron, modifier = Modifier.size(16.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.OnNavyMuted)
        Text(value, style = MaterialTheme.typography.titleSmall, color = RathYatraColors.White, fontWeight = FontWeight.Bold)
        Text(caption, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.OnNavyMuted)
    }
}
