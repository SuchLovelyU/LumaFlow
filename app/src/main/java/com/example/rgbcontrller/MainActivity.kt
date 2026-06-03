package com.example.rgbcontrller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.rgbcontrller.presentation.navigation.LightDeckApp
import com.example.rgbcontrller.ui.theme.RGBContrllerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RGBContrllerTheme {
                LightDeckApp()
            }
        }
    }
}
