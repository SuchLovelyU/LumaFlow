package com.example.rgbcontrller

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
        bluetoothPermissionLauncher.launch(requiredBluetoothPermissions())
        enableEdgeToEdge()
        setContent {
            val settings by AppContainer.settingsRepository.settings.collectAsState()
            RGBContrllerTheme(
                darkTheme = settings.darkMode,
                dynamicColor = settings.dynamicTheme,
            ) {
                LightDeckApp()
            }
        }
    }

    private fun requiredBluetoothPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
}
