package com.example.sahakarbharati.features.registration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.sahakarbharati.core.copy.AppLanguage
import com.example.sahakarbharati.data.AppSession
import com.example.sahakarbharati.core.copy.LocalCopy
import com.example.sahakarbharati.core.copy.LocalLanguage
import com.example.sahakarbharati.core.ui.ApplySystemBarIcons
import com.example.sahakarbharati.core.ui.SaffronButton
import com.example.sahakarbharati.core.ui.StackTopBar
import com.example.sahakarbharati.core.ui.WhiteCard
import com.example.sahakarbharati.ui.theme.FieldShape
import com.example.sahakarbharati.ui.theme.RathYatraColors

@Composable
fun SignInScreen(
    session: AppSession,
    onBack: () -> Unit,
    onSignedIn: () -> Unit,
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ApplySystemBarIcons(lightIcons = false)
    val copy = LocalCopy.current
    val hindi = LocalLanguage.current == AppLanguage.HI
    var loginId by rememberSaveable { mutableStateOf("") }
    var pin by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf("") }

    val title = if (hindi) "साइन इन" else "Sign In"
    val loginLabel = if (hindi) "लॉगिन आईडी / मोबाइल" else "LOGIN ID / MOBILE"
    val loginHint = if (hindi) {
        "पंजीकरण के बाद मिला लॉगिन आईडी या 10 अंकों का मोबाइल।"
    } else {
        "Use the login ID from registration or your 10-digit mobile number."
    }
    val pinLabel = if (hindi) "पिन" else "PIN"
    val noAccount = if (hindi) {
        "कोई खाता नहीं मिला। पहले खाता बनाएँ।"
    } else {
        "No account found. Create an account first."
    }
    val badCreds = if (hindi) {
        "लॉगिन आईडी या पिन गलत है।"
    } else {
        "Login ID or PIN is incorrect."
    }
    val missing = if (hindi) {
        "लॉगिन आईडी और पिन लिखें।"
    } else {
        "Enter your login ID and PIN."
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RathYatraColors.Ivory)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .windowInsetsPadding(WindowInsets.ime),
    ) {
        StackTopBar(title = title, onBack = onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WhiteCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        if (hindi) "पंजीकृत प्रतिभागी प्रवेश" else "Registered participant access",
                        style = MaterialTheme.typography.titleSmall,
                        color = RathYatraColors.Navy,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (hindi) {
                            "ऐप खोलने के लिए पंजीकरण और साइन इन आवश्यक है।"
                        } else {
                            "Registration and sign in are required before anyone can use the app."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = RathYatraColors.OnSurfaceVariant,
                    )
                }
            }
            WhiteCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(loginLabel, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.OnSurfaceVariant)
                    OutlinedTextField(
                        value = loginId,
                        onValueChange = { loginId = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = FieldShape,
                        colors = signInFieldColors(),
                        singleLine = true,
                    )
                    Text(loginHint, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
                }
            }
            WhiteCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(pinLabel, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.OnSurfaceVariant)
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.filter(Char::isDigit).take(6) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = FieldShape,
                        colors = signInFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                    )
                }
            }
        }
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (error.isNotEmpty()) {
                Text(error, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.Emergency)
                Spacer(Modifier.height(8.dp))
            }
            SaffronButton(
                text = copy.signIn,
                onClick = {
                    error = when {
                        loginId.isBlank() || pin.length < 4 -> missing
                        !session.isRegistered -> noAccount
                        !session.login(loginId, pin) -> badCreds
                        else -> {
                            onSignedIn()
                            ""
                        }
                    }
                },
                trailingIcon = Icons.AutoMirrored.Outlined.Login,
                contentDescription = copy.signIn,
            )
            TextButton(onClick = onCreateAccount) {
                Text(copy.createAccount, color = RathYatraColors.Navy)
            }
        }
    }
}

@Composable
private fun signInFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = RathYatraColors.Saffron,
    unfocusedBorderColor = RathYatraColors.Gold,
    focusedContainerColor = RathYatraColors.White,
    unfocusedContainerColor = RathYatraColors.White,
)
