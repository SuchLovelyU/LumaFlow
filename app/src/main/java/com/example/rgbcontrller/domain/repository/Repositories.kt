package com.example.rgbcontrller.domain.repository

import com.example.rgbcontrller.domain.model.DeviceInfo
import com.example.rgbcontrller.domain.model.Keyframe
import com.example.rgbcontrller.domain.model.LightEffect
import com.example.rgbcontrller.domain.model.LightSessionState
import com.example.rgbcontrller.domain.model.LiveControl
import com.example.rgbcontrller.domain.model.SensorMode
import com.example.rgbcontrller.domain.model.SensorSnapshot
import kotlinx.coroutines.flow.StateFlow

interface LightRepository {
    val session: StateFlow<LightSessionState>
    fun applyEffect(effect: LightEffect)
    fun updateLiveControl(control: LiveControl)
    fun updateKeyframes(keyframes: List<Keyframe>)
    fun togglePlayback()
}

interface DeviceRepository {
    val device: StateFlow<DeviceInfo>
}

interface EffectRepository {
    val effects: List<LightEffect>
}

interface SensorRepository {
    val modes: List<SensorMode>
    val snapshot: StateFlow<SensorSnapshot>
}
