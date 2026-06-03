package com.example.rgbcontrller.data.bluetooth

import com.example.rgbcontrller.domain.model.LedMatrix
import kotlinx.coroutines.flow.Flow

interface BluetoothService {
    val connectionEvents: Flow<BluetoothConnectionEvent>
    suspend fun scan()
    suspend fun connect(deviceAddress: String)
    suspend fun disconnect()
    suspend fun sendFrame(matrix: LedMatrix)
}

sealed interface BluetoothConnectionEvent {
    data object Searching : BluetoothConnectionEvent
    data class Connected(val deviceAddress: String) : BluetoothConnectionEvent
    data object Disconnected : BluetoothConnectionEvent
    data class Error(val message: String) : BluetoothConnectionEvent
}
