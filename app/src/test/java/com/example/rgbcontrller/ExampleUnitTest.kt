package com.example.rgbcontrller

import com.example.rgbcontrller.data.bluetooth.BluetoothConnectionEvent
import com.example.rgbcontrller.data.bluetooth.BluetoothDeviceCandidate
import com.example.rgbcontrller.data.bluetooth.BluetoothService
import com.example.rgbcontrller.data.bluetooth.Ws2812Protocol
import com.example.rgbcontrller.data.mock.KeyframeCodec
import com.example.rgbcontrller.data.mock.MockCatalog
import com.example.rgbcontrller.data.mock.MockDeviceRepository
import com.example.rgbcontrller.data.mock.MockSettingsRepository
import com.example.rgbcontrller.domain.engine.LightEngine
import com.example.rgbcontrller.domain.model.AppSettings
import com.example.rgbcontrller.domain.model.ConnectionStatus
import com.example.rgbcontrller.domain.model.Keyframe
import com.example.rgbcontrller.domain.model.LedMatrix
import com.example.rgbcontrller.domain.model.LedPixel
import com.example.rgbcontrller.domain.model.LightEffect
import com.example.rgbcontrller.domain.model.LightSessionState
import com.example.rgbcontrller.domain.model.LiveControl
import com.example.rgbcontrller.domain.model.PlaybackState
import com.example.rgbcontrller.domain.model.RgbColor
import com.example.rgbcontrller.domain.model.SensorMode
import com.example.rgbcontrller.domain.model.SensorSnapshot
import com.example.rgbcontrller.domain.model.Vector3
import com.example.rgbcontrller.domain.repository.EffectRepository
import com.example.rgbcontrller.domain.repository.LightRepository
import com.example.rgbcontrller.domain.repository.SensorRepository
import com.example.rgbcontrller.presentation.screens.dashboard.DashboardViewModel
import com.example.rgbcontrller.presentation.screens.editor.EditorViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun setOneLed_matchesProtocolExample() {
        val frame = Ws2812Protocol.setOneLed(
            id = 0,
            color = RgbColor(0, 0, 255),
            brightness = 1f,
        )

        assertEquals("AA 55 01 00 00 00 FF FF 01", frame.toHex())
    }

    @Test
    fun setAllLed_matchesProtocolExample() {
        val frame = Ws2812Protocol.setAllLed(
            color = RgbColor(255, 0, 0),
            brightness = 128 / 255f,
        )

        assertEquals("AA 55 02 FF 00 00 80 7D", frame.toHex())
    }

    @Test
    fun setFrame8_sendsThirtySixByteFrameWithChecksum() {
        val colors = listOf(
            RgbColor(255, 0, 0),
            RgbColor(0, 255, 0),
            RgbColor(0, 0, 255),
            RgbColor(255, 255, 0),
            RgbColor(0, 255, 255),
            RgbColor(128, 0, 255),
            RgbColor(255, 255, 255),
            RgbColor(0, 0, 0),
        )
        val matrix = LedMatrix(
            rows = 2,
            columns = 4,
            pixels = colors.mapIndexed { index, color ->
                LedPixel(id = index, color = color, brightness = if (index == 7) 0f else 1f)
            },
        )

        val frame = Ws2812Protocol.setFrame8(matrix)

        assertEquals(36, frame.size)
        assertEquals(
            "AA 55 10 FF FF 00 FF 00 00 FF FF 00 FF 00 FF FF 00 00 FF 00 FF FF FF 80 00 FF FF FF FF FF FF 00 00 00 FF 6F",
            frame.toHex(),
        )
    }

    @Test
    fun setFrame8_mapsSerpentineHardwareAndPremultipliesBrightness() {
        val matrix = LedMatrix(
            rows = 2,
            columns = 4,
            pixels = List(8) { index ->
                LedPixel(
                    id = index,
                    color = RgbColor((index + 1) * 10, 0, 0),
                    brightness = if (index == 4) 0.25f else 1f,
                )
            },
        )

        val payload = Ws2812Protocol.setFrame8(matrix).drop(3).dropLast(1)

        assertEquals(40, payload[0].toInt() and 0xFF)
        assertEquals(12, payload[16].toInt() and 0xFF)
        assertEquals(60, payload[20].toInt() and 0xFF)
        assertEquals(70, payload[24].toInt() and 0xFF)
        assertEquals(80, payload[28].toInt() and 0xFF)
        assertEquals(255, payload[31].toInt() and 0xFF)
    }

    @Test
    fun renderKeyframes_interpolatesTimelinePreview() {
        val matrix = LightEngine().renderKeyframes(
            rows = 2,
            columns = 4,
            positionMs = 500,
            keyframes = listOf(
                Keyframe("a", RgbColor(255, 0, 0), 1f, 1_000),
                Keyframe("b", RgbColor(0, 0, 255), 0.5f, 1_000),
            ),
        )

        assertEquals(8, matrix.pixels.size)
        assertTrue(matrix.pixels.first().color.red in 120..135)
        assertTrue(matrix.pixels.first().color.blue in 120..135)
        assertTrue(matrix.pixels.all { it.brightness in 0f..1f })
    }

    @Test
    fun musicPulse_staysOffBelowThresholdAndLightsAboveIt() {
        val effect = MockCatalog.effects.first { it.id == "music" }
        val control = LiveControl(brightness = 1f, musicThreshold = 0.6f)
        val engine = LightEngine()

        val quiet = engine.render(
            rows = 2,
            columns = 4,
            tick = 0,
            effect = effect,
            liveControl = control,
            sensorSnapshot = SensorSnapshot(0.4f, Vector3(0f, 1f, 0f), Vector3(0f, 0f, 0f), 0f),
        )
        val loud = engine.render(
            rows = 2,
            columns = 4,
            tick = 0,
            effect = effect,
            liveControl = control,
            sensorSnapshot = SensorSnapshot(0.9f, Vector3(0f, 1f, 0f), Vector3(0f, 0f, 0f), 0f),
        )

        assertTrue(quiet.pixels.all { it.brightness == 0f })
        assertTrue(loud.pixels.any { it.brightness > 0.3f })
    }

    @Test
    fun gravityFluid_usesContinuousBrightnessAcrossTilt() {
        val effect = MockCatalog.effects.first { it.id == "gravity" }
        val matrix = LightEngine().render(
            rows = 2,
            columns = 4,
            tick = 0,
            effect = effect,
            liveControl = LiveControl(brightness = 1f, fluidLevel = 0.5f, fluidDensity = 0.5f),
            sensorSnapshot = SensorSnapshot(0f, Vector3(1f, 0f, 0f), Vector3(0f, 0f, 0f), 0f),
        )

        val left = matrix.pixels.filter { it.id % matrix.columns == 0 }.map { it.brightness }.average()
        val right = matrix.pixels.filter { it.id % matrix.columns == matrix.columns - 1 }.map { it.brightness }.average()
        val uniqueBrightnessValues = matrix.pixels.map { (it.brightness * 100).toInt() }.toSet()

        assertTrue(left > right)
        assertTrue(uniqueBrightnessValues.size > 2)
    }

    @Test
    fun runAndBreathEffectsHaveClearBrightnessIntent() {
        val engine = LightEngine()
        val run = engine.render(
            rows = 2,
            columns = 4,
            tick = 0,
            effect = MockCatalog.effects.first { it.id == "run" },
            liveControl = LiveControl(brightness = 1f),
        )
        val breath = engine.render(
            rows = 2,
            columns = 4,
            tick = 0,
            effect = MockCatalog.effects.first { it.id == "breath" },
            liveControl = LiveControl(brightness = 1f),
        )

        assertTrue(run.pixels.any { it.brightness == 0f })
        assertTrue(run.pixels.maxOf { it.brightness } - run.pixels.minOf { it.brightness } > 0.8f)
        assertTrue(breath.pixels.map { (it.brightness * 100).toInt() }.toSet().size <= 2)
    }

    @Test
    fun keyframeCodec_roundTripsAndSkipsInvalidFrames() {
        val keyframes = listOf(
            Keyframe("kf-custom", RgbColor(12, 34, 56), 0.67f, 789),
            Keyframe("kf-overflow", RgbColor(300, -20, 128), 2f, -1),
        )

        val decoded = KeyframeCodec.decode("${KeyframeCodec.encode(keyframes)};bad|frame")

        assertEquals(2, decoded.size)
        assertEquals(Keyframe("kf-custom", RgbColor(12, 34, 56), 0.67f, 789), decoded[0])
        assertEquals(Keyframe("kf-overflow", RgbColor(255, 0, 128), 1f, 1), decoded[1])
    }

    @Test
    fun settingsRepository_persistsFlagsAndClampsSliderValues() {
        val repository = MockSettingsRepository()

        repository.updateDarkMode(true)
        repository.updateDynamicTheme(false)
        repository.updateAnimationSpeed(2f)
        repository.updateDefaultBrightness(-1f)
        repository.updateMasterBrightnessLimit(2f)

        assertTrue(repository.settings.value.darkMode)
        assertFalse(repository.settings.value.dynamicTheme)
        assertEquals(1f, repository.settings.value.animationSpeed, 0f)
        assertEquals(0f, repository.settings.value.defaultBrightness, 0f)
        assertEquals(1f, repository.settings.value.masterBrightnessLimit, 0f)
    }

    @Test
    fun deviceRepository_capsDirectSendAllByMasterBrightnessLimit() = runBlocking {
        val bluetoothService = FakeBluetoothService()
        val settings = MutableStateFlow(AppSettings(masterBrightnessLimit = 0.35f))
        val repository = MockDeviceRepository(bluetoothService = bluetoothService, settingsState = settings)

        repository.sendAll(RgbColor.White, 0.8f)
        delay(20)

        assertEquals(0.35f, bluetoothService.lastSendAllBrightness ?: -1f, 0f)
    }

    @Test
    fun deviceRepository_restoresStatusWhenScanCompletes() = runBlocking {
        val bluetoothService = FakeBluetoothService()
        val repository = MockDeviceRepository(bluetoothService)

        bluetoothService.emit(BluetoothConnectionEvent.Searching)
        delay(20)
        assertEquals(ConnectionStatus.Searching, repository.device.value.connectionStatus)

        bluetoothService.emit(BluetoothConnectionEvent.ScanComplete)
        delay(20)
        assertEquals(ConnectionStatus.Offline, repository.device.value.connectionStatus)

        bluetoothService.emit(BluetoothConnectionEvent.Connected("AA:BB:CC:DD:EE:FF"))
        delay(20)
        bluetoothService.emit(BluetoothConnectionEvent.Searching)
        delay(20)
        bluetoothService.emit(BluetoothConnectionEvent.ScanComplete)
        delay(20)

        assertEquals(ConnectionStatus.Connected, repository.device.value.connectionStatus)
    }

    @Test
    fun editorViewModel_updatesSelectedKeyframeColor() {
        val repository = FakeLightRepository()
        val viewModel = EditorViewModel(repository)

        viewModel.updateSelectedRed(0f)
        viewModel.updateSelectedGreen(1f)
        viewModel.updateSelectedBlue(0.5f)

        val selected = viewModel.keyframes.first { it.id == viewModel.selectedId }
        assertEquals(RgbColor(0, 255, 127), selected.color)
        assertEquals(selected.color, repository.lastKeyframes.first().color)
    }

    @Test
    fun editorViewModel_initializesKeyframesFromRepositorySession() {
        val customKeyframes = listOf(Keyframe("custom-1", RgbColor.Amber, 0.4f, 900))
        val viewModel = EditorViewModel(FakeLightRepository(initialKeyframes = customKeyframes))

        assertEquals(customKeyframes, viewModel.keyframes)
        assertEquals("custom-1", viewModel.selectedId)
    }

    @Test
    fun editorViewModel_duplicatesAndReordersKeyframesWithStableIds() {
        val repository = FakeLightRepository()
        val viewModel = EditorViewModel(repository)

        viewModel.select("kf-2")
        viewModel.duplicateSelected()
        val duplicated = viewModel.keyframes[2]

        assertEquals("kf-5", duplicated.id)
        assertEquals("kf-5", viewModel.selectedId)
        assertEquals(MockCatalog.keyframes[1].color, duplicated.color)

        viewModel.moveSelectedLeft()
        assertEquals("kf-5", viewModel.keyframes[1].id)
        assertEquals(viewModel.keyframes.map { it.id }, repository.lastKeyframes.map { it.id })

        viewModel.deleteSelected()
        viewModel.addKeyframe()
        assertEquals(viewModel.keyframes.size, viewModel.keyframes.map { it.id }.toSet().size)
    }

    @Test
    fun dashboardViewModel_activatesSensorMappedLightEffect() {
        val lightRepository = FakeLightRepository()
        val viewModel = DashboardViewModel(
            lightRepository = lightRepository,
            effectRepository = FakeEffectRepository(),
            sensorRepository = FakeSensorRepository(),
        )

        viewModel.activateSensorMode(MockCatalog.sensorModes.first { it.id == "gyro" })
        assertEquals("gyro", viewModel.activeSensorModeId)
        assertEquals("direction", lightRepository.lastEffect?.id)
    }

    private fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }
}

private class FakeLightRepository(
    initialKeyframes: List<Keyframe> = MockCatalog.keyframes,
) : LightRepository {
    var lastKeyframes: List<Keyframe> = initialKeyframes
        private set
    var lastEffect: LightEffect? = null
        private set

    override val session: StateFlow<LightSessionState> = MutableStateFlow(
        LightSessionState(
            device = MockCatalog.device,
            matrix = LedMatrix(rows = 2, columns = 4, pixels = emptyList()),
            activeEffect = null,
            liveControl = LiveControl(),
            brightnessLimit = 1f,
            playback = PlaybackState(isPlaying = true, positionMs = 0),
            keyframes = initialKeyframes,
        ),
    )

    override fun applyEffect(effect: LightEffect) {
        lastEffect = effect
    }

    override fun updateLiveControl(control: LiveControl) = Unit

    override fun updateEffectControl(control: LiveControl) = Unit

    override fun updateKeyframes(keyframes: List<Keyframe>) {
        lastKeyframes = keyframes
    }

    override fun saveKeyframePreset(name: String, keyframes: List<Keyframe>) {
        lastKeyframes = keyframes
    }

    override fun deleteKeyframePreset(id: String) = Unit

    override fun togglePlayback() = Unit
}

private class FakeEffectRepository : EffectRepository {
    override val effects: List<LightEffect> = MockCatalog.effects
}

private class FakeSensorRepository : SensorRepository {
    override val modes: List<SensorMode> = MockCatalog.sensorModes
    override val snapshot: StateFlow<SensorSnapshot> = MutableStateFlow(
        SensorSnapshot(
            microphoneLevel = 0.4f,
            gravity = Vector3(0f, 0.5f, 0.8f),
            gyroscope = Vector3(0.1f, 0f, 0.2f),
            shakeIntensity = 0.15f,
        ),
    )

    override fun setMicrophoneEnabled(enabled: Boolean) = Unit
}

private class FakeBluetoothService : BluetoothService {
    private val events = MutableSharedFlow<BluetoothConnectionEvent>(replay = 8)
    override val connectionEvents: Flow<BluetoothConnectionEvent> = events
    override val discoveredDevices: StateFlow<List<BluetoothDeviceCandidate>> = MutableStateFlow(emptyList())
    var lastSendAllBrightness: Float? = null
        private set

    suspend fun emit(event: BluetoothConnectionEvent) {
        events.emit(event)
    }

    override suspend fun scan() = Unit
    override suspend fun connect(deviceAddress: String) = Unit
    override suspend fun disconnect() = Unit
    override suspend fun sendFrame(matrix: LedMatrix) = Unit
    override suspend fun sendAll(color: RgbColor, brightness: Float) {
        lastSendAllBrightness = brightness
    }
}
