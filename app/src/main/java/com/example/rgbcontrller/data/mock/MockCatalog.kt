package com.example.rgbcontrller.data.mock

import com.example.rgbcontrller.domain.model.ConnectionStatus
import com.example.rgbcontrller.domain.model.DeviceInfo
import com.example.rgbcontrller.domain.model.EffectCategory
import com.example.rgbcontrller.domain.model.Keyframe
import com.example.rgbcontrller.domain.model.LightEffect
import com.example.rgbcontrller.domain.model.RgbColor
import com.example.rgbcontrller.domain.model.SensorMode
import com.example.rgbcontrller.domain.model.SensorModeType

object MockCatalog {
    val device = DeviceInfo(
        name = "Aurora Matrix 2x4",
        firmwareVersion = "v0.9.2-dev",
        ledCount = 8,
        batteryPercent = 86,
        isCharging = false,
        connectionStatus = ConnectionStatus.Offline,
    )

    val effects = listOf(
        LightEffect("solid", "Solid", "Stable single-color output for ambient light.", EffectCategory.Basic, listOf(RgbColor.White, RgbColor.Cyan)),
        LightEffect("breath", "Breath", "Soft rising and falling brightness.", EffectCategory.Basic, listOf(RgbColor.Cyan, RgbColor.Violet)),
        LightEffect("blink", "Blink", "Short pulses for alerts and rhythm marks.", EffectCategory.Basic, listOf(RgbColor.White, RgbColor.Amber)),
        LightEffect("run", "Run", "A continuous stream moving across the matrix.", EffectCategory.Basic, listOf(RgbColor.Green, RgbColor.Cyan)),
        LightEffect("rainbow", "Rainbow", "Full RGB color bands flowing through the LEDs.", EffectCategory.Dynamic, listOf(RgbColor.Coral, RgbColor.Amber, RgbColor.Green, RgbColor.Cyan, RgbColor.Violet)),
        LightEffect("wave", "Wave", "Soft color bands expanding across the matrix.", EffectCategory.Dynamic, listOf(RgbColor.Cyan, RgbColor.Violet, RgbColor.Pink)),
        LightEffect("meteor", "Meteor", "A bright head with a trailing tail.", EffectCategory.Dynamic, listOf(RgbColor.White, RgbColor.Cyan)),
        LightEffect("flame", "Flame", "Warm flicker with amber and coral cores.", EffectCategory.Dynamic, listOf(RgbColor.Coral, RgbColor.Amber)),
        LightEffect("aurora", "Aurora", "Slow green, cyan, and violet movement.", EffectCategory.Dynamic, listOf(RgbColor.Green, RgbColor.Cyan, RgbColor.Violet)),
        LightEffect("neon", "Neon", "High-contrast electric color switching.", EffectCategory.Dynamic, listOf(RgbColor.Pink, RgbColor.Cyan)),
        LightEffect("music", "Music Pulse", "LEDs stay off below the volume threshold, then react to peaks.", EffectCategory.Advanced, listOf(RgbColor.Cyan, RgbColor.Pink, RgbColor.Amber)),
        LightEffect("gravity", "Gravity Fluid", "Tilt the phone and light flows to the low side like liquid.", EffectCategory.Advanced, listOf(RgbColor.Cyan, RgbColor.Green, RgbColor.Amber)),
        LightEffect("shake", "Shake Burst", "Shake-triggered flashes and particle-like bursts.", EffectCategory.Advanced, listOf(RgbColor.White, RgbColor.Pink, RgbColor.Coral)),
        LightEffect("direction", "Direction", "Phone orientation changes the light movement direction.", EffectCategory.Advanced, listOf(RgbColor.Violet, RgbColor.Cyan)),
        LightEffect("ambient", "Ambient", "Brightness and warmth adapt to the room.", EffectCategory.Advanced, listOf(RgbColor.White, RgbColor.Amber)),
    )

    val shortcuts = listOf(
        effects.first { it.id == "aurora" },
        effects.first { it.id == "run" },
        effects.first { it.id == "breath" },
        effects.first { it.id == "rainbow" },
        effects.first { it.id == "music" },
        effects.first { it.id == "gravity" },
        effects.first { it.id == "flame" },
        effects.first { it.id == "meteor" },
    )

    val sensorModes = listOf(
        SensorMode("music", "Music Pulse", "Mic input drives the matrix only after the threshold is crossed.", SensorModeType.Music, listOf("Threshold gate", "Peak pulse", "Spectrum tint")),
        SensorMode("gravity", "Gravity Fluid", "A continuous liquid surface follows the low side of the phone.", SensorModeType.Gravity, listOf("Fluid surface", "Low-side pooling", "Density color")),
        SensorMode("gyro", "Gyro Follow", "Gyroscope movement changes the animation direction.", SensorModeType.Gyroscope, listOf("Rotation follow", "Direction track")),
        SensorMode("shake", "Shake Burst", "Shake intensity triggers bursts and flashes.", SensorModeType.Shake, listOf("Burst", "Spray", "Flash")),
    )

    val keyframes = listOf(
        Keyframe("kf-1", RgbColor.Cyan, 0.8f, 420),
        Keyframe("kf-2", RgbColor.Violet, 1f, 560),
        Keyframe("kf-3", RgbColor.Pink, 0.72f, 380),
        Keyframe("kf-4", RgbColor.Green, 0.9f, 640),
    )
}
