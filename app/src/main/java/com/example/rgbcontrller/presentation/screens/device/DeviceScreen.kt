package com.example.rgbcontrller.presentation.screens.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rgbcontrller.data.mock.AppContainer
import com.example.rgbcontrller.domain.model.ConnectionStatus
import com.example.rgbcontrller.domain.model.RgbColor
import com.example.rgbcontrller.domain.repository.DeviceRepository
import com.example.rgbcontrller.presentation.ui.components.AuroraBackground
import com.example.rgbcontrller.presentation.ui.components.DeviceStatusHeader
import com.example.rgbcontrller.presentation.ui.components.PageTitle

class DeviceViewModel(
    private val deviceRepository: DeviceRepository = AppContainer.deviceRepository,
) : ViewModel() {
    val device = deviceRepository.device
    val discoveredDevices = deviceRepository.discoveredDevices
    val statusMessage = deviceRepository.statusMessage

    fun scan() {
        deviceRepository.scan()
    }

    fun connect(address: String) {
        deviceRepository.connect(address)
    }

    fun disconnect() {
        deviceRepository.disconnect()
    }

    fun sendRedTest() {
        deviceRepository.sendAll(RgbColor(255, 0, 0), 0.5f)
    }

    fun turnOff() {
        deviceRepository.sendAll(RgbColor(0, 0, 0), 0f)
    }
}

@Composable
fun DeviceScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeviceViewModel = viewModel(),
) {
    val device by viewModel.device.collectAsState()
    val devices by viewModel.discoveredDevices.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isSearching = device.connectionStatus == ConnectionStatus.Searching
    val isConnected = device.connectionStatus == ConnectionStatus.Connected
    AuroraBackground(modifier.fillMaxSize()) {
        Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        PageTitle(
                            title = "Device",
                            modifier = Modifier.weight(1f),
                            subtitle = "Connect the 2x4 WS2812 Bluetooth controller",
                        )
                    }
                }
                item {
                    DeviceStatusHeader(device, onClick = onOpenSettings)
                }
                item {
                    WhiteCard {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            InfoRow("Device name", device.name)
                            InfoRow("Firmware", device.firmwareVersion)
                            InfoRow("LED count", "${device.ledCount}")
                            InfoRow("Bluetooth", device.connectionStatus.name)
                            statusMessage?.let {
                                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            modifier = Modifier.weight(1f),
                            enabled = !isSearching,
                            onClick = viewModel::scan,
                        ) {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(Icons.Filled.Search, contentDescription = null)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(if (isSearching) "Scanning" else "Scan")
                        }
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            enabled = isConnected,
                            onClick = viewModel::disconnect,
                        ) {
                            Icon(Icons.Filled.LinkOff, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Disconnect")
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            modifier = Modifier.weight(1f),
                            enabled = isConnected,
                            onClick = viewModel::sendRedTest,
                        ) {
                            Icon(Icons.Filled.FlashOn, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Test red")
                        }
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            enabled = isConnected,
                            onClick = viewModel::turnOff,
                        ) {
                            Icon(Icons.Filled.PowerSettingsNew, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Lights off")
                        }
                    }
                }
                item {
                    Text("Bluetooth devices", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                if (devices.isEmpty()) {
                    item {
                        WhiteCard {
                            Text(
                                "Tap Scan. Paired UART modules will appear first.",
                                modifier = Modifier.padding(18.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    items(devices, key = { it.address }) { bluetoothDevice ->
                        WhiteCard {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                InfoRow(bluetoothDevice.name ?: "Unknown device", bluetoothDevice.address)
                                InfoRow("Pairing", if (bluetoothDevice.isBonded) "Paired" else "Discovered")
                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isSearching,
                                    onClick = { viewModel.connect(bluetoothDevice.address) },
                                ) {
                                    Text("Connect")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WhiteCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        content = { content() },
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}
