package com.example.rgbcontrller.presentation.screens.sensors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rgbcontrller.data.mock.AppContainer
import com.example.rgbcontrller.domain.model.SensorMode
import com.example.rgbcontrller.domain.repository.SensorRepository
import com.example.rgbcontrller.presentation.ui.components.AuroraBackground
import com.example.rgbcontrller.presentation.ui.components.PageTitle
import com.example.rgbcontrller.presentation.ui.components.SensorModeCard
import com.example.rgbcontrller.presentation.ui.components.SensorPreview

class SensorModesViewModel(
    private val sensorRepository: SensorRepository = AppContainer.sensorRepository,
) : ViewModel() {
    val modes: List<SensorMode> = sensorRepository.modes
    val snapshot = sensorRepository.snapshot

    fun modeById(id: String): SensorMode? = modes.firstOrNull { it.id == id }
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
                    PageTitle("Sensor Modes", "Phone sensors become expressive light controls")
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
    modifier: Modifier = Modifier,
    viewModel: SensorModesViewModel = viewModel(),
) {
    val snapshot by viewModel.snapshot.collectAsState()
    val mode = viewModel.modeById(modeId)
    AuroraBackground(modifier.fillMaxSize()) {
        Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                PageTitle(mode?.title ?: "Sensor", mode?.description)
                SensorPreview(modeId, snapshot, Modifier.fillMaxWidth().height(220.dp))
                mode?.actions?.forEach { action ->
                    AssistChip(onClick = {}, label = { Text(action) })
                }
            }
        }
    }
}
