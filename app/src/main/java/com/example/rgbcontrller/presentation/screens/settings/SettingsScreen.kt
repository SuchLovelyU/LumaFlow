package com.example.rgbcontrller.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rgbcontrller.presentation.ui.components.AuroraBackground
import com.example.rgbcontrller.presentation.ui.components.ExpressiveSlider
import com.example.rgbcontrller.presentation.ui.components.PageTitle

class SettingsViewModel : ViewModel() {
    var darkMode by mutableStateOf(false)
        private set
    var dynamicTheme by mutableStateOf(true)
        private set
    var developerMode by mutableStateOf(false)
        private set
    var animationSpeed by mutableFloatStateOf(0.72f)
        private set
    var defaultBrightness by mutableFloatStateOf(0.78f)
        private set

    fun updateDarkMode(value: Boolean) {
        darkMode = value
    }

    fun updateDynamicTheme(value: Boolean) {
        dynamicTheme = value
    }

    fun updateDeveloperMode(value: Boolean) {
        developerMode = value
    }

    fun updateAnimationSpeed(value: Float) {
        animationSpeed = value
    }

    fun updateDefaultBrightness(value: Float) {
        defaultBrightness = value
    }
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(),
) {
    AuroraBackground(modifier.fillMaxSize()) {
        Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    PageTitle("Settings", "Personalize theme, motion and mock controls")
                }
                item {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            PreferenceSwitch("深色模式", "Use a darker light-control cockpit", viewModel.darkMode, viewModel::updateDarkMode)
                            PreferenceSwitch("动态主题", "Follow system Material You color", viewModel.dynamicTheme, viewModel::updateDynamicTheme)
                            PreferenceSwitch("开发者模式", "Show mock diagnostics and future BLE tools", viewModel.developerMode, viewModel::updateDeveloperMode)
                            ExpressiveSlider("动画速度", viewModel.animationSpeed, viewModel::updateAnimationSpeed)
                            ExpressiveSlider("默认亮度", viewModel.defaultBrightness, viewModel::updateDefaultBrightness)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferenceSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
