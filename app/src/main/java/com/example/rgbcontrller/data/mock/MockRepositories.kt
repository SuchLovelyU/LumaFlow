package com.example.rgbcontrller.data.mock

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.rgbcontrller.data.bluetooth.AndroidBluetoothService
import com.example.rgbcontrller.data.bluetooth.BluetoothConnectionEvent
import com.example.rgbcontrller.data.bluetooth.BluetoothDeviceCandidate
import com.example.rgbcontrller.data.bluetooth.BluetoothService
import com.example.rgbcontrller.domain.engine.LightEngine
import com.example.rgbcontrller.domain.model.AppSettings
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
import com.example.rgbcontrller.domain.repository.SettingsRepository
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
    private val deviceState: StateFlow<DeviceInfo> = MutableStateFlow(MockCatalog.device),
    private val settingsState: StateFlow<AppSettings> = MutableStateFlow(AppSettings()),
    private val keyframeStore: KeyframeStore = InMemoryKeyframeStore(),
    private val engine: LightEngine = LightEngine(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : LightRepository {
    private var activeEffect: LightEffect? = MockCatalog.effects.first { it.id == "aurora" }
    private var liveControl = LiveControl(brightness = settingsState.value.defaultBrightness)
    private var keyframes = keyframeStore.load().ifEmpty { MockCatalog.keyframes }
    private var playback = PlaybackState(isPlaying = true, positionMs = 0)
    private var renderMode = RenderMode.Effect

    private val _session = MutableStateFlow(
        LightSessionState(
            device = deviceState.value,
            matrix = renderMatrix(0),
            activeEffect = activeEffect,
            liveControl = liveControl,
            playback = playback,
            keyframes = keyframes,
        ),
    )
    override val session: StateFlow<LightSessionState> = _session.asStateFlow()

    init {
        scope.launch {
            var tick = 0L
            while (true) {
                delay(FrameDelayMs)
                val speedScale = 0.25f + settingsState.value.animationSpeed * 1.75f
                tick += (FrameDelayMs * speedScale).toLong().coerceAtLeast(1L)
                playback = playback.copy(
                    positionMs = if (playback.isPlaying) tick else playback.positionMs,
                )
                _session.value = _session.value.copy(
                    device = deviceState.value,
                    matrix = renderMatrix(tick),
                    activeEffect = activeEffect,
                    liveControl = liveControl,
                    playback = playback,
                    keyframes = keyframes,
                )
                bluetoothService.sendFrame(_session.value.matrix)
            }
        }
    }

    override fun applyEffect(effect: LightEffect) {
        activeEffect = effect
        renderMode = RenderMode.Effect
    }

    override fun updateLiveControl(control: LiveControl) {
        liveControl = control
        activeEffect = null
        renderMode = RenderMode.Live
    }

    override fun updateKeyframes(keyframes: List<Keyframe>) {
        this.keyframes = keyframes
        keyframeStore.save(keyframes)
        activeEffect = null
        renderMode = RenderMode.Editor
    }

    override fun togglePlayback() {
        playback = playback.copy(isPlaying = !playback.isPlaying)
    }

    private fun renderMatrix(tick: Long) = when (renderMode) {
        RenderMode.Effect -> engine.render(
            rows = 2,
            columns = 4,
            tick = tick,
            effect = activeEffect,
            liveControl = liveControl.copy(brightness = settingsState.value.defaultBrightness),
        )
        RenderMode.Live -> engine.render(2, 4, tick, null, liveControl)
        RenderMode.Editor -> engine.renderKeyframes(2, 4, playback.positionMs, keyframes)
    }

    private enum class RenderMode {
        Effect,
        Live,
        Editor,
    }

    private companion object {
        const val FrameDelayMs = 34L
    }
}

interface KeyframeStore {
    fun load(): List<Keyframe>
    fun save(keyframes: List<Keyframe>)
}

class InMemoryKeyframeStore(
    initialKeyframes: List<Keyframe> = MockCatalog.keyframes,
) : KeyframeStore {
    private var keyframes = initialKeyframes

    override fun load(): List<Keyframe> = keyframes

    override fun save(keyframes: List<Keyframe>) {
        this.keyframes = keyframes
    }
}

class SharedPreferencesKeyframeStore(
    context: Context,
) : KeyframeStore {
    private val preferences = context.applicationContext.getSharedPreferences(KeyframePrefsName, Context.MODE_PRIVATE)

    override fun load(): List<Keyframe> {
        val encoded = preferences.getString(KeyKeyframes, null) ?: return MockCatalog.keyframes
        val decoded = KeyframeCodec.decode(encoded)
        return if (decoded.isEmpty() && encoded.isNotBlank()) MockCatalog.keyframes else decoded
    }

    override fun save(keyframes: List<Keyframe>) {
        preferences.edit {
            putString(KeyKeyframes, KeyframeCodec.encode(keyframes))
        }
    }

    private companion object {
        const val KeyframePrefsName = "rgb_controller_keyframes"
        const val KeyKeyframes = "keyframes"
    }
}

object KeyframeCodec {
    private const val FrameSeparator = ";"
    private const val FieldSeparator = "|"

    fun encode(keyframes: List<Keyframe>): String {
        return keyframes.joinToString(FrameSeparator) { keyframe ->
            listOf(
                keyframe.id.sanitize(),
                keyframe.color.red.coerceIn(0, 255).toString(),
                keyframe.color.green.coerceIn(0, 255).toString(),
                keyframe.color.blue.coerceIn(0, 255).toString(),
                keyframe.brightness.coerceIn(0f, 1f).toString(),
                keyframe.durationMs.coerceAtLeast(1).toString(),
            ).joinToString(FieldSeparator)
        }
    }

    fun decode(value: String): List<Keyframe> {
        if (value.isBlank()) return emptyList()
        return value.split(FrameSeparator)
            .mapNotNull { encodedFrame ->
                val fields = encodedFrame.split(FieldSeparator)
                if (fields.size != 6) return@mapNotNull null
                val red = fields[1].toIntOrNull()?.coerceIn(0, 255) ?: return@mapNotNull null
                val green = fields[2].toIntOrNull()?.coerceIn(0, 255) ?: return@mapNotNull null
                val blue = fields[3].toIntOrNull()?.coerceIn(0, 255) ?: return@mapNotNull null
                val brightness = fields[4].toFloatOrNull()?.coerceIn(0f, 1f) ?: return@mapNotNull null
                val durationMs = fields[5].toIntOrNull()?.coerceAtLeast(1) ?: return@mapNotNull null
                Keyframe(
                    id = fields[0].ifBlank { "kf-${red + green + blue + durationMs}" },
                    color = RgbColor(red, green, blue),
                    brightness = brightness,
                    durationMs = durationMs,
                )
            }
    }

    private fun String.sanitize(): String = replace(FrameSeparator, "_").replace(FieldSeparator, "_")
}

class MockDeviceRepository(
    private val bluetoothService: BluetoothService,
    private val deviceState: MutableStateFlow<DeviceInfo> = MutableStateFlow(MockCatalog.device),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : DeviceRepository {
    override val device: StateFlow<DeviceInfo> = deviceState.asStateFlow()
    override val discoveredDevices: StateFlow<List<BluetoothDeviceCandidate>> = bluetoothService.discoveredDevices

    private val _statusMessage = MutableStateFlow<String?>(null)
    override val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()
    private var lastNonSearchingStatus = deviceState.value.connectionStatus

    init {
        scope.launch {
            bluetoothService.connectionEvents.collect { event ->
                when (event) {
                    BluetoothConnectionEvent.Searching -> {
                        if (deviceState.value.connectionStatus != ConnectionStatus.Searching) {
                            lastNonSearchingStatus = deviceState.value.connectionStatus
                        }
                        deviceState.value = deviceState.value.copy(connectionStatus = ConnectionStatus.Searching)
                        _statusMessage.value = "Searching for Bluetooth devices..."
                    }
                    BluetoothConnectionEvent.ScanComplete -> {
                        deviceState.value = deviceState.value.copy(connectionStatus = lastNonSearchingStatus)
                        _statusMessage.value = "Scan complete. Select a device to connect."
                    }
                    is BluetoothConnectionEvent.Connected -> {
                        deviceState.value = deviceState.value.copy(
                            connectionStatus = ConnectionStatus.Connected,
                            name = discoveredDevices.value.firstOrNull { it.address == event.deviceAddress }?.name
                                ?: event.deviceAddress,
                        )
                        lastNonSearchingStatus = ConnectionStatus.Connected
                        _statusMessage.value = "Connected to ${deviceState.value.name}."
                    }
                    BluetoothConnectionEvent.Disconnected -> {
                        deviceState.value = deviceState.value.copy(connectionStatus = ConnectionStatus.Offline)
                        lastNonSearchingStatus = ConnectionStatus.Offline
                        _statusMessage.value = "Bluetooth disconnected."
                    }
                    is BluetoothConnectionEvent.Message -> {
                        _statusMessage.value = event.message
                    }
                    is BluetoothConnectionEvent.Error -> {
                        deviceState.value = deviceState.value.copy(connectionStatus = ConnectionStatus.Offline)
                        lastNonSearchingStatus = ConnectionStatus.Offline
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

open class MockSettingsRepository : SettingsRepository {
    protected val _settings = MutableStateFlow(AppSettings())
    override val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    override fun updateDarkMode(value: Boolean) {
        update { it.copy(darkMode = value) }
    }

    override fun updateDynamicTheme(value: Boolean) {
        update { it.copy(dynamicTheme = value) }
    }

    override fun updateDeveloperMode(value: Boolean) {
        update { it.copy(developerMode = value) }
    }

    override fun updateAnimationSpeed(value: Float) {
        update { it.copy(animationSpeed = value.coerceIn(0f, 1f)) }
    }

    override fun updateDefaultBrightness(value: Float) {
        update { it.copy(defaultBrightness = value.coerceIn(0f, 1f)) }
    }

    protected open fun update(transform: (AppSettings) -> AppSettings) {
        _settings.value = transform(_settings.value)
    }
}

class SharedPreferencesSettingsRepository(
    context: Context,
) : MockSettingsRepository() {
    private val preferences = context.applicationContext.getSharedPreferences(SettingsPrefsName, Context.MODE_PRIVATE)

    init {
        _settings.value = preferences.readSettings()
    }

    override fun update(transform: (AppSettings) -> AppSettings) {
        super.update(transform)
        preferences.edit {
            putBoolean(KeyDarkMode, settings.value.darkMode)
            putBoolean(KeyDynamicTheme, settings.value.dynamicTheme)
            putBoolean(KeyDeveloperMode, settings.value.developerMode)
            putFloat(KeyAnimationSpeed, settings.value.animationSpeed)
            putFloat(KeyDefaultBrightness, settings.value.defaultBrightness)
        }
    }

    private fun SharedPreferences.readSettings(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            darkMode = getBoolean(KeyDarkMode, defaults.darkMode),
            dynamicTheme = getBoolean(KeyDynamicTheme, defaults.dynamicTheme),
            developerMode = getBoolean(KeyDeveloperMode, defaults.developerMode),
            animationSpeed = getFloat(KeyAnimationSpeed, defaults.animationSpeed).coerceIn(0f, 1f),
            defaultBrightness = getFloat(KeyDefaultBrightness, defaults.defaultBrightness).coerceIn(0f, 1f),
        )
    }

    private companion object {
        const val SettingsPrefsName = "rgb_controller_settings"
        const val KeyDarkMode = "dark_mode"
        const val KeyDynamicTheme = "dynamic_theme"
        const val KeyDeveloperMode = "developer_mode"
        const val KeyAnimationSpeed = "animation_speed"
        const val KeyDefaultBrightness = "default_brightness"
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
    lateinit var settingsRepository: SettingsRepository
        private set

    fun initialize(context: Context) {
        if (::lightRepository.isInitialized) return
        bluetoothService = AndroidBluetoothService(context)
        settingsRepository = SharedPreferencesSettingsRepository(context)
        val sharedDeviceState = MutableStateFlow(MockCatalog.device)
        lightRepository = MockLightRepository(
            bluetoothService = bluetoothService,
            deviceState = sharedDeviceState,
            settingsState = settingsRepository.settings,
            keyframeStore = SharedPreferencesKeyframeStore(context),
        )
        deviceRepository = MockDeviceRepository(bluetoothService, sharedDeviceState)
    }
}
