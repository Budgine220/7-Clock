package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

// Helper extension to skew a coordinate horizontally around the center Y coordinate
fun Offset.slant(h: Float, factor: Float): Offset {
    if (factor == 0f) return this
    // Centering the slant shift around the middle of the digit height (h / 2)
    val shift = (h / 2f - y) * factor
    return Offset(x + shift, y)
}

@Composable
fun SevenSegmentDigit(
    char: Char,
    activeColor: Color,
    inactiveOpacity: Float = 0.05f,
    isSlanted: Boolean = true,
    slantAngle: Float = 8f,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxHeight()
            .aspectRatio(1f / 1.8f) // Standard 7-segment digit ratio
    ) {
        val w = size.width
        val h = size.height
        val t = w * 0.12f // Segment thickness
        val g = w * 0.025f // Gap between segments

        // 7 segments are represented as: [A, B, C, D, E, F, G]
        val litState = getSegmentsForChar(char)

        // Calculate slant factor
        val slantFactor = if (isSlanted) Math.tan(Math.toRadians(slantAngle.toDouble())).toFloat() else 0f

        // Helper to draw a single segment as a closed path polygon
        fun drawSegment(points: List<Offset>, isLit: Boolean) {
            val path = Path().apply {
                val pStart = points[0].slant(h, slantFactor)
                moveTo(pStart.x, pStart.y)
                for (i in 1 until points.size) {
                    val p = points[i].slant(h, slantFactor)
                    lineTo(p.x, p.y)
                }
                close()
            }

            val color = if (isLit) activeColor else activeColor.copy(alpha = inactiveOpacity)

            // Draw main filled segment
            drawPath(
                path = path,
                color = color,
                style = Fill
            )
        }

        // Segment A (Top horizontal)
        val segAPoints = listOf(
            Offset(t + g, g),
            Offset(w - t - g, g),
            Offset(w - t * 1.5f - g, t - g),
            Offset(t * 1.5f + g, t - g)
        )
        drawSegment(segAPoints, litState[0])

        // Segment F (Top-Left vertical)
        val segFPoints = listOf(
            Offset(g, t + g),
            Offset(t - g, t + g),
            Offset(t - g, h / 2f - t / 2f - g),
            Offset(t / 2f + g, h / 2f - g),
            Offset(g, h / 2f - t / 2f - g)
        )
        drawSegment(segFPoints, litState[5])

        // Segment B (Top-Right vertical)
        val segBPoints = listOf(
            Offset(w - t + g, t + g),
            Offset(w - g, t + g),
            Offset(w - g, h / 2f - t / 2f - g),
            Offset(w - t / 2f - g, h / 2f - g),
            Offset(w - t + g, h / 2f - t / 2f - g)
        )
        drawSegment(segBPoints, litState[1])

        // Segment G (Middle horizontal)
        val segGPoints = listOf(
            Offset(t + g, h / 2f),
            Offset(t * 1.5f + g, h / 2f - t / 2f + g),
            Offset(w - t * 1.5f - g, h / 2f - t / 2f + g),
            Offset(w - t - g, h / 2f),
            Offset(w - t * 1.5f - g, h / 2f + t / 2f - g),
            Offset(t * 1.5f + g, h / 2f + t / 2f - g)
        )
        drawSegment(segGPoints, litState[6])

        // Segment E (Bottom-Left vertical)
        val segEPoints = listOf(
            Offset(g, h / 2f + t / 2f + g),
            Offset(t / 2f + g, h / 2f + g),
            Offset(t - g, h / 2f + t / 2f + g),
            Offset(t - g, h - t - g),
            Offset(g, h - t - g)
        )
        drawSegment(segEPoints, litState[4])

        // Segment C (Bottom-Right vertical)
        val segCPoints = listOf(
            Offset(w - t + g, h / 2f + t / 2f + g),
            Offset(w - t / 2f - g, h / 2f + g),
            Offset(w - g, h / 2f + t / 2f + g),
            Offset(w - g, h - t - g),
            Offset(w - t + g, h - t - g)
        )
        drawSegment(segCPoints, litState[2])

        // Segment D (Bottom horizontal)
        val segDPoints = listOf(
            Offset(t * 1.5f + g, h - t + g),
            Offset(w - t * 1.5f - g, h - t + g),
            Offset(w - t - g, h - g),
            Offset(t + g, h - g)
        )
        drawSegment(segDPoints, litState[3])
    }
}

@Composable
fun SevenSegmentColon(
    isVisible: Boolean,
    activeColor: Color,
    inactiveOpacity: Float = 0.05f,
    isSlanted: Boolean = true,
    slantAngle: Float = 8f,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxHeight()
            .width(16.dp)
    ) {
        val w = size.width
        val h = size.height
        val dotRadius = w * 0.25f
        val color = if (isVisible) activeColor else activeColor.copy(alpha = inactiveOpacity)

        val slantFactor = if (isSlanted) Math.tan(Math.toRadians(slantAngle.toDouble())).toFloat() else 0f

        val topDotCenter = Offset(w / 2f, h * 0.35f).slant(h, slantFactor)
        val bottomDotCenter = Offset(w / 2f, h * 0.65f).slant(h, slantFactor)


        drawCircle(color = color, radius = dotRadius, center = topDotCenter)
        drawCircle(color = color, radius = dotRadius, center = bottomDotCenter)
    }
}

private fun getSegmentsForChar(char: Char): BooleanArray {
    return when (char) {
        '0' -> booleanArrayOf(true, true, true, true, true, true, false)
        '1' -> booleanArrayOf(false, true, true, false, false, false, false)
        '2' -> booleanArrayOf(true, true, false, true, true, false, true)
        '3' -> booleanArrayOf(true, true, true, true, false, false, true)
        '4' -> booleanArrayOf(false, true, true, false, false, true, true)
        '5' -> booleanArrayOf(true, false, true, true, false, true, true)
        '6' -> booleanArrayOf(true, false, true, true, true, true, true)
        '7' -> booleanArrayOf(true, true, true, false, false, false, false)
        '8' -> booleanArrayOf(true, true, true, true, true, true, true)
        '9' -> booleanArrayOf(true, true, true, true, false, true, true)
        '-' -> booleanArrayOf(false, false, false, false, false, false, true)
        'H' -> booleanArrayOf(false, true, true, false, true, true, true)
        'E' -> booleanArrayOf(true, false, false, true, true, true, true)
        'L' -> booleanArrayOf(false, false, false, true, true, true, false)
        'P' -> booleanArrayOf(true, true, false, false, true, true, true)
        'A' -> booleanArrayOf(true, true, true, false, true, true, true)
        'F' -> booleanArrayOf(true, false, false, false, true, true, true)
        'o' -> booleanArrayOf(false, false, true, true, true, false, true)
        'u' -> booleanArrayOf(false, false, true, true, true, false, false)
        't' -> booleanArrayOf(false, false, false, true, true, true, true)
        'S' -> booleanArrayOf(true, false, true, true, false, true, true)
        'C' -> booleanArrayOf(true, false, false, true, true, true, false)
        'd' -> booleanArrayOf(false, true, true, true, true, false, true)
        'r' -> booleanArrayOf(false, false, false, false, true, false, true)
        'n' -> booleanArrayOf(false, false, true, false, true, false, true)
        ' ' -> booleanArrayOf(false, false, false, false, false, false, false)
        else -> booleanArrayOf(false, false, false, false, false, false, false)
    }
}
