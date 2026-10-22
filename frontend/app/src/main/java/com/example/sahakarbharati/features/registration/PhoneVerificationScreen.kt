package com.example.sahakarbharati.features.registration

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.sahakarbharati.core.copy.AppLanguage
import com.example.sahakarbharati.core.copy.LocalLanguage
import com.example.sahakarbharati.core.ui.ApplySystemBarIcons
import com.example.sahakarbharati.core.ui.SaffronButton
import com.example.sahakarbharati.core.ui.StackTopBar
import com.example.sahakarbharati.core.ui.WhiteCard
import com.example.sahakarbharati.ui.theme.FieldShape
import com.example.sahakarbharati.ui.theme.RathYatraColors
import kotlin.random.Random

@Composable
fun PhoneVerificationScreen(
    onBack: () -> Unit,
    onVerified: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ApplySystemBarIcons(lightIcons = false)
    val context = LocalContext.current
    val copy = phoneOtpCopy(LocalLanguage.current)
    var mobile by rememberSaveable { mutableStateOf("") }
    var otpInput by rememberSaveable { mutableStateOf("") }
    var issuedOtp by rememberSaveable { mutableStateOf("") }
    var issuedFor by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf("") }
    val otpSent = issuedOtp.length == 6 && issuedFor == mobile

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted && issuedOtp.length == 6) {
            showOtpNotification(context, issuedOtp)
        }
    }

    fun sendOtp() {
        if (mobile.length != 10) {
            error = copy.mobileRequired
            return
        }
        val code = Random.nextInt(100000, 1000000).toString()
        issuedOtp = code
        issuedFor = mobile
        otpInput = ""
        error = ""
        if (canPostNotifications(context)) {
            showOtpNotification(context, code)
        } else if (Build.VERSION.SDK_INT >= 33) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun verify() {
        when {
            !otpSent -> error = copy.sendFirst
            otpInput.length != 6 -> error = copy.otpRequired
            otpInput != issuedOtp -> error = copy.otpWrong
            else -> onVerified(mobile)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RathYatraColors.Ivory)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .windowInsetsPadding(WindowInsets.ime),
    ) {
        StackTopBar(title = copy.title, onBack = onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WhiteCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(copy.cardTitle, style = MaterialTheme.typography.titleSmall, color = RathYatraColors.Navy)
                    Text(copy.body, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
                }
            }
            WhiteCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(copy.mobileLabel, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.OnSurfaceVariant)
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = {
                            mobile = it.filter(Char::isDigit).take(10)
                            if (error == copy.mobileRequired) error = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = FieldShape,
                        colors = otpFieldColors(isError = error == copy.mobileRequired),
                        isError = error == copy.mobileRequired,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        prefix = { Text("+91 ", color = RathYatraColors.OnSurfaceVariant) },
                        singleLine = true,
                    )
                    Text(copy.mobileHint, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.OnSurfaceVariant)
                }
            }
            if (otpSent) {
                WhiteCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(copy.sentTitle, style = MaterialTheme.typography.titleSmall, color = RathYatraColors.Navy)
                        Text(
                            copy.sentBody(mobile),
                            style = MaterialTheme.typography.bodySmall,
                            color = RathYatraColors.OnSurfaceVariant,
                        )
                        Text(
                            issuedOtp,
                            modifier = Modifier.semantics { contentDescription = "One time password" },
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 6.sp,
                            ),
                            color = RathYatraColors.Navy,
                        )
                        Text(copy.otpLabel, style = MaterialTheme.typography.labelSmall, color = RathYatraColors.OnSurfaceVariant)
                        OutlinedTextField(
                            value = otpInput,
                            onValueChange = {
                                otpInput = it.filter(Char::isDigit).take(6)
                                if (error == copy.otpWrong || error == copy.otpRequired) error = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = FieldShape,
                            colors = otpFieldColors(isError = error == copy.otpWrong || error == copy.otpRequired),
                            isError = error == copy.otpWrong || error == copy.otpRequired,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                        )
                    }
                }
            }
        }
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (error.isNotEmpty()) {
                Text(error, style = MaterialTheme.typography.bodySmall, color = RathYatraColors.Emergency)
            }
            if (!otpSent) {
                SaffronButton(
                    text = copy.sendOtp,
                    onClick = { sendOtp() },
                    trailingIcon = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = copy.sendOtp,
                )
            } else {
                SaffronButton(
                    text = copy.verify,
                    onClick = { verify() },
                    trailingIcon = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = copy.verify,
                )
                TextButton(onClick = { sendOtp() }) {
                    Text(copy.resend, color = RathYatraColors.Navy, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun canPostNotifications(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < 33) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
}

private fun showOtpNotification(context: Context, otp: String) {
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    val channelId = "sahakar_otp"
    manager.createNotificationChannel(
        NotificationChannel(channelId, "OTP", NotificationManager.IMPORTANCE_HIGH),
    )
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.stat_notify_chat)
        .setContentTitle("Sahakar Bharati")
        .setContentText("Your OTP is $otp")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()
    runCatching { manager.notify(4101, notification) }
}

@Composable
private fun otpFieldColors(isError: Boolean) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = if (isError) RathYatraColors.Emergency else RathYatraColors.Saffron,
    unfocusedBorderColor = if (isError) RathYatraColors.Emergency else RathYatraColors.Gold,
    errorBorderColor = RathYatraColors.Emergency,
    focusedContainerColor = RathYatraColors.White,
    unfocusedContainerColor = RathYatraColors.White,
    errorContainerColor = RathYatraColors.White,
    focusedTextColor = RathYatraColors.Navy,
    unfocusedTextColor = RathYatraColors.Navy,
    cursorColor = RathYatraColors.Saffron,
    errorCursorColor = RathYatraColors.Emergency,
)

private data class PhoneOtpCopy(
    val title: String,
    val cardTitle: String,
    val body: String,
    val mobileLabel: String,
    val mobileHint: String,
    val mobileRequired: String,
    val sendOtp: String,
    val sentTitle: String,
    val sentBody: (String) -> String,
    val otpLabel: String,
    val otpRequired: String,
    val otpWrong: String,
    val sendFirst: String,
    val verify: String,
    val resend: String,
)

private fun phoneOtpCopy(language: AppLanguage): PhoneOtpCopy =
    if (language == AppLanguage.HI) PhoneOtpCopy(
        title = "मोबाइल सत्यापन",
        cardTitle = "मोबाइल नंबर",
        body = "पंजीकरण से पहले मोबाइल नंबर लिखें। OTP भेजें, उसे जाँचें, फिर व्यक्तिगत विवरण खुलेंगे।",
        mobileLabel = "मोबाइल नंबर",
        mobileHint = "10 अंकों का भारतीय मोबाइल नंबर।",
        mobileRequired = "10 अंकों का मोबाइल नंबर लिखें।",
        sendOtp = "OTP भेजें",
        sentTitle = "OTP भेज दिया गया",
        sentBody = { mobile -> "OTP +91 $mobile पर भेजा गया। नीचे दिया कोड लिखें और सत्यापित करें।" },
        otpLabel = "OTP",
        otpRequired = "6 अंकों का OTP लिखें।",
        otpWrong = "OTP गलत है। कोड जाँचकर फिर कोशिश करें।",
        sendFirst = "पहले OTP भेजें।",
        verify = "सत्यापित करें",
        resend = "OTP फिर भेजें",
    ) else PhoneOtpCopy(
        title = "Verify mobile",
        cardTitle = "Mobile number",
        body = "Enter your mobile number before registration. Send the OTP, verify it, and the personal details page will open.",
        mobileLabel = "MOBILE NUMBER",
        mobileHint = "10-digit Indian mobile number.",
        mobileRequired = "Enter a 10-digit mobile number.",
        sendOtp = "Send OTP",
        sentTitle = "OTP sent",
        sentBody = { mobile -> "OTP sent to +91 $mobile. Enter the code below and tap Verify." },
        otpLabel = "OTP",
        otpRequired = "Enter the 6-digit OTP.",
        otpWrong = "OTP is incorrect. Check the code and try again.",
        sendFirst = "Send the OTP first.",
        verify = "Verify",
        resend = "Resend OTP",
    )
