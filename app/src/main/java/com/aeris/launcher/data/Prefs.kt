package com.aeris.launcher.data

import android.content.Context
import org.json.JSONObject

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("aeris_prefs", Context.MODE_PRIVATE)

    var firstRunDone: Boolean
        get() = sp.getBoolean("first_run_done", false)
        set(v) { sp.edit().putBoolean("first_run_done", v).apply() }

    fun getList(name: String): List<String>? {
        val raw = sp.getString(name, null) ?: return null
        return if (raw.isEmpty()) emptyList() else raw.split("|")
    }

    fun putList(name: String, list: List<String>) {
        sp.edit().putString(name, list.joinToString("|")).apply()
    }

    fun loadSettings(): LauncherSettings {
        val d = LauncherSettings()
        return LauncherSettings(
            theme = runCatching { ThemeMode.valueOf(sp.getString("theme", null) ?: d.theme.name) }.getOrDefault(d.theme),
            iconScale = sp.getFloat("icon_scale", d.iconScale),
            iconShape = runCatching { IconShape.valueOf(sp.getString("icon_shape", null) ?: d.iconShape.name) }.getOrDefault(d.iconShape),
            showLabels = sp.getBoolean("show_labels", d.showLabels),
            hideSystemStatusBar = sp.getBoolean("hide_status", d.hideSystemStatusBar),
            customVolumeHud = sp.getBoolean("volume_hud", d.customVolumeHud),
            city = sp.getString("city", null) ?: d.city,
            lat = sp.getString("lat", null)?.toDoubleOrNull() ?: d.lat,
            lon = sp.getString("lon", null)?.toDoubleOrNull() ?: d.lon,
        )
    }

    fun saveSettings(s: LauncherSettings) {
        sp.edit()
            .putString("theme", s.theme.name)
            .putFloat("icon_scale", s.iconScale)
            .putString("icon_shape", s.iconShape.name)
            .putBoolean("show_labels", s.showLabels)
            .putBoolean("hide_status", s.hideSystemStatusBar)
            .putBoolean("volume_hud", s.customVolumeHud)
            .putString("city", s.city)
            .putString("lat", s.lat.toString())
            .putString("lon", s.lon.toString())
            .apply()
    }

    fun loadWeather(): WeatherData? = try {
        val raw = sp.getString("weather", null)
        if (raw == null) null else {
            val j = JSONObject(raw)
            WeatherData(
                temp = j.getInt("temp"), min = j.getInt("min"), max = j.getInt("max"),
                code = j.getInt("code"), isDay = j.getBoolean("day"),
                city = j.getString("city"), updatedAt = j.getLong("at"),
            )
        }
    } catch (e: Exception) {
        null
    }

    fun saveWeather(w: WeatherData) {
        val j = JSONObject()
            .put("temp", w.temp).put("min", w.min).put("max", w.max)
            .put("code", w.code).put("day", w.isDay)
            .put("city", w.city).put("at", w.updatedAt)
        sp.edit().putString("weather", j.toString()).apply()
    }
}
