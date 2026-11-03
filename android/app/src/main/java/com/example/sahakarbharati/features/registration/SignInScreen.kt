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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sahakarbharati.core.copy.AppLanguage
import com.example.sahakarbharati.core.copy.LocalCopy
import com.example.sahakarbharati.core.copy.LocalLanguage
import com.example.sahakarbharati.core.ui.ApplySystemBarIcons
import com.example.sahakarbharati.core.ui.SahakarSeal
import com.example.sahakarbharati.core.ui.SaffronButton
import com.example.sahakarbharati.core.ui.StackTopBar
import com.example.sahakarbharati.core.ui.UiState
import com.example.sahakarbharati.data.sahakarViewModel
import com.example.sahakarbharati.ui.theme.FieldShape
import com.example.sahakarbharati.ui.theme.RathYatraColors

@Composable
fun SignInScreen(
    onBack: () -> Unit,
    onSignedIn: () -> Unit,
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ApplySystemBarIcons(lightIcons = false)
    val copy = LocalCopy.current
    val hindi = AppLanguage.usesHindiCopy(LocalLanguage.current)
    val viewModel: SignInViewModel = sahakarViewModel()
    var mobile by rememberSaveable { mutableStateOf("") }
    var pin by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(viewModel.state) {
        when (val current = viewModel.state) {
            is UiState.Success -> {
                viewModel.consumeSuccess()
                onSignedIn()
            }
            is UiState.Error -> error = current.message
            else -> Unit
        }
    }

    val title = if (hindi) "साइन इन" else "Sign In"
    val loginLabel = if (hindi) "मोबाइल नंबर" else "MOBILE NUMBER"
    val loginHint = if (hindi) {
        "पंजीकरण में दर्ज 10 अंकों का मोबाइल नंबर।"
    } else {
        "Use the 10-digit mobile number from registration."
    }
    val pinLabel = if (hindi) "पिन" else "PIN"
    val accessTitle = if (hindi) "पंजीकृत प्रतिभागी प्रवेश" else "Registered participant access"
    val accessBody = if (hindi) {
        "ऐप खोलने के लिए पंजीकरण और साइन इन आवश्यक है।"
    } else {
        "Registration and sign in are required before anyone can use the app."
    }
    val missing = if (hindi) {
        "10 अंकों का मोबाइल नंबर और पिन लिखें।"
    } else {
        "Enter your 10-digit mobile number and PIN."
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RathYatraColors.ScreenBrush)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .windowInsetsPadding(WindowInsets.ime),
    ) {
        StackTopBar(title = title, onBack = onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SahakarSeal(size = 92.dp)
            Text(
                accessTitle,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = RathYatraColors.Navy,
                textAlign = TextAlign.Center,
            )
            Text(
                accessBody,
                style = MaterialTheme.typography.bodyMedium,
                color = RathYatraColors.OnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            if (error.isNotEmpty()) {
                Text(
                    error,
                    style = MaterialTheme.typography.bodySmall,
                    color = RathYatraColors.Emergency,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RathYatraColors.Emergency.copy(alpha = 0.1f), FieldShape)
                        .padding(12.dp),
                )
            }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    loginLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = RathYatraColors.DeepGreen,
                    fontWeight = FontWeight.Medium,
                )
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it.filter(Char::isDigit).take(10) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = FieldShape,
                    colors = signInFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                Text(loginHint, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
            }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    pinLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = RathYatraColors.DeepGreen,
                    fontWeight = FontWeight.Medium,
                )
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
        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (viewModel.state is UiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(28.dp),
                    color = RathYatraColors.Saffron,
                    strokeWidth = 3.dp,
                )
            } else {
                SaffronButton(
                    text = copy.signIn,
                    onClick = {
                        error = if (mobile.length != 10 || pin.length < 4) {
                            missing
                        } else {
                            viewModel.submit(mobile, pin)
                            ""
                        }
                    },
                    trailingIcon = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = copy.signIn,
                )
            }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onCreateAccount) {
                Text(copy.createAccount, color = RathYatraColors.SaffronPressed, fontWeight = FontWeight.SemiBold)
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
