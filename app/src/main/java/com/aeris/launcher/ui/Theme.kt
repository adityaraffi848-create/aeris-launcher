package com.aeris.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.aeris.launcher.data.LauncherSettings
import com.aeris.launcher.data.ThemeMode

@Immutable
class Palette(
    val mode: ThemeMode,
    val cardBrush: Brush,
    val cardBorder: Brush,
    val dockBrush: Brush,
    val scrim: Color,
    val onCard: Color,
    val sheetBg: Color,
    val sheetItem: Color,
    val sheetText: Color,
    val sheetSub: Color,
    val accent: Color,
)

fun paletteFor(mode: ThemeMode): Palette = when (mode) {
    ThemeMode.NORMAL -> Palette(
        mode = mode,
        cardBrush = SolidColor(Color(0xCCFFFFFF)),
        cardBorder = SolidColor(Color(0x14000000)),
        dockBrush = SolidColor(Color(0x66FFFFFF)),
        scrim = Color.Transparent,
        onCard = Color(0xFF3C3C43),
        sheetBg = Color(0xFFF2F2F7),
        sheetItem = Color.White,
        sheetText = Color(0xFF111114),
        sheetSub = Color(0xFF6C6C72),
        accent = Color(0xFF007AFF),
    )
    ThemeMode.GLASS -> Palette(
        mode = mode,
        cardBrush = Brush.verticalGradient(listOf(Color(0x47FFFFFF), Color(0x1AFFFFFF))),
        cardBorder = Brush.verticalGradient(listOf(Color(0xA6FFFFFF), Color(0x1FFFFFFF))),
        dockBrush = Brush.verticalGradient(listOf(Color(0x4DFFFFFF), Color(0x24FFFFFF))),
        scrim = Color(0x14000000),
        onCard = Color.White,
        sheetBg = Color(0xE6161824),
        sheetItem = Color(0x22FFFFFF),
        sheetText = Color.White,
        sheetSub = Color(0xB3FFFFFF),
        accent = Color(0xFF64D2FF),
    )
    ThemeMode.DARK -> Palette(
        mode = mode,
        cardBrush = SolidColor(Color(0xE61C1C1E)),
        cardBorder = SolidColor(Color(0x22FFFFFF)),
        dockBrush = SolidColor(Color(0xB31C1C1E)),
        scrim = Color(0x66000000),
        onCard = Color.White,
        sheetBg = Color(0xFF1C1C1E),
        sheetItem = Color(0xFF2C2C2E),
        sheetText = Color.White,
        sheetSub = Color(0xFF9A9AA0),
        accent = Color(0xFF0A84FF),
    )
}

val LocalPalette = staticCompositionLocalOf { paletteFor(ThemeMode.GLASS) }
val LocalSettings = staticCompositionLocalOf { LauncherSettings() }

/** Permukaan kaca: isi translusen + garis tepi bergradasi. */
fun Modifier.glass(p: Palette, shape: Shape, dock: Boolean = false): Modifier =
    this.clip(shape)
        .background(if (dock) p.dockBrush else p.cardBrush, shape)
        .border(1.dp, p.cardBorder, shape)

val LabelShadow = Shadow(color = Color(0x80000000), offset = Offset(0f, 1.5f), blurRadius = 4f)
