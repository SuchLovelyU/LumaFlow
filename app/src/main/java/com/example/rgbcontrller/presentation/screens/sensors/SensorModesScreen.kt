package com.example.rgbcontrller.presentation.screens.sensors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rgbcontrller.data.mock.AppContainer
import com.example.rgbcontrller.domain.model.SensorMode
import com.example.rgbcontrller.domain.model.SensorModeType
import com.example.rgbcontrller.domain.repository.EffectRepository
import com.example.rgbcontrller.domain.repository.LightRepository
import com.example.rgbcontrller.domain.repository.SensorRepository
import com.example.rgbcontrller.presentation.ui.components.AuroraBackground
import com.example.rgbcontrller.presentation.ui.components.PageTitle
import com.example.rgbcontrller.presentation.ui.components.SensorModeCard
import com.example.rgbcontrller.presentation.ui.components.SensorPreview

class SensorModesViewModel(
    private val sensorRepository: SensorRepository = AppContainer.sensorRepository,
    private val effectRepository: EffectRepository = AppContainer.effectRepository,
    private val lightRepository: LightRepository = AppContainer.lightRepository,
) : ViewModel() {
    val modes: List<SensorMode> = sensorRepository.modes
    val snapshot = sensorRepository.snapshot
    var activeModeId by mutableStateOf<String?>(null)
        private set
    var selectedAction by mutableStateOf<String?>(modes.firstOrNull()?.actions?.firstOrNull())
        private set

    fun modeById(id: String): SensorMode? = modes.firstOrNull { it.id == id }

    fun selectAction(action: String) {
        selectedAction = action
    }

    fun activateMode(modeId: String): Boolean {
        val mode = modeById(modeId) ?: return false
        val effectId = mode.effectId()
        val effect = effectRepository.effects.firstOrNull { it.id == effectId } ?: return false
        if (selectedAction !in mode.actions) {
            selectedAction = mode.actions.firstOrNull()
        }
        activeModeId = mode.id
        lightRepository.applyEffect(effect)
        return true
    }

    private fun SensorMode.effectId(): String = when (type) {
        SensorModeType.Music -> "music"
        SensorModeType.Gravity -> "gravity"
        SensorModeType.Gyroscope -> "direction"
        SensorModeType.Shake -> "shake"
    }
}

@Composable
fun SensorModesScreen(
    onOpenMode: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SensorModesViewModel = viewModel(),
) {
    val snapshot by viewModel.snapshot.collectAsState()
    AuroraBackground(modifier.fillMaxSize()) {
        Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    PageTitle(title = "Sensor Modes", subtitle = "Phone sensors become expressive light controls")
                }
                items(viewModel.modes, key = { it.id }) { mode ->
                    SensorModeCard(mode, snapshot, onClick = { onOpenMode(mode.id) })
                }
            }
        }
    }
}

@Composable
fun SensorModeDetailScreen(
    modeId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SensorModesViewModel = viewModel(),
) {
    val snapshot by viewModel.snapshot.collectAsState()
    val mode = viewModel.modeById(modeId)
    val isActive = viewModel.activeModeId == modeId
    AuroraBackground(modifier.fillMaxSize()) {
        Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        PageTitle(
                            title = mode?.title ?: "Sensor",
                            modifier = Modifier.weight(1f),
                            subtitle = mode?.description,
                        )
                    }
                }
                item {
                    SensorPreview(modeId, snapshot, Modifier.fillMaxWidth().height(220.dp))
                }
                mode?.let { sensorMode ->
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            sensorMode.actions.forEach { action ->
                                FilterChip(
                                    selected = viewModel.selectedAction == action,
                                    onClick = { viewModel.selectAction(action) },
                                    label = { Text(action) },
                                )
                            }
                        }
                    }
                    item {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { viewModel.activateMode(sensorMode.id) },
                        ) {
                            Text(if (isActive) "Mode active" else "Activate mode")
                        }
                    }
                }
            }
        }
    }
}
