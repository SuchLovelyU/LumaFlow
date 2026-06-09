package com.example.rgbcontrller.domain.model

import androidx.compose.ui.graphics.Color

data class RgbColor(
    val red: Int,
    val green: Int,
    val blue: Int,
) {
    fun toComposeColor(alpha: Float = 1f): Color = Color(
        red = red.coerceIn(0, 255) / 255f,
        green = green.coerceIn(0, 255) / 255f,
        blue = blue.coerceIn(0, 255) / 255f,
        alpha = alpha.coerceIn(0f, 1f),
    )

    companion object {
        val Cyan = RgbColor(0, 213, 255)
        val Violet = RgbColor(155, 92, 255)
        val Green = RgbColor(77, 255, 181)
        val Coral = RgbColor(255, 107, 74)
        val Amber = RgbColor(255, 183, 77)
        val Pink = RgbColor(255, 79, 216)
        val White = RgbColor(255, 255, 255)
    }
}

data class LedPixel(
    val id: Int,
    val color: RgbColor,
    val brightness: Float,
    val glowIntensity: Float = brightness,
    val animationPhase: Float = 0f,
)

data class LedMatrix(
    val rows: Int,
    val columns: Int,
    val pixels: List<LedPixel>,
) {
    val ledCount: Int = rows * columns
}

enum class ConnectionStatus {
    Connected,
    Searching,
    Offline,
}

data class DeviceInfo(
    val name: String,
    val firmwareVersion: String,
    val ledCount: Int,
    val batteryPercent: Int,
    val isCharging: Boolean,
    val connectionStatus: ConnectionStatus,
)

enum class EffectCategory(val label: String) {
    Basic("基础效果"),
    Dynamic("动态效果"),
    Advanced("高级效果"),
}

enum class DirectionMode(val label: String) {
    Horizontal("水平"),
    Vertical("垂直"),
    Circular("环形"),
}

data class LightEffect(
    val id: String,
    val name: String,
    val description: String,
    val category: EffectCategory,
    val palette: List<RgbColor>,
)

data class LiveControl(
    val color: RgbColor = RgbColor.Cyan,
    val hue: Float = 190f,
    val saturation: Float = 0.85f,
    val value: Float = 1f,
    val brightness: Float = 0.78f,
    val speed: Float = 0.55f,
    val direction: DirectionMode = DirectionMode.Horizontal,
    val selectedLedId: Int? = null,
)

enum class SensorModeType(val label: String) {
    Music("音乐律动"),
    Gravity("重力模式"),
    Gyroscope("陀螺仪"),
    Shake("摇晃模式"),
}

data class SensorMode(
    val id: String,
    val title: String,
    val description: String,
    val type: SensorModeType,
    val actions: List<String>,
)

data class Vector3(
    val x: Float,
    val y: Float,
    val z: Float,
)

data class SensorSnapshot(
    val microphoneLevel: Float,
    val gravity: Vector3,
    val gyroscope: Vector3,
    val shakeIntensity: Float,
)

data class Keyframe(
    val id: String,
    val color: RgbColor,
    val brightness: Float,
    val durationMs: Int,
)

data class PlaybackState(
    val isPlaying: Boolean,
    val positionMs: Long,
    val loop: Boolean = true,
)

data class AppSettings(
    val darkMode: Boolean = false,
    val dynamicTheme: Boolean = true,
    val developerMode: Boolean = false,
    val animationSpeed: Float = 0.72f,
    val defaultBrightness: Float = 0.78f,
)

data class LightSessionState(
    val device: DeviceInfo,
    val matrix: LedMatrix,
    val activeEffect: LightEffect?,
    val liveControl: LiveControl,
    val playback: PlaybackState,
    val keyframes: List<Keyframe>,
)
