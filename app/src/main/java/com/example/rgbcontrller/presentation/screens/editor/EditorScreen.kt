package com.example.rgbcontrller.presentation.screens.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rgbcontrller.data.mock.AppContainer
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
    var keyframes by mutableStateOf(lightRepository.session.value.keyframes)
        private set
    var selectedId by mutableStateOf(keyframes.firstOrNull()?.id)
        private set

    fun select(id: String) {
        selectedId = id
    }

    fun addKeyframe() {
        val palette = listOf(RgbColor.Cyan, RgbColor.Violet, RgbColor.Pink, RgbColor.Green, RgbColor.Coral, RgbColor.Amber)
        val next = Keyframe(
            id = nextKeyframeId(),
            color = palette[keyframes.size % palette.size],
            brightness = 0.65f + (keyframes.size % 3) * 0.12f,
            durationMs = 420 + keyframes.size * 40,
        )
        keyframes = keyframes + next
        selectedId = next.id
        lightRepository.updateKeyframes(keyframes)
    }

    fun duplicateSelected() {
        val selectedIndex = selectedIndex()
        if (selectedIndex < 0) return
        val source = keyframes[selectedIndex]
        val copy = source.copy(id = nextKeyframeId())
        keyframes = keyframes.toMutableList().apply {
            add(selectedIndex + 1, copy)
        }
        selectedId = copy.id
        lightRepository.updateKeyframes(keyframes)
    }

    fun deleteSelected() {
        val id = selectedId ?: return
        val removedIndex = selectedIndex()
        keyframes = keyframes.filterNot { it.id == id }
        selectedId = when {
            keyframes.isEmpty() -> null
            removedIndex in keyframes.indices -> keyframes[removedIndex].id
            else -> keyframes.last().id
        }
        lightRepository.updateKeyframes(keyframes)
    }

    fun moveSelectedLeft() {
        moveSelected(-1)
    }

    fun moveSelectedRight() {
        moveSelected(1)
    }

    fun togglePlayback() {
        lightRepository.togglePlayback()
    }

    fun activateEditorDefault() {
        lightRepository.updateKeyframes(keyframes)
    }

    fun updateSelectedBrightness(value: Float) {
        updateSelected { it.copy(brightness = value.coerceIn(0f, 1f)) }
    }

    fun updateSelectedDuration(scale: Float) {
        val durationMs = (200 + scale.coerceIn(0f, 1f) * 1_800).toInt()
        updateSelected { it.copy(durationMs = durationMs) }
    }

    fun updateSelectedRed(value: Float) {
        updateSelectedColor { it.copy(red = (value * 255).toInt().coerceIn(0, 255)) }
    }

    fun updateSelectedGreen(value: Float) {
        updateSelectedColor { it.copy(green = (value * 255).toInt().coerceIn(0, 255)) }
    }

    fun updateSelectedBlue(value: Float) {
        updateSelectedColor { it.copy(blue = (value * 255).toInt().coerceIn(0, 255)) }
    }

    private fun updateSelectedColor(transform: (RgbColor) -> RgbColor) {
        updateSelected { it.copy(color = transform(it.color)) }
    }

    private fun updateSelected(transform: (Keyframe) -> Keyframe) {
        val id = selectedId ?: return
        keyframes = keyframes.map { keyframe ->
            if (keyframe.id == id) transform(keyframe) else keyframe
        }
        lightRepository.updateKeyframes(keyframes)
    }

    private fun moveSelected(offset: Int) {
        val from = selectedIndex()
        if (from < 0) return
        val to = (from + offset).coerceIn(0, keyframes.lastIndex)
        if (from == to) return
        keyframes = keyframes.toMutableList().apply {
            add(to, removeAt(from))
        }
        lightRepository.updateKeyframes(keyframes)
    }

    private fun selectedIndex(): Int = keyframes.indexOfFirst { it.id == selectedId }

    private fun nextKeyframeId(): String {
        val usedIds = keyframes.map { it.id }.toSet()
        var nextNumber = keyframes
            .mapNotNull { it.id.removePrefix("kf-").toIntOrNull() }
            .maxOrNull()
            ?.plus(1) ?: 1
        while ("kf-$nextNumber" in usedIds) {
            nextNumber += 1
        }
        return "kf-$nextNumber"
    }
}

@Composable
fun EditorScreen(
    modifier: Modifier = Modifier,
    viewModel: EditorViewModel = viewModel(),
) {
    val state by viewModel.session.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.activateEditorDefault()
    }
    AuroraBackground(modifier.fillMaxSize()) {
        Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item {
                    PageTitle(title = "Editor", subtitle = "Build custom RGB animations with keyframes")
                }
                item {
                    LedMatrixPreview(state.matrix, title = "Realtime preview")
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            EditorToolButton(onClick = viewModel::togglePlayback, emphasized = true) {
                                Icon(
                                    if (state.playback.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (state.playback.isPlaying) "Pause" else "Play",
                                )
                            }
                            EditorToolButton(onClick = viewModel::addKeyframe, emphasized = true) {
                                Icon(Icons.Filled.Add, contentDescription = "Add keyframe")
                            }
                            EditorToolButton(
                                enabled = viewModel.selectedId != null,
                                onClick = viewModel::duplicateSelected,
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy keyframe")
                            }
                        }
                        val selectedIndex = viewModel.keyframes.indexOfFirst { it.id == viewModel.selectedId }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            EditorToolButton(
                                enabled = selectedIndex > 0,
                                onClick = viewModel::moveSelectedLeft,
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Move keyframe left")
                            }
                            EditorToolButton(
                                enabled = selectedIndex >= 0 && selectedIndex < viewModel.keyframes.lastIndex,
                                onClick = viewModel::moveSelectedRight,
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Move keyframe right")
                            }
                            EditorToolButton(
                                enabled = viewModel.selectedId != null,
                                onClick = viewModel::deleteSelected,
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete keyframe")
                            }
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
                        if (selected == null) {
                            Text(
                                "No frame selected",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            ExpressiveSlider("Selected brightness", selected.brightness, viewModel::updateSelectedBrightness)
                            ExpressiveSlider("Red", selected.color.red / 255f, viewModel::updateSelectedRed)
                            ExpressiveSlider("Green", selected.color.green / 255f, viewModel::updateSelectedGreen)
                            ExpressiveSlider("Blue", selected.color.blue / 255f, viewModel::updateSelectedBlue)
                            ExpressiveSlider(
                                "Duration",
                                ((selected.durationMs - 200) / 1800f).coerceIn(0f, 1f),
                                viewModel::updateSelectedDuration,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorToolButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasized: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(64.dp),
        shape = CircleShape,
        color = if (emphasized) colorScheme.primaryContainer else colorScheme.surface,
        contentColor = when {
            !enabled -> colorScheme.onSurface.copy(alpha = 0.32f)
            emphasized -> colorScheme.onPrimaryContainer
            else -> colorScheme.onSurface
        },
        border = if (emphasized) null else BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.34f)),
        shadowElevation = 2.dp,
        tonalElevation = if (emphasized) 1.dp else 0.dp,
        content = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                content()
            }
        },
    )
}
