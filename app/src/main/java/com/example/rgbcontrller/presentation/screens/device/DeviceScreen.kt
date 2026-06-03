package com.example.rgbcontrller.presentation.screens.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rgbcontrller.data.mock.AppContainer
import com.example.rgbcontrller.domain.repository.DeviceRepository
import com.example.rgbcontrller.presentation.ui.components.AuroraBackground
import com.example.rgbcontrller.presentation.ui.components.DeviceStatusHeader
import com.example.rgbcontrller.presentation.ui.components.PageTitle

class DeviceViewModel(
    deviceRepository: DeviceRepository = AppContainer.deviceRepository,
) : ViewModel() {
    val device = deviceRepository.device
}

@Composable
fun DeviceScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeviceViewModel = viewModel(),
) {
    val device by viewModel.device.collectAsState()
    AuroraBackground(modifier.fillMaxSize()) {
        Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    PageTitle("Device", "Mock hardware profile and future Bluetooth boundary")
                }
                item {
                    DeviceStatusHeader(device, onClick = onOpenSettings)
                }
                item {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            InfoRow("Device name", device.name)
                            InfoRow("Firmware", device.firmwareVersion)
                            InfoRow("LED count", "${device.ledCount}")
                            InfoRow("Battery", "${device.batteryPercent}%")
                            InfoRow("BluetoothService", "Reserved interface, not implemented")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}
