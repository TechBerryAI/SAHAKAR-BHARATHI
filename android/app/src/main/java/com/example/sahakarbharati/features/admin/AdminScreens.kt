package com.example.sahakarbharati.features.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import com.example.sahakarbharati.core.copy.formatMobile
import com.example.sahakarbharati.core.copy.personInitials
import com.example.sahakarbharati.core.copy.regionLabel
import com.example.sahakarbharati.core.copy.titleCase
import com.example.sahakarbharati.core.ui.SaffronButton
import com.example.sahakarbharati.core.ui.SignedInScaffold
import com.example.sahakarbharati.core.ui.StatusChip
import com.example.sahakarbharati.core.ui.WhiteCard
import com.example.sahakarbharati.data.model.AdminUserSummary
import com.example.sahakarbharati.data.model.ROLE_ADMIN
import com.example.sahakarbharati.data.model.ROLE_PARTICIPANT
import com.example.sahakarbharati.data.model.ROLE_SUPER_ADMIN
import com.example.sahakarbharati.data.model.isStaffRole
import com.example.sahakarbharati.data.rememberAppSession
import com.example.sahakarbharati.data.sahakarViewModel
import com.example.sahakarbharati.ui.navigation.RathYatraDestinations
import com.example.sahakarbharati.ui.theme.RathYatraColors

@Composable
fun AdminUsersScreen(
    onOpenUser: (String) -> Unit,
    onOpenBooths: () -> Unit,
    onSelectTab: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val session = rememberAppSession()
    val viewModel: AdminUsersViewModel = sahakarViewModel()
    SignedInScaffold(
        title = "Registered users",
        selectedTab = RathYatraDestinations.ADMIN,
        showAdmin = isStaffRole(session.role()),
        onSelectTab = onSelectTab,
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
            AdminSectionSwitch(places = false, onPeople = {}, onBooths = onOpenBooths)
            OutlinedTextField(
                value = viewModel.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Search name, mobile, or login ID") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
            )
            Text(
                if (viewModel.loading) "Loading…" else {
                    val noun = if (viewModel.total == 1) "person" else "people"
                    "${viewModel.total} $noun"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = RathYatraColors.OnSurfaceVariant,
            )
            viewModel.error?.let { message ->
                Text(message, color = RathYatraColors.Emergency, style = MaterialTheme.typography.bodyMedium)
            }
            if (viewModel.loading) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RathYatraColors.Saffron)
                }
            } else if (viewModel.users.isEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("No one matches this search.", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Try a login ID, a mobile number, or part of a name.",
                        color = RathYatraColors.OnSurfaceVariant,
                    )
                }
            } else {
                viewModel.users.forEach { user ->
                    AdminUserRow(user, onClick = { onOpenUser(user.id) })
                }
                if (viewModel.users.size < viewModel.total) {
                    TextButton(
                        onClick = viewModel::loadMore,
                        enabled = !viewModel.loadingMore,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (viewModel.loadingMore) "Please wait…" else "Load more")
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminUserRow(user: AdminUserSummary, onClick: () -> Unit) {
    val place = regionLabel(
        user.district?.let(::titleCase),
        user.state?.let(::titleCase),
    )
    WhiteCard(onClick = onClick) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(RathYatraColors.Navy),
                contentAlignment = Alignment.Center,
            ) {
                Text(personInitials(user.fullName), color = RathYatraColors.Ivory, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(titleCase(user.fullName), fontWeight = FontWeight.SemiBold)
                Text(
                    buildString {
                        append(formatMobile(user.mobile))
                        if (place.isNotBlank()) append(" · $place")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = RathYatraColors.OnSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(user.loginId, style = MaterialTheme.typography.labelMedium)
                    StatusChip(roleLabel(user.role))
                }
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = RathYatraColors.OnSurfaceVariant)
        }
    }
}

@Composable
fun AdminUserScreen(
    userId: String,
    onBack: () -> Unit,
    onOpenPeople: () -> Unit,
    onOpenBooths: () -> Unit,
    onSelectTab: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val session = rememberAppSession()
    val viewModel: AdminUserViewModel = sahakarViewModel()
    androidx.compose.runtime.LaunchedEffect(userId) { viewModel.load(userId) }
    val person = viewModel.person
    val profile = person?.profile
    val canGrant = session.role() == ROLE_SUPER_ADMIN &&
        profile != null &&
        profile.role != ROLE_SUPER_ADMIN &&
        profile.loginId != session.loginId

    SignedInScaffold(
        title = "User details",
        selectedTab = RathYatraDestinations.ADMIN_USER,
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
            AdminSectionSwitch(places = false, onPeople = onOpenPeople, onBooths = onOpenBooths)
            viewModel.error?.let { message ->
                Text(message, color = RathYatraColors.Emergency, style = MaterialTheme.typography.bodyMedium)
            }
            if (viewModel.loading && person == null) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RathYatraColors.Saffron)
                }
            } else if (profile != null) {
                WhiteCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(titleCase(profile.fullName), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(roleLabel(profile.role), color = RathYatraColors.OnSurfaceVariant)
                        StatusChip(profile.loginId)
                    }
                }
                DetailCard(
                    "Account",
                    listOf(
                        "Participant ID" to profile.loginId,
                        "Mobile" to formatMobile(profile.mobile),
                        "Email" to shown(profile.email),
                        "Gender" to profile.gender?.let(::titleCase).let(::shown),
                        "Occupation" to profile.occupation?.let(::titleCase).let(::shown),
                    ),
                )
                DetailCard(
                    "Address",
                    listOf(
                        "Address" to shown(profile.addressLine),
                        "Locality" to profile.locality?.let(::titleCase).let(::shown),
                        "District" to profile.district?.let(::titleCase).let(::shown),
                        "State" to profile.state?.let(::titleCase).let(::shown),
                        "PIN code" to shown(profile.pincode),
                    ),
                )
                if (canGrant) {
                    WhiteCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                if (profile.role == ROLE_ADMIN) "Admin access is on" else "Participant only",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                if (profile.role == ROLE_ADMIN) {
                                    "This person can open the directory. They cannot grant access to anyone else."
                                } else {
                                    "Give admin access so this person can open the directory and read registrations."
                                },
                                color = RathYatraColors.OnSurfaceVariant,
                            )
                            if (profile.role == ROLE_ADMIN) {
                                TextButton(
                                    onClick = { viewModel.setRole(userId, ROLE_PARTICIPANT) },
                                    enabled = !viewModel.busy,
                                ) {
                                    Text(if (viewModel.busy) "Please wait…" else "Remove admin access")
                                }
                            } else {
                                SaffronButton(
                                    text = if (viewModel.busy) "Please wait…" else "Give admin access",
                                    onClick = { if (!viewModel.busy) viewModel.setRole(userId, ROLE_ADMIN) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailCard(title: String, rows: List<Pair<String, String>>) {
    WhiteCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            rows.forEach { (label, value) ->
                Column {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.OnSurfaceVariant)
                    Text(value, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

private fun roleLabel(role: String): String = when (role) {
    ROLE_SUPER_ADMIN -> "Super admin"
    ROLE_ADMIN -> "Admin"
    else -> "Participant"
}

private fun shown(value: String?): String = value?.trim()?.ifBlank { null } ?: "—"
