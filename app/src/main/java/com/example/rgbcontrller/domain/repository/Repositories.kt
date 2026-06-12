package com.example.rgbcontrller.domain.repository

import com.example.rgbcontrller.domain.model.DeviceInfo
import com.example.rgbcontrller.domain.model.AppSettings
import com.example.rgbcontrller.domain.model.Keyframe
import com.example.rgbcontrller.domain.model.LightEffect
import com.example.rgbcontrller.domain.model.LightSessionState
import com.example.rgbcontrller.domain.model.LiveControl
import com.example.rgbcontrller.domain.model.RgbColor
import com.example.rgbcontrller.domain.model.SensorMode
import com.example.rgbcontrller.domain.model.SensorSnapshot
import com.example.rgbcontrller.data.bluetooth.BluetoothDeviceCandidate
import kotlinx.coroutines.flow.StateFlow

interface LightRepository {
    val session: StateFlow<LightSessionState>
    fun applyEffect(effect: LightEffect)
    fun updateLiveControl(control: LiveControl)
    fun updateEffectControl(control: LiveControl)
    fun updateKeyframes(keyframes: List<Keyframe>)
    fun saveKeyframePreset(name: String, keyframes: List<Keyframe>)
    fun deleteKeyframePreset(id: String)
    fun togglePlayback()
}

interface DeviceRepository {
    val device: StateFlow<DeviceInfo>
    val discoveredDevices: StateFlow<List<BluetoothDeviceCandidate>>
    val statusMessage: StateFlow<String?>
    fun scan()
    fun connect(deviceAddress: String)
    fun disconnect()
    fun sendAll(color: RgbColor, brightness: Float)
}

interface EffectRepository {
    val effects: List<LightEffect>
}

interface SensorRepository {
    val modes: List<SensorMode>
    val snapshot: StateFlow<SensorSnapshot>
    fun setMicrophoneEnabled(enabled: Boolean)
}

interface SettingsRepository {
    val settings: StateFlow<AppSettings>
    fun updateDarkMode(value: Boolean)
    fun updateDynamicTheme(value: Boolean)
    fun updateDeveloperMode(value: Boolean)
    fun updateAnimationSpeed(value: Float)
    fun updateDefaultBrightness(value: Float)
    fun updateMasterBrightnessLimit(value: Float)
}
