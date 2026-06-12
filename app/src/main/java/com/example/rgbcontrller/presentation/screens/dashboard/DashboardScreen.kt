package com.example.rgbcontrller.presentation.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rgbcontrller.data.mock.AppContainer
import com.example.rgbcontrller.data.mock.MockCatalog
import com.example.rgbcontrller.domain.model.LightEffect
import com.example.rgbcontrller.domain.model.LightSessionState
import com.example.rgbcontrller.domain.model.LiveControl
import com.example.rgbcontrller.domain.model.SensorMode
import com.example.rgbcontrller.domain.model.SensorModeType
import com.example.rgbcontrller.domain.model.SensorSnapshot
import com.example.rgbcontrller.domain.repository.EffectRepository
import com.example.rgbcontrller.domain.repository.LightRepository
import com.example.rgbcontrller.domain.repository.SensorRepository
import com.example.rgbcontrller.presentation.ui.components.AuroraBackground
import com.example.rgbcontrller.presentation.ui.components.DeviceStatusHeader
import com.example.rgbcontrller.presentation.ui.components.ExpressiveSlider
import com.example.rgbcontrller.presentation.ui.components.LedMatrixPreview
import com.example.rgbcontrller.presentation.ui.components.PageTitle
import com.example.rgbcontrller.presentation.ui.components.SceneShortcutCard
import com.example.rgbcontrller.presentation.ui.components.SensorModeCard
import kotlinx.coroutines.flow.StateFlow

class DashboardViewModel(
    private val lightRepository: LightRepository = AppContainer.lightRepository,
    private val effectRepository: EffectRepository = AppContainer.effectRepository,
    private val sensorRepository: SensorRepository = AppContainer.sensorRepository,
) : ViewModel() {
    val uiState: StateFlow<LightSessionState> = lightRepository.session
    val sensorSnapshot: StateFlow<SensorSnapshot> = sensorRepository.snapshot
    val effects: List<LightEffect> = effectRepository.effects
    val sensorModes: List<SensorMode> = sensorRepository.modes

    private val defaultEffect = MockCatalog.effects.first { it.id == "aurora" }
    var selectedEffect by mutableStateOf(defaultEffect)
        private set
    var activeSensorModeId by mutableStateOf<String?>(null)
        private set

    fun activateHomeDefault() {
        lightRepository.applyEffect(selectedEffect)
    }

    fun applyShortcut(effect: LightEffect) {
        selectedEffect = effect
        activeSensorModeId = null
        lightRepository.applyEffect(effect)
    }

    fun activateSensorMode(mode: SensorMode) {
        val effect = effectRepository.effects.firstOrNull { it.id == mode.effectId() } ?: return
        selectedEffect = effect
        activeSensorModeId = mode.id
        lightRepository.applyEffect(effect)
    }

    fun updateEffectControl(transform: (LiveControl) -> LiveControl) {
        lightRepository.updateEffectControl(transform(uiState.value.liveControl))
    }

    private fun SensorMode.effectId(): String = when (type) {
        SensorModeType.Music -> "music"
        SensorModeType.Gravity -> "gravity"
        SensorModeType.Gyroscope -> "direction"
        SensorModeType.Shake -> "shake"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenDevice: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snapshot by viewModel.sensorSnapshot.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.activateHomeDefault()
    }

    AuroraBackground(modifier.fillMaxSize()) {
        Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent) { padding ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(span = { GridItemSpan(2) }) {
                    Column {
                        PageTitle(title = "LumaFlow", subtitle = "Home, effects, and sensors in one control surface")
                        Spacer(Modifier.height(16.dp))
                        DeviceStatusHeader(state.device, onClick = onOpenDevice)
                        Spacer(Modifier.height(16.dp))
                        LedMatrixPreview(state.matrix, title = state.activeEffect?.name ?: "Home preview")
                    }
                }

                if (state.activeEffect?.id == "music" || viewModel.selectedEffect.id == "music") {
                    item(span = { GridItemSpan(2) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            EffectSpeedSlider(state.liveControl.speed, viewModel::updateEffectControl)
                            ExpressiveSlider(
                                label = "Music threshold",
                                value = state.liveControl.musicThreshold,
                                onValueChange = { value ->
                                    viewModel.updateEffectControl { it.copy(musicThreshold = value.coerceIn(0f, 0.95f)) }
                                },
                            )
                        }
                    }
                }

                if (state.activeEffect?.id == "gravity" || viewModel.selectedEffect.id == "gravity") {
                    item(span = { GridItemSpan(2) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ExpressiveSlider(
                                label = "Fluid amount",
                                value = state.liveControl.fluidLevel,
                                onValueChange = { value ->
                                    viewModel.updateEffectControl { it.copy(fluidLevel = value.coerceIn(0.05f, 0.95f)) }
                                },
                            )
                            ExpressiveSlider(
                                label = "Fluid density",
                                value = state.liveControl.fluidDensity,
                                onValueChange = { value ->
                                    viewModel.updateEffectControl { it.copy(fluidDensity = value.coerceIn(0f, 1f)) }
                                },
                            )
                        }
                    }
                }

                if (viewModel.selectedEffect.usesSpeedControl() && viewModel.selectedEffect.id != "music") {
                    item(span = { GridItemSpan(2) }) {
                        EffectSpeedSlider(state.liveControl.speed, viewModel::updateEffectControl)
                    }
                }

                item(span = { GridItemSpan(2) }) {
                    PageTitle(title = "Sensor modes")
                }
                items(viewModel.sensorModes, key = { it.id }, span = { GridItemSpan(2) }) { mode ->
                    SensorModeCard(
                        mode = mode,
                        snapshot = snapshot,
                        isActive = viewModel.activeSensorModeId == mode.id,
                        onClick = { viewModel.activateSensorMode(mode) },
                    )
                }

                item(span = { GridItemSpan(2) }) {
                    PageTitle(title = "Effects")
                }
                items(viewModel.effects, key = { it.id }) { effect ->
                    SceneShortcutCard(
                        effect = effect,
                        isActive = viewModel.selectedEffect.id == effect.id && viewModel.activeSensorModeId == null,
                        onClick = { viewModel.applyShortcut(effect) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EffectSpeedSlider(
    speed: Float,
    onUpdate: ((LiveControl) -> LiveControl) -> Unit,
) {
    ExpressiveSlider(
        label = "Effect speed",
        value = speed,
        onValueChange = { value -> onUpdate { it.copy(speed = value.coerceIn(0f, 1f)) } },
    )
}

private fun LightEffect.usesSpeedControl(): Boolean {
    return id in setOf("breath", "blink", "run", "rainbow", "wave", "meteor", "flame", "aurora", "neon", "music", "direction", "ambient")
}
