package com.example.rgbcontrller.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rgbcontrller.data.mock.AppContainer
import com.example.rgbcontrller.domain.model.AppSettings
import com.example.rgbcontrller.domain.repository.SettingsRepository
import com.example.rgbcontrller.presentation.ui.components.AuroraBackground
import com.example.rgbcontrller.presentation.ui.components.ExpressiveSlider
import com.example.rgbcontrller.presentation.ui.components.PageTitle
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(
    private val settingsRepository: SettingsRepository = AppContainer.settingsRepository,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = settingsRepository.settings

    fun updateDarkMode(value: Boolean) {
        settingsRepository.updateDarkMode(value)
    }

    fun updateDynamicTheme(value: Boolean) {
        settingsRepository.updateDynamicTheme(value)
    }

    fun updateDeveloperMode(value: Boolean) {
        settingsRepository.updateDeveloperMode(value)
    }

    fun updateAnimationSpeed(value: Float) {
        settingsRepository.updateAnimationSpeed(value)
    }

    fun updateDefaultBrightness(value: Float) {
        settingsRepository.updateDefaultBrightness(value)
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    AuroraBackground(modifier.fillMaxSize()) {
        Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        PageTitle(
                            title = "Settings",
                            modifier = Modifier.weight(1f),
                            subtitle = "Personalize theme, motion and controller defaults",
                        )
                    }
                }
                item {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            PreferenceSwitch("深色模式", "Use a darker light-control cockpit", settings.darkMode, viewModel::updateDarkMode)
                            PreferenceSwitch("动态主题", "Follow system Material You color", settings.dynamicTheme, viewModel::updateDynamicTheme)
                            PreferenceSwitch("开发者模式", "Show mock diagnostics and future BLE tools", settings.developerMode, viewModel::updateDeveloperMode)
                            ExpressiveSlider("动画速度", settings.animationSpeed, viewModel::updateAnimationSpeed)
                            ExpressiveSlider("默认亮度", settings.defaultBrightness, viewModel::updateDefaultBrightness)
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
