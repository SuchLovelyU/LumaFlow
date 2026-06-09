package com.example.rgbcontrller.domain.engine

import com.example.rgbcontrller.domain.model.LedMatrix
import com.example.rgbcontrller.domain.model.LedPixel
import com.example.rgbcontrller.domain.model.Keyframe
import com.example.rgbcontrller.domain.model.LightEffect
import com.example.rgbcontrller.domain.model.LiveControl
import com.example.rgbcontrller.domain.model.RgbColor
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class LightEngine {
    fun render(
        rows: Int,
        columns: Int,
        tick: Long,
        effect: LightEffect?,
        liveControl: LiveControl,
    ): LedMatrix {
        val total = rows * columns
        val pixels = List(total) { index ->
            val phase = ((tick / 34f) + index * 0.55f) % 360f
            val base = effect?.palette?.takeIf { it.isNotEmpty() } ?: listOf(liveControl.color)
            val color = colorFor(effect?.id, base, index, total, phase)
            val brightness = brightnessFor(effect?.id, index, total, phase) * liveControl.brightness
            LedPixel(
                id = index,
                color = color,
                brightness = brightness.coerceIn(0f, 1f),
                glowIntensity = (brightness * 1.25f).coerceIn(0f, 1f),
                animationPhase = phase,
            )
        }
        return LedMatrix(rows, columns, pixels)
    }

    fun renderKeyframes(
        rows: Int,
        columns: Int,
        positionMs: Long,
        keyframes: List<Keyframe>,
    ): LedMatrix {
        val total = rows * columns
        val safeKeyframes = keyframes.takeIf { it.isNotEmpty() } ?: listOf(
            Keyframe("fallback", RgbColor.Cyan, 0.8f, 500),
        )
        val totalDuration = safeKeyframes.sumOf { it.durationMs.coerceAtLeast(1) }.coerceAtLeast(1)
        val timelinePosition = (positionMs % totalDuration).toInt()

        var elapsed = 0
        val currentIndex = safeKeyframes.indexOfFirst { keyframe ->
            val nextElapsed = elapsed + keyframe.durationMs.coerceAtLeast(1)
            val contains = timelinePosition < nextElapsed
            if (!contains) elapsed = nextElapsed
            contains
        }.takeIf { it >= 0 } ?: 0

        val current = safeKeyframes[currentIndex]
        val next = safeKeyframes[(currentIndex + 1).mod(safeKeyframes.size)]
        val frameDuration = current.durationMs.coerceAtLeast(1)
        val progress = ((timelinePosition - elapsed).coerceAtLeast(0) / frameDuration.toFloat()).coerceIn(0f, 1f)
        val baseColor = mix(current.color, next.color, progress)
        val baseBrightness = current.brightness + (next.brightness - current.brightness) * progress

        val pixels = List(total) { index ->
            val shimmer = (0.82f + abs(sin((positionMs / 28f + index * 24f).toRadians())).toFloat() * 0.18f)
            val brightness = (baseBrightness * shimmer).coerceIn(0f, 1f)
            LedPixel(
                id = index,
                color = baseColor,
                brightness = brightness,
                glowIntensity = (brightness * 1.2f).coerceIn(0f, 1f),
                animationPhase = progress * 360f,
            )
        }
        return LedMatrix(rows, columns, pixels)
    }

    private fun colorFor(
        effectId: String?,
        palette: List<RgbColor>,
        index: Int,
        total: Int,
        phase: Float,
    ): RgbColor {
        if (effectId == "flame") {
            val warm = if ((phase.toInt() + index) % 3 == 0) RgbColor.Amber else RgbColor.Coral
            return mix(warm, RgbColor.Amber, abs(sin(phase.toRadians())).toFloat())
        }

        if (effectId == "blink") {
            return if (sin(phase.toRadians()) > 0f) palette.first() else palette.last()
        }

        val position = ((phase / 80f) + index / total.toFloat()).mod(1f)
        val scaled = position * palette.size
        val first = scaled.toInt().mod(palette.size)
        val second = (first + 1).mod(palette.size)
        return mix(palette[first], palette[second], scaled % 1f)
    }

    private fun brightnessFor(
        effectId: String?,
        index: Int,
        total: Int,
        phase: Float,
    ): Float {
        return when (effectId) {
            "solid" -> 0.92f
            "blink" -> if (sin(phase.toRadians()) > 0.35f) 1f else 0.18f
            "run", "meteor" -> {
                val head = ((phase / 45f).toInt()).mod(total)
                val distance = minOf(abs(index - head), total - abs(index - head))
                (1f - distance * 0.22f).coerceIn(0.14f, 1f)
            }
            "music" -> (0.3f + abs(sin((phase + index * 22f).toRadians())).toFloat() * 0.7f)
            "flame" -> (0.44f + abs(sin((phase * 1.7f + index * 31f).toRadians())).toFloat() * 0.56f)
            else -> (0.45f + sin(phase.toRadians()).toFloat() * 0.25f + index / total.toFloat() * 0.18f)
        }
    }

    private fun mix(start: RgbColor, end: RgbColor, fraction: Float): RgbColor {
        val t = fraction.coerceIn(0f, 1f)
        return RgbColor(
            red = (start.red + (end.red - start.red) * t).toInt(),
            green = (start.green + (end.green - start.green) * t).toInt(),
            blue = (start.blue + (end.blue - start.blue) * t).toInt(),
        )
    }

    private fun Float.toRadians(): Double = this / 180.0 * PI
}
