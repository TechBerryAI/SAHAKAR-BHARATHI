package com.example.sahakarbharati

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.sahakarbharati.ui.navigation.RathYatraScreen
import com.example.sahakarbharati.ui.theme.SahakarBharatiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SahakarBharatiTheme {
                RathYatraScreen(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
