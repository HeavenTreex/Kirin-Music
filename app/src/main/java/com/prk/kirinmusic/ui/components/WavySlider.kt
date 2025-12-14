package com.prk.kirinmusic.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WavySlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit = {},
    modifier: Modifier = Modifier,
    waveAmplitude: Float = 0.5f,
    waveFrequency: Float = 5f
) {
    val trackHeight = 8.dp
    val thumbRadius = 8.dp
    val colorPrimary = MaterialTheme.colorScheme.primary
    val colorSecondary = MaterialTheme.colorScheme.secondaryContainer
    val interactionSource = remember { MutableInteractionSource() }

    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        onValueChangeFinished = onValueChangeFinished,
        interactionSource = interactionSource,
        colors = SliderDefaults.colors(
            thumbColor = Color.Transparent,
            activeTrackColor = Color.Transparent,
            inactiveTrackColor = Color.Transparent
        ),
        thumb = {
            Box(modifier = Modifier.size(thumbRadius * 2))
        },
        track = { sliderState ->
            Canvas(modifier = Modifier.fillMaxWidth().height(trackHeight * 2)) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val waveHeightPx = trackHeight.toPx() * waveAmplitude
                val centerY = canvasHeight / 2f
                val activeWidth = canvasWidth * value

                val inactivePath = Path().apply {
                    moveTo(0f, centerY)
                    lineTo(canvasWidth, centerY)
                }

                drawPath(
                    inactivePath,
                    colorSecondary,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = trackHeight.toPx())
                )

                val activePath = Path().apply {
                    moveTo(0f, centerY)
                    for (x in 0..activeWidth.toInt() step 5) {
                        val yOffset = sin(x * waveFrequency * (kotlin.math.PI / 180).toFloat()) * waveHeightPx
                        lineTo(x.toFloat(), centerY + yOffset)
                    }
                }

                drawPath(
                    activePath,
                    colorPrimary,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = trackHeight.toPx(), cap = StrokeCap.Round)
                )

                val thumbX = activeWidth.coerceIn(thumbRadius.toPx(), canvasWidth - thumbRadius.toPx())
                val thumbY = if (activeWidth.toInt() > 0) {
                    centerY + sin(activeWidth * waveFrequency * (kotlin.math.PI / 180).toFloat()) * waveHeightPx
                } else {
                    centerY
                }

                drawCircle(
                    colorPrimary,
                    radius = thumbRadius.toPx(),
                    center = Offset(thumbX, thumbY)
                )
            }
        }
    )
}