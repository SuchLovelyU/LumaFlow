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
                    PageTitle("Live Control", "Tune color, brightness, speed and direction")
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
                        ExpressiveSlider("Hue", control.hue / 360f, { value -> viewModel.update { it.copy(hue = value * 360f, color = hueToRgb(value * 360f)) } })
                        ExpressiveSlider("Saturation", control.saturation, { value -> viewModel.update { it.copy(saturation = value) } })
                        ExpressiveSlider("Value", control.value, { value -> viewModel.update { it.copy(value = value) } })
                        ExpressiveSlider("Red", control.color.red / 255f, { value -> viewModel.update { it.copy(color = it.color.copy(red = (value * 255).toInt())) } })
                        ExpressiveSlider("Green", control.color.green / 255f, { value -> viewModel.update { it.copy(color = it.color.copy(green = (value * 255).toInt())) } })
                        ExpressiveSlider("Blue", control.color.blue / 255f, { value -> viewModel.update { it.copy(color = it.color.copy(blue = (value * 255).toInt())) } })
                        ExpressiveSlider("Brightness", control.brightness, { value -> viewModel.update { it.copy(brightness = value) } })
                        ExpressiveSlider("Speed", control.speed, { value -> viewModel.update { it.copy(speed = value) } })
                        DirectionSegmentedControl(selected = control.direction, onSelect = viewModel::updateDirection)
                    }
                }
            }
        }
    }
}

private fun hueToRgb(hue: Float): RgbColor {
    val normalized = ((hue % 360f) + 360f) % 360f
    val x = (1f - kotlin.math.abs((normalized / 60f) % 2f - 1f)) * 255
    return when {
        normalized < 60f -> RgbColor(255, x.toInt(), 0)
        normalized < 120f -> RgbColor(x.toInt(), 255, 0)
        normalized < 180f -> RgbColor(0, 255, x.toInt())
        normalized < 240f -> RgbColor(0, x.toInt(), 255)
        normalized < 300f -> RgbColor(x.toInt(), 0, 255)
        else -> RgbColor(255, 0, x.toInt())
    }
}
