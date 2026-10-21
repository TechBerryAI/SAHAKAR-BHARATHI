package com.example.sahakarbharati.features.registration

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.example.sahakarbharati.R
import androidx.compose.runtime.Composable
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import com.example.sahakarbharati.core.copy.AppLanguage
import com.example.sahakarbharati.core.copy.LocalCopy
import com.example.sahakarbharati.core.copy.LocalLanguage
import com.example.sahakarbharati.core.ui.ApplySystemBarIcons
import com.example.sahakarbharati.core.ui.SaffronButton
import com.example.sahakarbharati.core.ui.UiState
import com.example.sahakarbharati.data.sahakarViewModel
import com.example.sahakarbharati.core.ui.StackTopBar
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
    val hindi = LocalLanguage.current == AppLanguage.HI
    val viewModel: SignInViewModel = sahakarViewModel()
    var loginId by rememberSaveable { mutableStateOf("") }
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
    val loginLabel = if (hindi) "लॉगिन आईडी / मोबाइल" else "Login ID / mobile"
    val loginHint = if (hindi) {
        "पंजीकरण के बाद मिला लॉगिन आईडी या 10 अंकों का मोबाइल।"
    } else {
        "Use the login ID from registration or your 10-digit mobile number."
    }
    val pinLabel = if (hindi) "पिन" else "PIN"
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
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(RathYatraColors.PrimaryFixed),
                )
                Image(
                    painter = painterResource(R.drawable.ic_sahakar_emblem),
                    contentDescription = null,
                    modifier = Modifier.size(68.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (hindi) "पंजीकृत प्रतिभागी प्रवेश" else "Registered participant access",
                style = MaterialTheme.typography.titleMedium,
                color = RathYatraColors.Navy,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (hindi) {
                    "ऐप खोलने के लिए पंजीकरण और साइन इन आवश्यक है।"
                } else {
                    "Registration and sign in are required before anyone can use the app."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = RathYatraColors.OnSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(22.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(RathYatraColors.White)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SignInField(
                    label = loginLabel,
                    value = loginId,
                    onValueChange = { loginId = it },
                    hint = loginHint,
                )
                SignInField(
                    label = pinLabel,
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(6) },
                    keyboardType = KeyboardType.NumberPassword,
                    hidden = true,
                )
            }
        }
        Column(
            Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (error.isNotEmpty()) {
                Text(error, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.Emergency)
                Spacer(Modifier.height(8.dp))
            }
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
                        error = if (loginId.isBlank() || pin.length < 4) {
                            missing
                        } else {
                            viewModel.submit(loginId, pin)
                            ""
                        }
                    },
                    trailingIcon = Icons.AutoMirrored.Outlined.Login,
                    contentDescription = copy.signIn,
                )
            }
            TextButton(onClick = onCreateAccount) {
                Text(
                    copy.createAccount,
                    color = RathYatraColors.Navy,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun SignInField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    hidden: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = RathYatraColors.Navy,
            fontWeight = FontWeight.SemiBold,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = FieldShape,
            colors = signInFieldColors(),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (hidden) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            singleLine = true,
        )
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = RathYatraColors.OnSurfaceVariant,
            )
        }
    }
}

@Composable
private fun signInFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = RathYatraColors.Saffron,
    unfocusedBorderColor = RathYatraColors.SurfaceDim,
    focusedContainerColor = RathYatraColors.White,
    unfocusedContainerColor = RathYatraColors.White,
)
