package com.example.rgbcontrller.data.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import com.example.rgbcontrller.domain.model.LedMatrix
import com.example.rgbcontrller.domain.model.RgbColor
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AndroidBluetoothService(
    context: Context,
) : BluetoothService {
    private val appContext = context.applicationContext
    private val bluetoothManager = appContext.getSystemService(BluetoothManager::class.java)
    private val adapter = bluetoothManager?.adapter
    private val scanner get() = adapter?.bluetoothLeScanner
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeMutex = Mutex()

    private var gatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var negotiatedPayloadSize = DefaultPayloadSize
    private var scanning = false

    private val _connectionEvents = MutableSharedFlow<BluetoothConnectionEvent>(extraBufferCapacity = 16)
    override val connectionEvents: SharedFlow<BluetoothConnectionEvent> = _connectionEvents.asSharedFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDeviceCandidate>>(emptyList())
    override val discoveredDevices: StateFlow<List<BluetoothDeviceCandidate>> = _discoveredDevices.asStateFlow()

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            addDevice(
                BluetoothDeviceCandidate(
                    name = result.scanRecord?.deviceName ?: device.safeName(),
                    address = device.address,
                    isBonded = device.bondState == BluetoothDevice.BOND_BONDED,
                ),
            )
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, it) }
        }

        override fun onScanFailed(errorCode: Int) {
            scope.launch {
                _connectionEvents.emit(BluetoothConnectionEvent.Error("BLE scan failed: $errorCode."))
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                scope.launch {
                    closeGatt()
                    _connectionEvents.emit(BluetoothConnectionEvent.Error("BLE connection failed: $status."))
                }
                return
            }

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    this@AndroidBluetoothService.gatt = gatt
                    gatt.requestMtu(PreferredMtu)
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    scope.launch {
                        closeGatt()
                        _connectionEvents.emit(BluetoothConnectionEvent.Disconnected)
                    }
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                negotiatedPayloadSize = (mtu - GattHeaderSize).coerceAtLeast(DefaultPayloadSize)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            scope.launch {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    _connectionEvents.emit(BluetoothConnectionEvent.Error("BLE service discovery failed: $status."))
                    return@launch
                }

                val writable = findWriteCharacteristic(gatt.services)
                if (writable == null) {
                    closeGatt()
                    _connectionEvents.emit(BluetoothConnectionEvent.Error("No writable BLE UART characteristic found."))
                    return@launch
                }

                writable.writeType = if (writable.supportsWriteNoResponse()) {
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                } else {
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                }
                writeCharacteristic = writable
                _connectionEvents.emit(BluetoothConnectionEvent.Connected(gatt.device.address))
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun scan() {
        val bluetoothAdapter = adapter
        val bluetoothScanner = scanner
        if (bluetoothAdapter == null || bluetoothScanner == null) {
            _connectionEvents.emit(BluetoothConnectionEvent.Error("This device does not support BLE."))
            return
        }
        if (!hasBluetoothPermission()) {
            _connectionEvents.emit(BluetoothConnectionEvent.Error("Bluetooth permission is required."))
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            _connectionEvents.emit(BluetoothConnectionEvent.Error("Bluetooth is turned off."))
            return
        }

        _connectionEvents.emit(BluetoothConnectionEvent.Searching)
        _discoveredDevices.value = bluetoothAdapter.bondedDevices.orEmpty().map {
            BluetoothDeviceCandidate(it.safeName(), it.address, isBonded = true)
        }.sortedWith(deviceComparator)

        if (scanning) {
            bluetoothScanner.stopScan(scanCallback)
        }
        scanning = true
        bluetoothScanner.startScan(
            null,
            ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build(),
            scanCallback,
        )
    }

    @SuppressLint("MissingPermission")
    override suspend fun connect(deviceAddress: String) {
        val bluetoothAdapter = adapter
        val bluetoothScanner = scanner
        if (bluetoothAdapter == null) {
            _connectionEvents.emit(BluetoothConnectionEvent.Error("This device does not support BLE."))
            return
        }
        if (!hasBluetoothPermission()) {
            _connectionEvents.emit(BluetoothConnectionEvent.Error("Bluetooth permission is required."))
            return
        }

        withContext(Dispatchers.IO) {
            if (scanning) {
                bluetoothScanner?.stopScan(scanCallback)
                scanning = false
            }
            closeGatt()
            _connectionEvents.emit(BluetoothConnectionEvent.Searching)
            negotiatedPayloadSize = DefaultPayloadSize
            val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
            gatt = device.connectGatt(appContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        }
    }

    override suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            closeGatt()
            _connectionEvents.emit(BluetoothConnectionEvent.Disconnected)
        }
    }

    override suspend fun sendFrame(matrix: LedMatrix) {
        writeBytes(Ws2812Protocol.setFrame8(matrix), reportIfDisconnected = false)
    }

    override suspend fun sendAll(color: RgbColor, brightness: Float) {
        writeBytes(Ws2812Protocol.setAllLed(color, brightness), reportIfDisconnected = true)
    }

    private suspend fun writeBytes(bytes: ByteArray, reportIfDisconnected: Boolean) {
        withContext(Dispatchers.IO) {
            writeMutex.withLock {
                val activeGatt = gatt
                val characteristic = writeCharacteristic
                if (activeGatt == null || characteristic == null) {
                    if (reportIfDisconnected) {
                        _connectionEvents.emit(BluetoothConnectionEvent.Error("No BLE device is connected."))
                    }
                    return@withLock
                }

                val chunkSize = negotiatedPayloadSize.coerceAtLeast(DefaultPayloadSize)
                bytes.asIterable().chunked(chunkSize).forEach { chunk ->
                    val payload = chunk.toByteArray()
                    val result = activeGatt.writeCharacteristic(
                        characteristic,
                        payload,
                        characteristic.writeType,
                    )
                    if (result != BluetoothStatusCodes.SUCCESS) {
                        _connectionEvents.emit(BluetoothConnectionEvent.Error("BLE write failed: $result."))
                        return@withLock
                    }
                    delay(WriteGapMs)
                }
            }
        }
    }

    private fun findWriteCharacteristic(services: List<BluetoothGattService>): BluetoothGattCharacteristic? {
        val byPreferredUuid = PreferredWriteCharacteristicUuids.firstNotNullOfOrNull { uuid ->
            services.firstNotNullOfOrNull { service -> service.getCharacteristic(uuid) }
        }
        if (byPreferredUuid?.isWritable() == true) return byPreferredUuid

        return services
            .flatMap { it.characteristics }
            .firstOrNull { it.isWritable() }
    }

    private fun addDevice(device: BluetoothDeviceCandidate) {
        _discoveredDevices.value = (_discoveredDevices.value.filterNot { it.address == device.address } + device)
            .sortedWith(deviceComparator)
    }

    private fun hasBluetoothPermission(): Boolean {
        return appContext.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            appContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun closeGatt() {
        writeCharacteristic = null
        runCatching { gatt?.disconnect() }
        runCatching { gatt?.close() }
        gatt = null
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.safeName(): String? = runCatching { name }.getOrNull()

    private fun BluetoothGattCharacteristic.isWritable(): Boolean {
        return supportsWrite() || supportsWriteNoResponse()
    }

    private fun BluetoothGattCharacteristic.supportsWrite(): Boolean {
        return properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0
    }

    private fun BluetoothGattCharacteristic.supportsWriteNoResponse(): Boolean {
        return properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0
    }

    private companion object {
        const val PreferredMtu = 64
        const val GattHeaderSize = 3
        const val DefaultPayloadSize = 20
        const val WriteGapMs = 8L

        val PreferredWriteCharacteristicUuids = listOf(
            UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"), // HM-10 / JDY style UART
            UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e"), // Nordic UART RX
            UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb"),
            UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb"),
        )

        val deviceComparator = compareByDescending<BluetoothDeviceCandidate> { it.name != null }
            .thenByDescending { it.isBonded }
            .thenBy { it.name ?: "Unknown device" }
            .thenBy { it.address }
    }
}
