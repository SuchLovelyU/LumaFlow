package com.example.rgbcontrller.data.mock

import android.content.Context
import com.example.rgbcontrller.data.bluetooth.AndroidBluetoothService
import com.example.rgbcontrller.data.bluetooth.BluetoothConnectionEvent
import com.example.rgbcontrller.data.bluetooth.BluetoothDeviceCandidate
import com.example.rgbcontrller.data.bluetooth.BluetoothService
import com.example.rgbcontrller.domain.engine.LightEngine
import com.example.rgbcontrller.domain.model.ConnectionStatus
import com.example.rgbcontrller.domain.model.DeviceInfo
import com.example.rgbcontrller.domain.model.Keyframe
import com.example.rgbcontrller.domain.model.LightEffect
import com.example.rgbcontrller.domain.model.LightSessionState
import com.example.rgbcontrller.domain.model.LiveControl
import com.example.rgbcontrller.domain.model.PlaybackState
import com.example.rgbcontrller.domain.model.RgbColor
import com.example.rgbcontrller.domain.model.SensorMode
import com.example.rgbcontrller.domain.model.SensorSnapshot
import com.example.rgbcontrller.domain.model.Vector3
import com.example.rgbcontrller.domain.repository.DeviceRepository
import com.example.rgbcontrller.domain.repository.EffectRepository
import com.example.rgbcontrller.domain.repository.LightRepository
import com.example.rgbcontrller.domain.repository.SensorRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

class MockLightRepository(
    private val bluetoothService: BluetoothService,
    private val engine: LightEngine = LightEngine(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : LightRepository {
    private var activeEffect: LightEffect? = MockCatalog.effects.first { it.id == "aurora" }
    private var liveControl = LiveControl()
    private var keyframes = MockCatalog.keyframes
    private var playback = PlaybackState(isPlaying = true, positionMs = 0)

    private val _session = MutableStateFlow(
        LightSessionState(
            device = MockCatalog.device,
            matrix = engine.render(2, 4, 0, activeEffect, liveControl),
            activeEffect = activeEffect,
            liveControl = liveControl,
            playback = playback,
        ),
    )
    override val session: StateFlow<LightSessionState> = _session.asStateFlow()

    init {
        scope.launch {
            var tick = 0L
            while (true) {
                delay(34)
                tick += 34
                playback = playback.copy(
                    positionMs = if (playback.isPlaying) tick else playback.positionMs,
                )
                _session.value = _session.value.copy(
                    matrix = engine.render(2, 4, tick, activeEffect, liveControl),
                    activeEffect = activeEffect,
                    liveControl = liveControl,
                    playback = playback,
                )
                bluetoothService.sendFrame(_session.value.matrix)
            }
        }
    }

    override fun applyEffect(effect: LightEffect) {
        activeEffect = effect
    }

    override fun updateLiveControl(control: LiveControl) {
        liveControl = control
        activeEffect = null
    }

    override fun updateKeyframes(keyframes: List<Keyframe>) {
        this.keyframes = keyframes
    }

    override fun togglePlayback() {
        playback = playback.copy(isPlaying = !playback.isPlaying)
    }
}

class MockDeviceRepository(
    private val bluetoothService: BluetoothService,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : DeviceRepository {
    private val _device = MutableStateFlow(MockCatalog.device)
    override val device: StateFlow<DeviceInfo> = _device.asStateFlow()
    override val discoveredDevices: StateFlow<List<BluetoothDeviceCandidate>> = bluetoothService.discoveredDevices

    private val _statusMessage = MutableStateFlow<String?>(null)
    override val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        scope.launch {
            bluetoothService.connectionEvents.collect { event ->
                when (event) {
                    BluetoothConnectionEvent.Searching -> {
                        _device.value = _device.value.copy(connectionStatus = ConnectionStatus.Searching)
                        _statusMessage.value = "Searching for Bluetooth devices..."
                    }
                    is BluetoothConnectionEvent.Connected -> {
                        _device.value = _device.value.copy(
                            connectionStatus = ConnectionStatus.Connected,
                            name = discoveredDevices.value.firstOrNull { it.address == event.deviceAddress }?.name
                                ?: event.deviceAddress,
                        )
                        _statusMessage.value = "Connected to ${_device.value.name}."
                    }
                    BluetoothConnectionEvent.Disconnected -> {
                        _device.value = _device.value.copy(connectionStatus = ConnectionStatus.Offline)
                        _statusMessage.value = "Bluetooth disconnected."
                    }
                    is BluetoothConnectionEvent.Error -> {
                        _device.value = _device.value.copy(connectionStatus = ConnectionStatus.Offline)
                        _statusMessage.value = event.message
                    }
                }
            }
        }
    }

    override fun scan() {
        scope.launch {
            bluetoothService.scan()
        }
    }

    override fun connect(deviceAddress: String) {
        scope.launch {
            bluetoothService.connect(deviceAddress)
        }
    }

    override fun disconnect() {
        scope.launch {
            bluetoothService.disconnect()
        }
    }

    override fun sendAll(color: RgbColor, brightness: Float) {
        scope.launch {
            bluetoothService.sendAll(color, brightness)
        }
    }
}

class MockEffectRepository : EffectRepository {
    override val effects: List<LightEffect> = MockCatalog.effects
}

class MockSensorRepository(
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : SensorRepository {
    override val modes: List<SensorMode> = MockCatalog.sensorModes

    private val _snapshot = MutableStateFlow(
        SensorSnapshot(
            microphoneLevel = 0.4f,
            gravity = Vector3(0f, 0.5f, 0.8f),
            gyroscope = Vector3(0.1f, 0f, 0.2f),
            shakeIntensity = 0.15f,
        ),
    )
    override val snapshot: StateFlow<SensorSnapshot> = _snapshot.asStateFlow()

    init {
        scope.launch {
            var t = 0f
            while (true) {
                delay(48)
                t += 0.07f
                _snapshot.value = SensorSnapshot(
                    microphoneLevel = (0.22f + kotlin.math.abs(sin(t * 1.9f)) * 0.78f),
                    gravity = Vector3(sin(t) * 0.8f, cos(t * 0.75f) * 0.8f, 0.6f),
                    gyroscope = Vector3(cos(t * 1.2f) * 0.7f, sin(t * 0.9f) * 0.7f, sin(t) * 0.45f),
                    shakeIntensity = if ((t.toInt() % 5) == 0) 0.85f else kotlin.math.abs(sin(t * 2.6f)) * 0.35f,
                )
            }
        }
    }
}

object AppContainer {
    private lateinit var bluetoothService: BluetoothService
    lateinit var lightRepository: LightRepository
        private set
    lateinit var deviceRepository: DeviceRepository
        private set
    val effectRepository: EffectRepository = MockEffectRepository()
    val sensorRepository: SensorRepository = MockSensorRepository()

    fun initialize(context: Context) {
        if (::lightRepository.isInitialized) return
        bluetoothService = AndroidBluetoothService(context)
        lightRepository = MockLightRepository(bluetoothService)
        deviceRepository = MockDeviceRepository(bluetoothService)
    }
}
