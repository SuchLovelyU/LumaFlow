package com.example.rgbcontrller.presentation.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.rgbcontrller.domain.model.ConnectionStatus
import com.example.rgbcontrller.domain.model.DeviceInfo
import com.example.rgbcontrller.domain.model.DirectionMode
import com.example.rgbcontrller.domain.model.Keyframe
import com.example.rgbcontrller.domain.model.LedMatrix
import com.example.rgbcontrller.domain.model.LightEffect
import com.example.rgbcontrller.domain.model.RgbColor
import com.example.rgbcontrller.domain.model.SensorMode
import com.example.rgbcontrller.domain.model.SensorSnapshot
import kotlin.math.sin

@Composable
fun AuroraBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000), RepeatMode.Reverse),
        label = "drift",
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Canvas(Modifier.fillMaxSize().blur(28.dp)) {
            drawCircle(
                color = Color(0xFF00D5FF).copy(alpha = 0.18f),
                radius = size.minDimension * 0.55f,
                center = Offset(size.width * (0.18f + drift * 0.14f), size.height * 0.12f),
            )
            drawCircle(
                color = Color(0xFFFF4FD8).copy(alpha = 0.12f),
                radius = size.minDimension * 0.5f,
                center = Offset(size.width * (0.85f - drift * 0.18f), size.height * 0.38f),
            )
            drawCircle(
                color = Color(0xFF4DFFB5).copy(alpha = 0.10f),
                radius = size.minDimension * 0.42f,
                center = Offset(size.width * 0.35f, size.height * (0.86f - drift * 0.08f)),
            )
        }
        content()
    }
}

@Composable
fun DeviceStatusHeader(
    device: DeviceInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        ),
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
            Column(horizontalAlignment = Alignment.End) {
                Text("${device.batteryPercent}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (device.isCharging) "Charging" else "Battery",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { device.batteryPercent / 100f },
                    modifier = Modifier.width(76.dp).height(6.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
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
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
        ),
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
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
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
        drawCircle(color.copy(alpha = 0.22f * pixel.glowIntensity), ledRadius * 2.8f, center)
        drawCircle(color.copy(alpha = 0.34f * pixel.glowIntensity), ledRadius * 1.75f, center)
        drawCircle(color.copy(alpha = 0.95f), ledRadius * (0.82f + pixel.brightness * 0.28f), center)
        drawCircle(Color.White.copy(alpha = 0.45f), ledRadius * 0.25f, center + Offset(-ledRadius * 0.25f, -ledRadius * 0.25f))
    }
}

@Composable
fun SceneShortcutCard(effect: LightEffect, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .height(142.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)),
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
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            MiniEffectPreview(effect.palette, modifier = Modifier.size(96.dp).clip(RoundedCornerShape(8.dp)))
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
        animationSpec = infiniteRepeatable(tween(2400), RepeatMode.Restart),
        label = "phase",
    )
    Canvas(modifier) {
        val colors = palette.ifEmpty { listOf(RgbColor.Cyan) }.map { it.toComposeColor() }
        drawRoundRect(
            brush = Brush.linearGradient(colors, start = Offset(size.width * phase, 0f), end = Offset(0f, size.height)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
        )
        repeat(8) { index ->
            val x = size.width * ((index / 7f + phase) % 1f)
            val y = size.height * (0.22f + 0.56f * absSin(index * 0.7f + phase * 6.28f))
            drawCircle(Color.White.copy(alpha = 0.16f), size.minDimension * 0.12f, Offset(x, y))
        }
    }
}

@Composable
fun ExpressiveSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value.coerceIn(0f, 1f), onValueChange = { onValueChange(it.coerceIn(0f, 1f)) })
    }
}

@Composable
fun DirectionSegmentedControl(
    selected: DirectionMode,
    onSelect: (DirectionMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DirectionMode.entries.forEach { mode ->
            FilterChip(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                label = { Text(mode.label) },
            )
        }
    }
}

@Composable
fun ColorWheelControl(
    colors: List<RgbColor>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.aspectRatio(1f)) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        repeat(72) { i ->
            val angle = i / 72f * 360f
            drawArc(
                brush = Brush.sweepGradient(colors.map { it.toComposeColor() }),
                startAngle = angle,
                sweepAngle = 7f,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
            )
        }
        drawCircle(Color.White.copy(alpha = 0.16f), radius * 0.46f, center)
        drawCircle(Color.White.copy(alpha = 0.86f), radius * 0.08f, center + Offset(radius * 0.28f, -radius * 0.18f))
    }
}

@Composable
fun SensorModeCard(
    mode: SensorMode,
    snapshot: SensorSnapshot,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            SensorPreview(mode.id, snapshot, Modifier.size(82.dp).clip(RoundedCornerShape(8.dp)))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(mode.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(mode.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    mode.actions.take(3).forEach { AssistChip(onClick = {}, label = { Text(it) }) }
                }
            }
        }
    }
}

@Composable
fun SensorPreview(id: String, snapshot: SensorSnapshot, modifier: Modifier = Modifier) {
    Canvas(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        val level = when (id) {
            "music" -> snapshot.microphoneLevel
            "shake" -> snapshot.shakeIntensity
            else -> (kotlin.math.abs(snapshot.gravity.x) + kotlin.math.abs(snapshot.gravity.y)) / 2f
        }.coerceIn(0f, 1f)
        drawRect(
            brush = Brush.linearGradient(listOf(Color(0xFF00D5FF), Color(0xFFFF4FD8), Color(0xFFFFB74D))),
            alpha = 0.26f + level * 0.55f,
        )
        repeat(8) { index ->
            val barWidth = size.width / 12f
            val barHeight = size.height * (0.16f + absSin(index + level * 4f) * 0.74f)
            val left = size.width * (0.12f + index * 0.095f)
            drawRoundRect(
                color = Color.White.copy(alpha = 0.76f),
                topLeft = Offset(left, size.height - barHeight - 10f),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f),
            )
        }
    }
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
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
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
                                .clip(RoundedCornerShape(8.dp))
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

private fun absSin(value: Float): Float = kotlin.math.abs(sin(value))
