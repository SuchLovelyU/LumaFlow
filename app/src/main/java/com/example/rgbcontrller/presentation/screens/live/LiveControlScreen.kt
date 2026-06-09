package com.example.rgbcontrller.presentation.screens.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rgbcontrller.data.mock.AppContainer
import com.example.rgbcontrller.domain.model.DirectionMode
import com.example.rgbcontrller.domain.model.LiveControl
import com.example.rgbcontrller.domain.model.RgbColor
import com.example.rgbcontrller.domain.repository.LightRepository
import com.example.rgbcontrller.presentation.ui.components.AuroraBackground
import com.example.rgbcontrller.presentation.ui.components.ColorWheelControl
import com.example.rgbcontrller.presentation.ui.components.DirectionSegmentedControl
import com.example.rgbcontrller.presentation.ui.components.ExpressiveSlider
import com.example.rgbcontrller.presentation.ui.components.LedMatrixPreview
import com.example.rgbcontrller.presentation.ui.components.PageTitle

class LiveControlViewModel(
    private val lightRepository: LightRepository = AppContainer.lightRepository,
) : ViewModel() {
    val session = lightRepository.session

    fun update(transform: (LiveControl) -> LiveControl) {
        lightRepository.updateLiveControl(transform(session.value.liveControl))
    }

    fun updateDirection(direction: DirectionMode) {
        update { it.copy(direction = direction) }
    }

    fun updateHsv(hue: Float = session.value.liveControl.hue, saturation: Float = session.value.liveControl.saturation, value: Float = session.value.liveControl.value) {
        update {
            val nextHue = hue.coerceIn(0f, 360f)
            val nextSaturation = saturation.coerceIn(0f, 1f)
            val nextValue = value.coerceIn(0f, 1f)
            it.copy(
                hue = nextHue,
                saturation = nextSaturation,
                value = nextValue,
                color = hsvToRgb(nextHue, nextSaturation, nextValue),
            )
        }
    }
}

@Composable
fun LiveControlScreen(
    modifier: Modifier = Modifier,
    viewModel: LiveControlViewModel = viewModel(),
) {
    val state by viewModel.session.collectAsState()
    val control = state.liveControl
    AuroraBackground(modifier.fillMaxSize()) {
        Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item {
                    PageTitle(title = "Live Control", subtitle = "Tune color, brightness, speed and direction")
                }
                item {
                    LedMatrixPreview(state.matrix, title = "Live preview")
                }
                item {
                    ColorWheelControl(
                        colors = listOf(RgbColor.Coral, RgbColor.Amber, RgbColor.Green, RgbColor.Cyan, RgbColor.Violet, RgbColor.Pink, RgbColor.Coral),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 52.dp),
                    )
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ExpressiveSlider("Hue", control.hue / 360f, { value -> viewModel.updateHsv(hue = value * 360f) })
                        ExpressiveSlider("Saturation", control.saturation, { value -> viewModel.updateHsv(saturation = value) })
                        ExpressiveSlider("Value", control.value, { value -> viewModel.updateHsv(value = value) })
                        ExpressiveSlider("Red", control.color.red / 255f, { value -> viewModel.update { it.copy(color = it.color.copy(red = (value * 255).toInt().coerceIn(0, 255))) } })
                        ExpressiveSlider("Green", control.color.green / 255f, { value -> viewModel.update { it.copy(color = it.color.copy(green = (value * 255).toInt().coerceIn(0, 255))) } })
                        ExpressiveSlider("Blue", control.color.blue / 255f, { value -> viewModel.update { it.copy(color = it.color.copy(blue = (value * 255).toInt().coerceIn(0, 255))) } })
                        ExpressiveSlider("Brightness", control.brightness, { value -> viewModel.update { it.copy(brightness = value.coerceIn(0f, 1f)) } })
                        ExpressiveSlider("Speed", control.speed, { value -> viewModel.update { it.copy(speed = value.coerceIn(0f, 1f)) } })
                        DirectionSegmentedControl(selected = control.direction, onSelect = viewModel::updateDirection)
                    }
                }
            }
        }
    }
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
