package com.example.rgbcontrller.presentation.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.rgbcontrller.domain.model.ConnectionStatus
import com.example.rgbcontrller.domain.model.DeviceInfo
import com.example.rgbcontrller.domain.model.Keyframe
import com.example.rgbcontrller.domain.model.LedMatrix
import com.example.rgbcontrller.domain.model.LightEffect
import com.example.rgbcontrller.domain.model.RgbColor
import com.example.rgbcontrller.domain.model.SensorMode
import com.example.rgbcontrller.domain.model.SensorSnapshot
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun AuroraBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        Color(0xFFEEF6FC),
                    ),
                ),
            ),
    ) {
        content()
    }
}

@Composable
fun DeviceStatusHeader(
    device: DeviceInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(device.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(device.connectionStatus)
                    Text(
                        text = when (device.connectionStatus) {
                            ConnectionStatus.Connected -> "Bluetooth connected"
                            ConnectionStatus.Searching -> "Searching"
                            ConnectionStatus.Offline -> "Offline"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusDot(status: ConnectionStatus) {
    val color = when (status) {
        ConnectionStatus.Connected -> Color(0xFF4DFFB5)
        ConnectionStatus.Searching -> Color(0xFFFFB74D)
        ConnectionStatus.Offline -> Color(0xFFFF6B4A)
    }
    Box(
        Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
fun LedMatrixPreview(
    matrix: LedMatrix,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            title?.let {
                Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(16.dp))
            }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.72f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                MaterialTheme.colorScheme.surface,
                            ),
                        ),
                    ),
            ) {
                drawMatrix(matrix)
            }
        }
    }
}

private fun DrawScope.drawMatrix(matrix: LedMatrix) {
    val columns = matrix.columns
    val rows = matrix.rows
    val cellW = size.width / columns
    val cellH = size.height / rows
    val ledRadius = minOf(cellW, cellH) * 0.22f
    matrix.pixels.forEach { pixel ->
        val row = pixel.id / columns
        val column = pixel.id % columns
        val center = Offset(column * cellW + cellW / 2f, row * cellH + cellH / 2f)
        val color = pixel.color.toComposeColor()
        drawCircle(Color.Black.copy(alpha = 0.22f), ledRadius * 1.18f, center)
        drawCircle(color.copy(alpha = 0.22f * pixel.glowIntensity), ledRadius * 2.8f, center)
        drawCircle(color.copy(alpha = 0.34f * pixel.glowIntensity), ledRadius * 1.75f, center)
        drawCircle(color.copy(alpha = 0.95f * pixel.brightness), ledRadius * (0.72f + pixel.brightness * 0.38f), center)
        drawCircle(Color.White.copy(alpha = 0.45f * pixel.brightness), ledRadius * 0.25f, center + Offset(-ledRadius * 0.25f, -ledRadius * 0.25f))
    }
}

@Composable
fun SceneShortcutCard(effect: LightEffect, onClick: () -> Unit, modifier: Modifier = Modifier, isActive: Boolean = false) {
    val shape = RoundedCornerShape(22.dp)
    Card(
        onClick = onClick,
        modifier = modifier.height(142.dp),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 4.dp else 2.dp),
        border = selectedBorder(isActive),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            MiniEffectPreview(effect.palette, modifier = Modifier.fillMaxWidth().height(58.dp))
            Column {
                Text(effect.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(effect.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun EffectMarketCard(effect: LightEffect, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            MiniEffectPreview(effect.palette, modifier = Modifier.size(96.dp).clip(RoundedCornerShape(18.dp)))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(effect.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(effect.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                Spacer(Modifier.height(8.dp))
                AssistChip(onClick = onClick, label = { Text(effect.category.label) })
            }
        }
    }
}

@Composable
fun MiniEffectPreview(palette: List<RgbColor>, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "effect-preview")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8800, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    Canvas(modifier) {
        val colors = palette.ifEmpty { listOf(RgbColor.Cyan) }.map { it.toComposeColor() }
        val corner = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx())
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    colors.first().copy(alpha = 0.78f),
                    colors.getOrElse(1) { colors.first() }.copy(alpha = 0.64f),
                    colors.last().copy(alpha = 0.82f),
                ),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            ),
            cornerRadius = corner,
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.16f),
            radius = size.minDimension * (0.52f + 0.08f * absSin(phase * TwoPi)),
            center = Offset(
                size.width * (0.2f + 0.12f * sin(phase * TwoPi)),
                size.height * (0.24f + 0.1f * cos(phase * TwoPi)),
            ),
        )
        colors.forEachIndexed { index, color ->
            val path = Path()
            val bandPhase = phase * TwoPi + index * 1.25f
            val centerY = size.height * (0.32f + index.coerceAtMost(2) * 0.17f)
            val amplitude = size.height * (0.11f + index.coerceAtMost(2) * 0.018f)
            val steps = 28
            repeat(steps + 1) { step ->
                val progress = step / steps.toFloat()
                val x = size.width * progress
                val y = centerY + sin(progress * TwoPi + bandPhase) * amplitude
                if (step == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            drawPath(
                path = path,
                color = color.copy(alpha = 0.34f),
                style = Stroke(width = size.minDimension * (0.11f + index * 0.012f), cap = StrokeCap.Round),
            )
        }
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.26f), Color.White.copy(alpha = 0.05f)),
            ),
            cornerRadius = corner,
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.28f),
            cornerRadius = corner,
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}

@Composable
fun ExpressiveSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
) {
    val safeRange = if (valueRange.endInclusive > valueRange.start) valueRange else valueRange.start..(valueRange.start + 0.001f)
    val displayValue = value.coerceIn(valueRange.start, valueRange.endInclusive)
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text("${(displayValue * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value.coerceIn(safeRange.start, safeRange.endInclusive),
            onValueChange = { onValueChange(it.coerceIn(safeRange.start, safeRange.endInclusive)) },
            valueRange = safeRange,
        )
    }
}

@Composable
fun ColorWheelControl(
    colors: List<RgbColor>,
    hue: Float,
    saturation: Float,
    onColorSelected: (hue: Float, saturation: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    fun selectColor(position: Offset, width: Int, height: Int) {
        val radius = minOf(width, height) / 2f
        val center = Offset(width / 2f, height / 2f)
        val dx = position.x - center.x
        val dy = position.y - center.y
        val distance = sqrt(dx * dx + dy * dy)
        val nextHue = ((atan2(dy, dx).toFloat() / Math.PI.toFloat() * 180f) + 360f) % 360f
        val nextSaturation = (distance / radius).coerceIn(0f, 1f)
        onColorSelected(nextHue, nextSaturation)
    }
    val markerStrokeColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)

    Canvas(
        modifier
            .aspectRatio(1f)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    selectColor(down.position, size.width, size.height)
                    drag(down.id) { change ->
                        selectColor(change.position, size.width, size.height)
                        change.consume()
                    }
                }
            },
    ) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val wheelColors = colors.ifEmpty { listOf(RgbColor.Coral, RgbColor.Amber, RgbColor.Green, RgbColor.Cyan, RgbColor.Violet, RgbColor.Pink, RgbColor.Coral) }
            .map { it.toComposeColor() }
        drawCircle(
            brush = Brush.sweepGradient(wheelColors, center = center),
            radius = radius,
            center = center,
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color.White.copy(alpha = 0f)),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
        val angle = hue / 180f * Math.PI.toFloat()
        val markerRadius = saturation.coerceIn(0f, 1f) * radius
        val marker = center + Offset(cos(angle) * markerRadius, sin(angle) * markerRadius)
        drawCircle(Color.White, radius * 0.085f, marker)
        drawCircle(markerStrokeColor, radius * 0.085f, marker, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx()))
    }
}

@Composable
fun SensorModeCard(
    mode: SensorMode,
    snapshot: SensorSnapshot,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
) {
    val shape = RoundedCornerShape(22.dp)
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 4.dp else 2.dp),
        border = selectedBorder(isActive),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            SensorPreview(mode.id, snapshot, Modifier.size(82.dp).clip(RoundedCornerShape(18.dp)))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(mode.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(mode.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
        }
    }
}

@Composable
fun SensorPreview(id: String, snapshot: SensorSnapshot, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "sensor-preview")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(7200, easing = LinearEasing), RepeatMode.Restart),
        label = "sensor-phase",
    )
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    Canvas(modifier.background(MaterialTheme.colorScheme.surface)) {
        val level = when (id) {
            "music" -> snapshot.microphoneLevel
            "shake" -> snapshot.shakeIntensity
            else -> (kotlin.math.abs(snapshot.gravity.x) + kotlin.math.abs(snapshot.gravity.y)) / 2f
        }.coerceIn(0f, 1f)
        drawSensorBackdrop(phase, level, primary, secondary, tertiary)
        when (id) {
            "music" -> drawMusicSensorIcon(phase, level, primary, secondary)
            "gravity" -> drawGravitySensorIcon(snapshot, level, primary, tertiary)
            "gyro" -> drawGyroSensorIcon(phase, snapshot, primary, secondary)
            "shake" -> drawShakeSensorIcon(phase, level, tertiary, secondary)
            else -> drawGyroSensorIcon(phase, snapshot, primary, secondary)
        }
    }
}

private fun DrawScope.drawSensorBackdrop(
    phase: Float,
    level: Float,
    primary: Color,
    secondary: Color,
    tertiary: Color,
) {
    val corner = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx())
    drawRoundRect(
        brush = Brush.linearGradient(
            listOf(
                Color(0xFFF9FCFF),
                primary.copy(alpha = 0.12f + level * 0.1f),
                secondary.copy(alpha = 0.08f),
            ),
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        ),
        cornerRadius = corner,
    )
    drawCircle(
        color = tertiary.copy(alpha = 0.1f + level * 0.12f),
        radius = size.minDimension * 0.46f,
        center = Offset(
            size.width * (0.72f + 0.08f * sin(phase * TwoPi)),
            size.height * (0.24f + 0.06f * cos(phase * TwoPi)),
        ),
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.64f),
        cornerRadius = corner,
        style = Stroke(width = 1.dp.toPx()),
    )
}

private fun DrawScope.drawMusicSensorIcon(
    phase: Float,
    level: Float,
    primary: Color,
    secondary: Color,
) {
    val mid = size.height * 0.52f
    val amplitude = size.height * (0.1f + level * 0.18f)
    val path = Path()
    val steps = 34
    repeat(steps + 1) { step ->
        val progress = step / steps.toFloat()
        val x = size.width * (0.12f + progress * 0.76f)
        val wave = sin(progress * TwoPi * 2.2f + phase * TwoPi)
        val y = mid + wave * amplitude * (0.62f + 0.38f * sin(progress * PI.toFloat()))
        if (step == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    drawPath(
        path = path,
        brush = Brush.linearGradient(
            listOf(primary, secondary),
            start = Offset(size.width * 0.12f, 0f),
            end = Offset(size.width * 0.88f, 0f),
        ),
        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
    )
    repeat(5) { index ->
        val progress = index / 4f
        val x = size.width * (0.22f + progress * 0.56f)
        val glow = 0.36f + 0.4f * absSin(phase * TwoPi + index)
        drawCircle(
            color = Color.White.copy(alpha = glow),
            radius = size.minDimension * (0.03f + level * 0.02f),
            center = Offset(x, mid),
        )
    }
}

private fun DrawScope.drawGravitySensorIcon(
    snapshot: SensorSnapshot,
    level: Float,
    primary: Color,
    tertiary: Color,
) {
    val glassLeft = size.width * 0.22f
    val glassTop = size.height * 0.16f
    val glassSize = Size(size.width * 0.56f, size.height * 0.68f)
    val radius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx())
    drawRoundRect(
        color = Color.White.copy(alpha = 0.42f),
        topLeft = Offset(glassLeft, glassTop),
        size = glassSize,
        cornerRadius = radius,
    )
    val baseSurface = glassTop + glassSize.height * (0.58f - level * 0.08f)
    val slope = snapshot.gravity.x.coerceIn(-1f, 1f) * glassSize.height * 0.18f
    val edgeInset = 4.dp.toPx()
    val liquidPath = Path().apply {
        moveTo(glassLeft + edgeInset, baseSurface - slope)
        lineTo(glassLeft + glassSize.width - edgeInset, baseSurface + slope)
        lineTo(glassLeft + glassSize.width - edgeInset, glassTop + glassSize.height - 5.dp.toPx())
        lineTo(glassLeft + edgeInset, glassTop + glassSize.height - 5.dp.toPx())
        close()
    }
    drawPath(
        path = liquidPath,
        brush = Brush.verticalGradient(
            listOf(primary.copy(alpha = 0.72f), tertiary.copy(alpha = 0.62f)),
            startY = baseSurface - glassSize.height * 0.2f,
            endY = glassTop + glassSize.height,
        ),
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.78f),
        topLeft = Offset(glassLeft, glassTop),
        size = glassSize,
        cornerRadius = radius,
        style = Stroke(width = 2.dp.toPx()),
    )
    drawLine(
        color = Color.White.copy(alpha = 0.72f),
        start = Offset(glassLeft + 8.dp.toPx(), baseSurface - slope),
        end = Offset(glassLeft + glassSize.width - 8.dp.toPx(), baseSurface + slope),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawGyroSensorIcon(
    phase: Float,
    snapshot: SensorSnapshot,
    primary: Color,
    secondary: Color,
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val rotation = phase * 360f + snapshot.gyroscope.z * 18f
    repeat(3) { index ->
        val inset = size.minDimension * (0.18f + index * 0.09f)
        drawArc(
            color = if (index % 2 == 0) primary.copy(alpha = 0.54f) else secondary.copy(alpha = 0.44f),
            startAngle = rotation + index * 46f,
            sweepAngle = 210f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(size.width - inset * 2f, size.height - inset * 2f),
            style = Stroke(width = (3 + index).dp.toPx(), cap = StrokeCap.Round),
        )
    }
    val dotAngle = phase * TwoPi + snapshot.gyroscope.z * 0.2f
    val dotRadius = size.minDimension * 0.26f
    drawCircle(
        color = Color.White,
        radius = size.minDimension * 0.055f,
        center = center + Offset(cos(dotAngle) * dotRadius, sin(dotAngle) * dotRadius),
    )
    drawCircle(primary.copy(alpha = 0.72f), size.minDimension * 0.09f, center)
}

private fun DrawScope.drawShakeSensorIcon(
    phase: Float,
    level: Float,
    primary: Color,
    secondary: Color,
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val pulse = 0.72f + level * 0.52f + 0.08f * sin(phase * TwoPi)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(primary.copy(alpha = 0.48f), primary.copy(alpha = 0.04f)),
            center = center,
            radius = size.minDimension * 0.36f * pulse,
        ),
        radius = size.minDimension * 0.36f * pulse,
        center = center,
    )
    repeat(6) { index ->
        val angle = phase * TwoPi * 0.6f + index * TwoPi / 6f
        val inner = size.minDimension * (0.16f + level * 0.04f)
        val outer = size.minDimension * (0.3f + level * 0.13f)
        drawLine(
            color = if (index % 2 == 0) primary.copy(alpha = 0.68f) else secondary.copy(alpha = 0.54f),
            start = center + Offset(cos(angle) * inner, sin(angle) * inner),
            end = center + Offset(cos(angle) * outer, sin(angle) * outer),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
    drawRoundRect(
        color = Color.White.copy(alpha = 0.8f),
        topLeft = Offset(size.width * 0.35f, size.height * 0.35f),
        size = Size(size.width * 0.3f, size.height * 0.3f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),
    )
}

@Composable
fun TimelineEditor(
    keyframes: List<Keyframe>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Keyframes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("${keyframes.size} frames", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))
        if (keyframes.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(96.dp),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("No keyframes yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(keyframes, key = { it.id }) { keyframe ->
                val isSelected = selectedId == keyframe.id
                Surface(
                    modifier = Modifier
                        .width(112.dp)
                        .height(96.dp)
                        .clickable { onSelect(keyframe.id) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    tonalElevation = if (isSelected) 4.dp else 1.dp,
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(keyframe.color.toComposeColor()),
                        )
                        Text("${keyframe.durationMs} ms", style = MaterialTheme.typography.labelLarge)
                        Text("${(keyframe.brightness * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            }
        }
        AnimatedVisibility(visible = selectedId != null, enter = fadeIn(), exit = fadeOut()) {
            Text(
                selectedId?.let { "Frame $it selected" }.orEmpty(),
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun selectedBorder(isActive: Boolean): BorderStroke? {
    return if (isActive) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
    } else {
        null
    }
}

@Composable
fun PageTitle(title: String, modifier: Modifier = Modifier, subtitle: String? = null) {
    Column(modifier.fillMaxWidth()) {
        AnimatedContent(
            targetState = title,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
            label = "page-title",
        ) { value ->
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        subtitle?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private const val TwoPi = 6.2831855f

private fun absSin(value: Float): Float = kotlin.math.abs(sin(value))
