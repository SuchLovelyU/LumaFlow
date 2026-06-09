package com.example.rgbcontrller.data.bluetooth

import com.example.rgbcontrller.domain.model.RgbColor
import com.example.rgbcontrller.domain.model.LedMatrix
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothService {
    val connectionEvents: Flow<BluetoothConnectionEvent>
    val discoveredDevices: StateFlow<List<BluetoothDeviceCandidate>>
    suspend fun scan()
    suspend fun connect(deviceAddress: String)
    suspend fun disconnect()
    suspend fun sendFrame(matrix: LedMatrix)
    suspend fun sendAll(color: RgbColor, brightness: Float)
}

data class BluetoothDeviceCandidate(
    val name: String?,
    val address: String,
    val isBonded: Boolean,
)

sealed interface BluetoothConnectionEvent {
    data object Searching : BluetoothConnectionEvent
    data object ScanComplete : BluetoothConnectionEvent
    data class Connected(val deviceAddress: String) : BluetoothConnectionEvent
    data object Disconnected : BluetoothConnectionEvent
    data class Message(val message: String) : BluetoothConnectionEvent
    data class Error(val message: String) : BluetoothConnectionEvent
}
