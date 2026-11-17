package com.aeris.launcher.data

import android.content.ComponentName

data class AppItem(
    val key: String,          // ComponentName.flattenToString()
    val packageName: String,
    val className: String,
    val label: String,
    val category: String,     // kategori untuk App Library
) {
    val component: ComponentName get() = ComponentName(packageName, className)
}

enum class ThemeMode(val title: String) {
    NORMAL("Normal"),
    GLASS("Liquid Glass"),
    DARK("Gelap"),
}

enum class IconShape(val title: String, val cornerPercent: Int) {
    SQUIRCLE("Squircle", 26),
    CIRCLE("Bulat", 50),
    SQUARE("Kotak", 12),
}

data class LauncherSettings(
    val theme: ThemeMode = ThemeMode.GLASS,
    val iconScale: Float = 1f,
    val iconShape: IconShape = IconShape.SQUIRCLE,
    val showLabels: Boolean = true,
    val hideSystemStatusBar: Boolean = true,
    val customVolumeHud: Boolean = true,
    val city: String = "Jakarta",
    val lat: Double = -6.2146,
    val lon: Double = 106.8451,
)

data class WeatherData(
    val temp: Int,
    val min: Int,
    val max: Int,
    val code: Int,
    val isDay: Boolean,
    val city: String,
    val updatedAt: Long,
)
