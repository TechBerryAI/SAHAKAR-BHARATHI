package com.example.sahakarbharati.features.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sahakarbharati.core.copy.LocalCopy
import com.example.sahakarbharati.core.copy.LocalLanguage
import com.example.sahakarbharati.core.copy.formatMobile
import com.example.sahakarbharati.core.copy.genderLabel
import com.example.sahakarbharati.core.copy.personInitials
import com.example.sahakarbharati.core.copy.regionLabel
import com.example.sahakarbharati.core.copy.roleLabel
import com.example.sahakarbharati.core.copy.titleCase
import com.example.sahakarbharati.core.copy.ui
import com.example.sahakarbharati.core.copy.uiCount
import com.example.sahakarbharati.core.copy.uiKnown
import com.example.sahakarbharati.core.ui.occupationLabel
import com.example.sahakarbharati.core.ui.InitialAvatar
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
        title = ui("registeredUsers"),
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
                if (viewModel.loading) ui("loading") else {
                    uiCount(LocalLanguage.current, viewModel.total, "onePerson", "manyPeople")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = RathYatraColors.OnSurfaceVariant,
            )
            viewModel.error?.let { message ->
                Text(uiKnown(message), color = RathYatraColors.Emergency, style = MaterialTheme.typography.bodyMedium)
            }
            if (viewModel.loading) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RathYatraColors.Saffron)
                }
            } else if (viewModel.users.isEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(ui("noSearchMatch"), fontWeight = FontWeight.SemiBold)
                    Text(
                        ui("searchTry"),
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
                        Text(if (viewModel.loadingMore) ui("pleaseWait") else ui("loadMore"))
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
            InitialAvatar(personInitials(user.fullName), size = 40.dp, fontSize = 13.sp)
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
                    StatusChip(roleLabel(LocalLanguage.current, user.role))
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
        title = ui("userDetails"),
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
                Text(uiKnown(message), color = RathYatraColors.Emergency, style = MaterialTheme.typography.bodyMedium)
            }
            if (viewModel.loading && person == null) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RathYatraColors.Saffron)
                }
            } else if (profile != null) {
                WhiteCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(titleCase(profile.fullName), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(roleLabel(LocalLanguage.current, profile.role), color = RathYatraColors.OnSurfaceVariant)
                        StatusChip(profile.loginId)
                    }
                }
                val language = LocalLanguage.current
                val copy = LocalCopy.current
                DetailCard(
                    ui("account"),
                    listOf(
                        ui("loginId") to profile.loginId,
                        ui("mobile") to formatMobile(profile.mobile),
                        ui("email") to shown(profile.email),
                        ui("gender") to genderLabel(language, profile.gender),
                        ui("occupation") to (occupationLabel(copy, profile.occupation) ?: shown(profile.occupation)),
                    ),
                )
                DetailCard(
                    ui("address"),
                    listOf(
                        ui("address") to shown(profile.addressLine),
                        ui("locality") to profile.locality?.let(::titleCase).let(::shown),
                        ui("district") to profile.district?.let(::titleCase).let(::shown),
                        ui("state") to profile.state?.let(::titleCase).let(::shown),
                        ui("pincode") to shown(profile.pincode),
                    ),
                )
                if (canGrant) {
                    WhiteCard {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                if (profile.role == ROLE_ADMIN) ui("adminAccessOn") else ui("participantOnly"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                if (profile.role == ROLE_ADMIN) {
                                    ui("adminAccessOnBody")
                                } else {
                                    ui("giveAdminBody")
                                },
                                color = RathYatraColors.OnSurfaceVariant,
                            )
                            if (profile.role == ROLE_ADMIN) {
                                TextButton(
                                    onClick = { viewModel.setRole(userId, ROLE_PARTICIPANT) },
                                    enabled = !viewModel.busy,
                                ) {
                                    Text(if (viewModel.busy) ui("pleaseWait") else ui("removeAdmin"))
                                }
                            } else {
                                SaffronButton(
                                    text = if (viewModel.busy) ui("pleaseWait") else ui("giveAdmin"),
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

private fun shown(value: String?): String = value?.trim()?.ifBlank { null } ?: "—"
