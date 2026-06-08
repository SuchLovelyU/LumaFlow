package com.example.rgbcontrller

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.example.rgbcontrller.data.mock.AppContainer
import com.example.rgbcontrller.presentation.navigation.LightDeckApp
import com.example.rgbcontrller.ui.theme.RGBContrllerTheme

class MainActivity : ComponentActivity() {
    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContainer.initialize(applicationContext)
        bluetoothPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            ),
        )
        enableEdgeToEdge()
        setContent {
            RGBContrllerTheme {
                LightDeckApp()
            }
        }
    }
}
