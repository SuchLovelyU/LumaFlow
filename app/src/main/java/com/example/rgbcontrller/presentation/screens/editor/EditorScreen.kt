package com.example.rgbcontrller.presentation.screens.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
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
import com.example.rgbcontrller.data.mock.MockCatalog
import com.example.rgbcontrller.domain.model.Keyframe
import com.example.rgbcontrller.domain.model.RgbColor
import com.example.rgbcontrller.domain.repository.LightRepository
import com.example.rgbcontrller.presentation.ui.components.AuroraBackground
import com.example.rgbcontrller.presentation.ui.components.ExpressiveSlider
import com.example.rgbcontrller.presentation.ui.components.LedMatrixPreview
import com.example.rgbcontrller.presentation.ui.components.PageTitle
import com.example.rgbcontrller.presentation.ui.components.TimelineEditor

class EditorViewModel(
    private val lightRepository: LightRepository = AppContainer.lightRepository,
) : ViewModel() {
    val session = lightRepository.session
    var keyframes by mutableStateOf(MockCatalog.keyframes)
        private set
    var selectedId by mutableStateOf(keyframes.firstOrNull()?.id)
        private set

    fun select(id: String) {
        selectedId = id
    }

    fun addKeyframe() {
        val palette = listOf(RgbColor.Cyan, RgbColor.Violet, RgbColor.Pink, RgbColor.Green, RgbColor.Coral, RgbColor.Amber)
        val next = Keyframe(
            id = "kf-${keyframes.size + 1}",
            color = palette[keyframes.size % palette.size],
            brightness = 0.65f + (keyframes.size % 3) * 0.12f,
            durationMs = 420 + keyframes.size * 40,
        )
        keyframes = keyframes + next
        selectedId = next.id
        lightRepository.updateKeyframes(keyframes)
    }

    fun deleteSelected() {
        val id = selectedId ?: return
        keyframes = keyframes.filterNot { it.id == id }
        selectedId = keyframes.firstOrNull()?.id
        lightRepository.updateKeyframes(keyframes)
    }

    fun togglePlayback() {
        lightRepository.togglePlayback()
    }
}

@Composable
fun EditorScreen(
    modifier: Modifier = Modifier,
    viewModel: EditorViewModel = viewModel(),
) {
    val state by viewModel.session.collectAsState()
    AuroraBackground(modifier.fillMaxSize()) {
        Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item {
                    PageTitle("Editor", "Build custom RGB animations with keyframes")
                }
                item {
                    LedMatrixPreview(state.matrix, title = "Realtime preview")
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(modifier = Modifier.weight(1f), onClick = viewModel::togglePlayback) {
                            Text(if (state.playback.isPlaying) "Pause" else "Play")
                        }
                        Button(modifier = Modifier.weight(1f), onClick = viewModel::addKeyframe) {
                            Text("Add")
                        }
                        OutlinedButton(modifier = Modifier.weight(1f), onClick = viewModel::deleteSelected) {
                            Text("Delete")
                        }
                    }
                }
                item {
                    TimelineEditor(
                        keyframes = viewModel.keyframes,
                        selectedId = viewModel.selectedId,
                        onSelect = viewModel::select,
                    )
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val selected = viewModel.keyframes.firstOrNull { it.id == viewModel.selectedId }
                        ExpressiveSlider("Selected brightness", selected?.brightness ?: 0f, {})
                        ExpressiveSlider("Duration scale", ((selected?.durationMs ?: 0) / 1000f).coerceIn(0f, 1f), {})
                    }
                }
            }
        }
    }
}
