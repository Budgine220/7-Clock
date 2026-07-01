package com.example.ui

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ClockDashboard(
    viewModel: ClockViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val timeState by viewModel.timeState.collectAsStateWithLifecycle()

    val ledColor = remember(settings.ledColorHex) {
        try {
            Color(android.graphics.Color.parseColor(settings.ledColorHex))
        } catch (e: Exception) {
            Color(0xFF00F0FF) // Fallback Cyan
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Adaptive, squish-free layout using BoxWithConstraints
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                val availableWidth = maxWidth
                val availableWidthPx = availableWidth.value
                val numDigits = 4
                val numColons = 1
                val colonWidthPx = 12f
                val spacerWidthPx = 4f
                val numSpacers = 2
                
                val nonDigitSpacePx = (numColons * colonWidthPx) + (numSpacers * spacerWidthPx)
                val availableDigitSpacePx = (availableWidthPx - nonDigitSpacePx).coerceAtLeast(0f)
                val digitWidthPx = (availableDigitSpacePx / numDigits).coerceAtLeast(10f)
                
                // Height is aspect ratio 1:1.8
                val calculatedHeightDp = (digitWidthPx * 1.8f).dp
                val digitHeight = calculatedHeightDp.coerceAtMost(if (isLandscape) 180.dp else 135.dp)

                Row(
                    modifier = Modifier.wrapContentSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // HOURS
                    SevenSegmentDigit(
                        char = timeState.hours.getOrNull(0) ?: ' ',
                        activeColor = ledColor,
                        inactiveOpacity = settings.inactiveOpacity,
                        isSlanted = settings.isSlanted,
                        slantAngle = settings.slantAngle,
                        modifier = Modifier.height(digitHeight)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    SevenSegmentDigit(
                        char = timeState.hours.getOrNull(1) ?: ' ',
                        activeColor = ledColor,
                        inactiveOpacity = settings.inactiveOpacity,
                        isSlanted = settings.isSlanted,
                        slantAngle = settings.slantAngle,
                        modifier = Modifier.height(digitHeight)
                    )

                    // COLON
                    SevenSegmentColon(
                        isVisible = timeState.colonsVisible,
                        activeColor = ledColor,
                        inactiveOpacity = settings.inactiveOpacity,
                        isSlanted = settings.isSlanted,
                        slantAngle = settings.slantAngle,
                        modifier = Modifier.height(digitHeight)
                    )

                    // MINUTES
                    SevenSegmentDigit(
                        char = timeState.minutes.getOrNull(0) ?: ' ',
                        activeColor = ledColor,
                        inactiveOpacity = settings.inactiveOpacity,
                        isSlanted = settings.isSlanted,
                        slantAngle = settings.slantAngle,
                        modifier = Modifier.height(digitHeight)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    SevenSegmentDigit(
                        char = timeState.minutes.getOrNull(1) ?: ' ',
                        activeColor = ledColor,
                        inactiveOpacity = settings.inactiveOpacity,
                        isSlanted = settings.isSlanted,
                        slantAngle = settings.slantAngle,
                        modifier = Modifier.height(digitHeight)
                    )
                }
            }

            if (!settings.use24HourFormat && timeState.amPm.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = timeState.amPm,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = ledColor
                )
            }
        }

        // 24/12HR format switcher placed in the bottom-left (LD) corner, styled clean and small
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.toggleFormat24h() },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ledColor
                ),
                border = BorderStroke(1.dp, ledColor.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("24h_format_switch")
            ) {
                Text(
                    text = if (settings.use24HourFormat) "24H" else "12H",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                )
            }
        }
    }
}
