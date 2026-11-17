package com.aeris.launcher.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Cuaca dari Open-Meteo (gratis, tanpa API key). */
object WeatherRepository {

    fun fetch(lat: Double, lon: Double, city: String): WeatherData? = try {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,weather_code,is_day" +
            "&daily=temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=1"
        val json = JSONObject(httpGet(url))
        val cur = json.getJSONObject("current")
        val daily = json.getJSONObject("daily")
        WeatherData(
            temp = Math.round(cur.getDouble("temperature_2m")).toInt(),
            max = Math.round(daily.getJSONArray("temperature_2m_max").getDouble(0)).toInt(),
            min = Math.round(daily.getJSONArray("temperature_2m_min").getDouble(0)).toInt(),
            code = cur.getInt("weather_code"),
            isDay = cur.getInt("is_day") == 1,
            city = city,
            updatedAt = System.currentTimeMillis(),
        )
    } catch (e: Exception) {
        null
    }

    /** Cari koordinat kota. Hasil: (nama, lat, lon). */
    fun geocode(name: String): Triple<String, Double, Double>? = try {
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=" +
            URLEncoder.encode(name, "UTF-8") + "&count=1&language=id"
        val results = JSONObject(httpGet(url)).optJSONArray("results")
        if (results == null || results.length() == 0) null else {
            val r = results.getJSONObject(0)
            Triple(r.getString("name"), r.getDouble("latitude"), r.getDouble("longitude"))
        }
    } catch (e: Exception) {
        null
    }

    private fun httpGet(u: String): String {
        val c = URL(u).openConnection() as HttpURLConnection
        c.connectTimeout = 8000
        c.readTimeout = 8000
        return try {
            c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    fun describe(code: Int): String = when (code) {
        0 -> "Cerah"
        1, 2 -> "Cerah Berawan"
        3 -> "Berawan"
        45, 48 -> "Berkabut"
        in 51..57 -> "Gerimis"
        in 61..67 -> "Hujan"
        in 71..77, 85, 86 -> "Salju"
        in 80..82 -> "Hujan Lokal"
        in 95..99 -> "Badai Petir"
        else -> "—"
    }

    fun emoji(code: Int, isDay: Boolean): String = when (code) {
        0 -> if (isDay) "☀️" else "🌙"
        1, 2 -> if (isDay) "⛅" else "☁️"
        3 -> "☁️"
        45, 48 -> "🌫️"
        in 51..67, in 80..82 -> "🌧️"
        in 71..77, 85, 86 -> "❄️"
        in 95..99 -> "⛈️"
        else -> "🌡️"
    }
}
