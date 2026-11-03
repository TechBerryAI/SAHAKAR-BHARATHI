package com.example.sahakarbharati.features.registration

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sahakarbharati.R
import com.example.sahakarbharati.core.copy.AppLanguage
import com.example.sahakarbharati.core.copy.LocalCopy
import com.example.sahakarbharati.core.copy.LocalLanguage
import com.example.sahakarbharati.core.ui.ApplySystemBarIcons
import com.example.sahakarbharati.core.ui.SaffronButton
import com.example.sahakarbharati.core.ui.UiState
import com.example.sahakarbharati.data.sahakarViewModel
import com.example.sahakarbharati.core.ui.StackTopBar
import com.example.sahakarbharati.ui.theme.RathYatraColors

@Composable
fun SignInScreen(
    onBack: () -> Unit,
    onSignedIn: () -> Unit,
    onCreateAccount: () -> Unit,
    presetMobile: String = "",
    accountReady: Boolean = false,
    modifier: Modifier = Modifier,
) {
    ApplySystemBarIcons(lightIcons = false)
    val copy = LocalCopy.current
    val hindi = AppLanguage.usesHindiCopy(LocalLanguage.current)
    val viewModel: SignInViewModel = sahakarViewModel()
    var mobile by rememberSaveable { mutableStateOf(presetMobile.filter(Char::isDigit).take(10)) }
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
    val missing = if (hindi) {
        "अपना 10 अंकों का मोबाइल नंबर और पिन लिखें।"
    } else {
        "Enter your 10-digit mobile number and PIN."
    }
    var showPin by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8F3))
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .windowInsetsPadding(WindowInsets.ime),
    ) {
        StackTopBar(title = title, onBack = onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_sahakar_seal),
                contentDescription = null,
                modifier = Modifier
                    .size(88.dp)
                    .align(Alignment.CenterHorizontally),
                contentScale = ContentScale.Fit,
            )
            Text(
                if (hindi) "पंजीकृत प्रतिभागी प्रवेश" else "Registered participant access",
                color = Color(0xFF17263F),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                if (hindi) {
                    "ऐप का उपयोग करने से पहले पंजीकरण और साइन इन आवश्यक है।"
                } else {
                    "Registration and sign in are required before anyone can use the app."
                },
                color = Color(0xFF5C665F),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            if (accountReady && mobile.isNotBlank()) {
                Text(
                    if (hindi) "खाता तैयार — मोबाइल नंबर: $mobile" else "Account ready — Mobile number: $mobile",
                    color = Color(0xFF0F3D2E),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE8F6EE), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                )
            }
            if (error.isNotEmpty()) {
                Text(error, color = RathYatraColors.Emergency, fontSize = 14.sp)
            }
            SignInField(
                label = if (hindi) "मोबाइल नंबर" else "MOBILE NUMBER",
                value = mobile,
                onValue = { mobile = it.filter(Char::isDigit).take(10) },
                hint = if (hindi) "पंजीकरण में दर्ज 10 अंकों का मोबाइल नंबर।" else "Use the 10-digit mobile number from registration.",
                keyboard = KeyboardType.Number,
            )
            SignInField(
                label = if (hindi) "पिन" else "PIN",
                value = pin,
                onValue = { pin = it.filter(Char::isDigit).take(6) },
                keyboard = KeyboardType.NumberPassword,
                password = !showPin,
                onToggleSecret = { showPin = !showPin },
            )
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (viewModel.state is UiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.height(28.dp), color = RathYatraColors.Saffron, strokeWidth = 3.dp)
            } else {
                SaffronButton(
                    text = copy.signIn,
                    onClick = {
                        val mobileOk = mobile.matches(Regex("^[6-9]\\d{9}$"))
                        val pinOk = pin.matches(Regex("^\\d{4,6}$"))
                        error = if (!mobileOk || !pinOk) missing else {
                            viewModel.submit(mobile, pin)
                            ""
                        }
                    },
                    trailingIcon = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = copy.signIn,
                )
            }
        }
    }
}

@Composable
private fun SignInField(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    keyboard: KeyboardType,
    hint: String? = null,
    password: Boolean = false,
    onToggleSecret: (() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label.uppercase(),
            color = Color(0xFF0F3D2E),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.4.sp,
        )
        Box(Modifier.fillMaxWidth()) {
            BasicTextField(
                value = value,
                onValueChange = onValue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .border(1.dp, Color(0xFFE2C48A), RoundedCornerShape(12.dp))
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(start = 14.dp, end = if (onToggleSecret != null) 44.dp else 14.dp),
                textStyle = TextStyle(color = Color(0xFF1A1C19), fontSize = 16.sp),
                singleLine = true,
                visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = keyboard),
                cursorBrush = SolidColor(RathYatraColors.Saffron),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) { inner() }
                },
            )
            if (onToggleSecret != null) {
                Icon(
                    imageVector = if (password) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                    contentDescription = if (password) "Show" else "Hide",
                    tint = Color(0xFF5C665F),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 6.dp)
                        .size(40.dp)
                        .clickable(onClick = onToggleSecret)
                        .padding(9.dp),
                )
            }
        }
        if (!hint.isNullOrBlank()) {
            Text(hint, color = Color(0xFF5C665F), fontSize = 12.sp)
        }
    }
}
