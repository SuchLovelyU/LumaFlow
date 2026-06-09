package com.example.rgbcontrller

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.example.rgbcontrller.data.mock.AppContainer
import com.example.rgbcontrller.presentation.navigation.LightDeckApp
import com.example.rgbcontrller.ui.theme.RGBContrllerTheme

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        AppContainer.setMicrophoneEnabled(results[Manifest.permission.RECORD_AUDIO] == true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContainer.initialize(applicationContext)
        AppContainer.setMicrophoneEnabled(hasPermission(Manifest.permission.RECORD_AUDIO))
        permissionLauncher.launch(requiredRuntimePermissions())
        enableEdgeToEdge()
        setContent {
            val settings by AppContainer.settingsRepository.settings.collectAsState()
            RGBContrllerTheme(
                darkTheme = settings.darkMode,
                dynamicColor = false,
            ) {
                LightDeckApp()
            }
        }
    }

    private fun requiredRuntimePermissions(): Array<String> {
        val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        return bluetoothPermissions + Manifest.permission.RECORD_AUDIO
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
}
