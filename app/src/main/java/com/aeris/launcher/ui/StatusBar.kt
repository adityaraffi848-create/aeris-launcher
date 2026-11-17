package com.aeris.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aeris.launcher.system.SystemStatus

/** Status bar ala iOS: jam di kiri, sinyal / wifi / baterai di kanan. Ketuk atau tarik ke bawah = Pusat Kontrol. */
@Composable
fun IosStatusBar(status: SystemStatus, barHeight: Dp, onOpenControl: () -> Unit) {
    val textStyle = TextStyle(shadow = LabelShadow)
    Row(
        Modifier
            .fillMaxWidth()
            .height(barHeight.coerceAtLeast(32.dp))
            .pointerInput(Unit) { detectTapGestures { onOpenControl() } }
            .pointerInput(Unit) {
                var dy = 0f
                detectVerticalDragGestures(
                    onDragStart = { dy = 0f },
                    onDragEnd = { if (dy > 30f) onOpenControl() },
                    onVerticalDrag = { _, d -> dy += d },
                )
            }
            .padding(horizontal = 26.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(status.timeText, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, style = textStyle)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (status.cellular || !status.wifi) SignalBars(status.cellLevel)
            if (status.wifi) WifiIcon(status.wifiLevel)
            BatteryIcon(status.battery, status.charging, textStyle)
        }
    }
}

@Composable
fun SignalBars(level: Int, color: Color = Color.White) {
    Canvas(Modifier.size(width = 18.dp, height = 11.dp)) {
        val bw = this.size.width / 7f
        for (i in 0 until 4) {
            val h = this.size.height * (0.38f + 0.2f * i)
            drawRoundRect(
                color = color.copy(alpha = if (i < level) 1f else 0.35f),
                topLeft = Offset(i * 2 * bw, this.size.height - h),
                size = Size(bw, h),
                cornerRadius = CornerRadius(bw / 3f),
            )
        }
    }
}

@Composable
fun WifiIcon(level: Int, color: Color = Color.White) {
    Canvas(Modifier.size(width = 16.dp, height = 12.dp)) {
        val w = this.size.width
        val sw = 1.8.dp.toPx()
        val cx = w / 2f
        val cy = this.size.height - sw * 0.6f
        drawCircle(color, radius = sw * 0.75f, center = Offset(cx, cy))
        for (i in 1..3) {
            val r = cy * i / 3f
            drawArc(
                color = color.copy(alpha = if (i <= level) 1f else 0.35f),
                startAngle = 225f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = Size(2 * r, 2 * r),
                style = Stroke(width = sw, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
fun BatteryIcon(percent: Int, charging: Boolean, textStyle: TextStyle) {
    val fill = when {
        charging -> Color(0xFF34C759)
        percent <= 15 -> Color(0xFFFF3B30)
        else -> Color.White
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$percent", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, style = textStyle)
        Spacer(Modifier.width(3.dp))
        Canvas(Modifier.size(width = 26.dp, height = 12.dp)) {
            val nubW = 2.dp.toPx()
            val bodyW = this.size.width - nubW - 1.dp.toPx()
            val stroke = 1.dp.toPx()
            val r = 3.5.dp.toPx()
            drawRoundRect(
                color = Color.White.copy(alpha = 0.5f),
                size = Size(bodyW, this.size.height),
                cornerRadius = CornerRadius(r),
                style = Stroke(stroke),
            )
            val inset = 1.8.dp.toPx()
            val maxFill = bodyW - inset * 2
            drawRoundRect(
                color = fill,
                topLeft = Offset(inset, inset),
                size = Size((maxFill * percent.coerceIn(2, 100) / 100f), this.size.height - inset * 2),
                cornerRadius = CornerRadius(r - 1.5.dp.toPx()),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.5f),
                topLeft = Offset(bodyW + 1.dp.toPx(), this.size.height * 0.32f),
                size = Size(nubW, this.size.height * 0.36f),
                cornerRadius = CornerRadius(1.dp.toPx()),
            )
        }
    }
}

@Composable
fun MagnifierIcon(color: Color, iconSize: Dp = 14.dp) {
    Canvas(Modifier.size(iconSize)) {
        val w = this.size.minDimension
        val stroke = w * 0.13f
        drawCircle(color, radius = w * 0.33f, center = Offset(w * 0.42f, w * 0.42f), style = Stroke(stroke))
        drawLine(color, Offset(w * 0.66f, w * 0.66f), Offset(w * 0.92f, w * 0.92f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}
