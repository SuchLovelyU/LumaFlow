package com.example.rgbcontrller.presentation.screens.editor

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rgbcontrller.data.mock.AppContainer
import com.example.rgbcontrller.data.mock.KeyframeCodec
import com.example.rgbcontrller.domain.model.Keyframe
import com.example.rgbcontrller.domain.model.KeyframePreset
import com.example.rgbcontrller.domain.model.RgbColor
import com.example.rgbcontrller.domain.repository.LightRepository
import com.example.rgbcontrller.presentation.ui.components.AuroraBackground
import com.example.rgbcontrller.presentation.ui.components.ColorWheelControl
import com.example.rgbcontrller.presentation.ui.components.ExpressiveSlider
import com.example.rgbcontrller.presentation.ui.components.LedMatrixPreview
import com.example.rgbcontrller.presentation.ui.components.PageTitle
import com.example.rgbcontrller.presentation.ui.components.TimelineEditor
import java.io.IOException

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
            brightness = (0.65f + (keyframes.size % 3) * 0.12f).coerceIn(0f, 1f),
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

    fun saveConfig() {
        lightRepository.saveKeyframePreset(defaultPresetName(), keyframes)
    }

    fun loadPreset(preset: KeyframePreset) {
        keyframes = preset.keyframes
        selectedId = keyframes.firstOrNull()?.id
        lightRepository.updateKeyframes(keyframes)
    }

    fun deletePreset(id: String) {
        lightRepository.deleteKeyframePreset(id)
    }

    fun exportConfig(): String {
        return KeyframeCodec.encode(keyframes)
    }

    fun importConfig(encoded: String): Boolean {
        val imported = KeyframeCodec.decode(encoded)
        if (imported.isEmpty()) return false
        keyframes = imported
        selectedId = imported.firstOrNull()?.id
        lightRepository.updateKeyframes(keyframes)
        return true
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

    fun updateSelectedColor(color: RgbColor) {
        updateSelected { it.copy(color = color) }
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

    private fun defaultPresetName(): String {
        val existing = session.value.keyframePresets.map { it.name }.toSet()
        var number = existing.size + 1
        while ("Preset $number" in existing) {
            number += 1
        }
        return "Preset $number"
    }
}

@Composable
fun EditorScreen(
    modifier: Modifier = Modifier,
    viewModel: EditorViewModel = viewModel(),
) {
    val state by viewModel.session.collectAsState()
    val context = LocalContext.current
    var showColorPicker by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            val saved = context.writeConfig(uri, viewModel.exportConfig())
            context.showEditorToast(if (saved) "Configuration exported" else "Export failed")
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val imported = context.readConfig(uri)?.let(viewModel::importConfig) == true
            context.showEditorToast(if (imported) "Configuration imported" else "Import failed")
        }
    }
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
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            EditorToolButton(onClick = {
                                viewModel.saveConfig()
                                context.showEditorToast("Preset saved")
                            }) {
                                Icon(Icons.Filled.Save, contentDescription = "Save configuration")
                            }
                            EditorToolButton(onClick = { exportLauncher.launch("lumaflow-keyframes.txt") }) {
                                Icon(Icons.Filled.FileDownload, contentDescription = "Export configuration")
                            }
                            EditorToolButton(onClick = { importLauncher.launch(arrayOf("text/*", "application/octet-stream")) }) {
                                Icon(Icons.Filled.FileUpload, contentDescription = "Import configuration")
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
                    PresetPicker(
                        presets = state.keyframePresets,
                        onSelect = viewModel::loadPreset,
                        onDelete = viewModel::deletePreset,
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
                            ExpressiveSlider(
                                label = "Selected brightness",
                                value = selected.brightness,
                                onValueChange = viewModel::updateSelectedBrightness,
                            )
                            ExpressiveSlider("Red", selected.color.red / 255f, viewModel::updateSelectedRed)
                            ExpressiveSlider("Green", selected.color.green / 255f, viewModel::updateSelectedGreen)
                            ExpressiveSlider("Blue", selected.color.blue / 255f, viewModel::updateSelectedBlue)
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { showColorPicker = true },
                            ) {
                                Icon(Icons.Filled.ColorLens, contentDescription = null)
                                Text("Pick color")
                            }
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
    val selected = viewModel.keyframes.firstOrNull { it.id == viewModel.selectedId }
    if (showColorPicker && selected != null) {
        KeyframeColorPickerDialog(
            color = selected.color,
            onDismiss = { showColorPicker = false },
            onColorChange = viewModel::updateSelectedColor,
        )
    }
}

@Composable
private fun PresetPicker(
    presets: List<KeyframePreset>,
    onSelect: (KeyframePreset) -> Unit,
    onDelete: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Presets", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            Text("${presets.size} saved", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (presets.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(72.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("No saved presets", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(presets, key = { it.id }) { preset ->
                    Card(
                        onClick = { onSelect(preset) },
                        modifier = Modifier.size(width = 156.dp, height = 104.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.SpaceBetween) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(preset.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                IconButton(
                                    modifier = Modifier.size(32.dp),
                                    onClick = { onDelete(preset.id) },
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete preset")
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                preset.keyframes.take(5).forEach { keyframe ->
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .height(22.dp)
                                            .background(keyframe.color.toComposeColor(), CircleShape),
                                    )
                                }
                            }
                            Text("${preset.keyframes.size} frames", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyframeColorPickerDialog(
    color: RgbColor,
    onDismiss: () -> Unit,
    onColorChange: (RgbColor) -> Unit,
) {
    val hsv = remember(color) { color.toHsv() }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        },
        title = { Text("Pick color") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .background(color.toComposeColor(), CircleShape),
                )
                ColorWheelControl(
                    colors = listOf(RgbColor.Coral, RgbColor.Amber, RgbColor.Green, RgbColor.Cyan, RgbColor.Violet, RgbColor.Pink, RgbColor.Coral),
                    hue = hsv.hue,
                    saturation = hsv.saturation,
                    onColorSelected = { hue, saturation ->
                        onColorChange(hsvToRgb(hue, saturation, hsv.value))
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                )
                ExpressiveSlider("Red", color.red / 255f, { value ->
                    onColorChange(color.copy(red = (value * 255).toInt().coerceIn(0, 255)))
                })
                ExpressiveSlider("Green", color.green / 255f, { value ->
                    onColorChange(color.copy(green = (value * 255).toInt().coerceIn(0, 255)))
                })
                ExpressiveSlider("Blue", color.blue / 255f, { value ->
                    onColorChange(color.copy(blue = (value * 255).toInt().coerceIn(0, 255)))
                })
            }
        },
    )
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

private data class HsvColor(
    val hue: Float,
    val saturation: Float,
    val value: Float,
)

private fun RgbColor.toHsv(): HsvColor {
    val red = red / 255f
    val green = green / 255f
    val blue = blue / 255f
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val delta = max - min
    val hue = when {
        delta == 0f -> 0f
        max == red -> (60f * ((green - blue) / delta)).mod(360f)
        max == green -> 60f * ((blue - red) / delta + 2f)
        else -> 60f * ((red - green) / delta + 4f)
    }
    val saturation = if (max == 0f) 0f else delta / max
    return HsvColor(hue, saturation, max)
}

private fun hsvToRgb(hue: Float, saturation: Float, value: Float): RgbColor {
    val normalized = ((hue % 360f) + 360f) % 360f
    val c = value.coerceIn(0f, 1f) * saturation.coerceIn(0f, 1f)
    val x = c * (1f - kotlin.math.abs((normalized / 60f) % 2f - 1f))
    val m = value.coerceIn(0f, 1f) - c
    val (r, g, b) = when {
        normalized < 60f -> Triple(c, x, 0f)
        normalized < 120f -> Triple(x, c, 0f)
        normalized < 180f -> Triple(0f, c, x)
        normalized < 240f -> Triple(0f, x, c)
        normalized < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return RgbColor(
        red = ((r + m) * 255).toInt().coerceIn(0, 255),
        green = ((g + m) * 255).toInt().coerceIn(0, 255),
        blue = ((b + m) * 255).toInt().coerceIn(0, 255),
    )
}

private fun Context.writeConfig(uri: Uri, content: String): Boolean {
    return try {
        contentResolver.openOutputStream(uri)?.use { output ->
            output.write(content.toByteArray(Charsets.UTF_8))
        } != null
    } catch (_: IOException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

private fun Context.readConfig(uri: Uri): String? {
    return try {
        contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes().toString(Charsets.UTF_8)
        }
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    }
}

private fun Context.showEditorToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
