package com.example.sahakarbharati.features.registration

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sahakarbharati.core.copy.AppLanguage
import com.example.sahakarbharati.core.copy.LocalCopy
import com.example.sahakarbharati.core.copy.LocalLanguage
import com.example.sahakarbharati.core.ui.ApplySystemBarIcons
import com.example.sahakarbharati.core.ui.SaffronButton
import com.example.sahakarbharati.core.ui.SahakarEmblem
import com.example.sahakarbharati.core.ui.StatusChip
import com.example.sahakarbharati.core.ui.WhiteCard
import com.example.sahakarbharati.core.ui.occupationLabel
import com.example.sahakarbharati.data.rememberAppSession

/**
 * Same stop as the website's /register/success: show the new participant ID, then Home.
 */
@Composable
fun RegistrationSuccessScreen(
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ApplySystemBarIcons(lightIcons = true)
    BackHandler(onBack = onGoHome)
    val copy = LocalCopy.current
    val hindi = AppLanguage.usesHindiCopy(LocalLanguage.current)
    val session = rememberAppSession()
    val loginId = session.loginId.ifBlank { "—" }
    val occupation = occupationLabel(copy, session.occupation)

    Column(
        modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8F3))
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFFE9872A))
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(56.dp)
                .padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                SahakarEmblem(emblemSize = 26.dp)
            }
            Text(
                if (hindi) "पंजीकृत" else "Registered",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
            )
        }
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
                        if (hindi) "खाता तैयार" else "ACCOUNT READY",
                        color = Color(0xFF1B5A44),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.4.sp,
                    )
                    Text(
                        if (hindi) "यह प्रतिभागी आईडी रखें" else "Keep this participant ID",
                        color = Color(0xFF1A1C19),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        if (hindi) {
                            "यह आईडी संभालकर रखें। साइन इन पंजीकरण वाले मोबाइल नंबर और पिन से होगा।"
                        } else {
                            "Keep this ID for your records. Sign in with the mobile number and PIN from registration."
                        },
                        color = Color(0xFF5C665F),
                        fontSize = 14.sp,
                    )
                }
            }
            WhiteCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column {
                        Text(copy.participantId, color = Color(0xFF5C665F), fontSize = 12.sp)
                        Text(loginId, color = Color(0xFF1A1C19), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                    if (occupation != null || session.name.isNotBlank()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (occupation != null) {
                                StatusChip(occupation, container = Color(0x1A0F3D2E), content = Color(0xFF0F3D2E))
                            }
                            if (session.name.isNotBlank()) {
                                StatusChip(session.name, container = Color(0x29E9872A), content = Color(0xFFC9741F))
                            }
                        }
                    }
                }
            }
            SaffronButton(
                text = if (hindi) "होम पर जाएँ" else "Go to Home",
                onClick = onGoHome,
            )
        }
    }
}
