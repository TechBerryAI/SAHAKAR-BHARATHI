package com.example.sahakarbharati

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import com.example.sahakarbharati.ui.navigation.RathYatraScreen
import com.example.sahakarbharati.ui.theme.SahakarBharatiTheme
import java.net.URI

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestLocalNetworkIfNeeded()
        enableEdgeToEdge()
        setContent {
            SahakarBharatiTheme {
                RathYatraScreen(modifier = Modifier.fillMaxSize())
            }
        }
    }

    /**
     * Android 16 drops connections to private addresses such as the emulator host
     * (10.0.2.2) until this permission is granted. The failure looks like a dead
     * server. Public HTTPS and device loopback do not need it.
     */
    private fun requestLocalNetworkIfNeeded() {
        if (Build.VERSION.SDK_INT < 37) return
        if (!hostNeedsLocalNetwork(BuildConfig.API_BASE_URL)) return
        val permission = Manifest.permission.ACCESS_LOCAL_NETWORK
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) return
        ActivityCompat.requestPermissions(this, arrayOf(permission), LOCAL_NETWORK_REQUEST)
    }

    private companion object {
        const val LOCAL_NETWORK_REQUEST = 41
    }
}

internal fun hostNeedsLocalNetwork(baseUrl: String): Boolean {
    val host = runCatching { URI(baseUrl).host }.getOrNull()?.lowercase() ?: return false
    if (host == "localhost" || host == "127.0.0.1" || host == "::1") return false
    val parts = host.split('.')
    if (parts.size != 4) return false
    val numbers = parts.map { it.toIntOrNull() ?: return false }
    val first = numbers[0]
    val second = numbers[1]
    return first == 10 || (first == 192 && second == 168) || (first == 172 && second in 16..31)
}
