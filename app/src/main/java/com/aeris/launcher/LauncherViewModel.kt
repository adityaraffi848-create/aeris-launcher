package com.aeris.launcher

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aeris.launcher.data.AppItem
import com.aeris.launcher.data.AppRepository
import com.aeris.launcher.data.IconRepository
import com.aeris.launcher.data.ImageUtils
import com.aeris.launcher.data.LauncherSettings
import com.aeris.launcher.data.Prefs
import com.aeris.launcher.data.WeatherData
import com.aeris.launcher.data.WeatherRepository
import com.aeris.launcher.system.TorchController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

class LauncherViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx: Context = app.applicationContext
    private val prefs = Prefs(ctx)
    private val repo = AppRepository(ctx)
    private val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    val icons = IconRepository(ctx)
    val torch = TorchController(ctx)

    var apps by mutableStateOf<List<AppItem>>(emptyList()); private set
    var homeKeys by mutableStateOf(prefs.getList("home") ?: emptyList()); private set
    var dockKeys by mutableStateOf(prefs.getList("dock") ?: emptyList()); private set
    var settings by mutableStateOf(prefs.loadSettings()); private set
    var weather by mutableStateOf<WeatherData?>(prefs.loadWeather()); private set

    var iconVersion by mutableIntStateOf(0); private set
    var photoVersion by mutableIntStateOf(0); private set
    var homeSignal by mutableIntStateOf(0); private set

    var mediaVolume by mutableFloatStateOf(0f); private set
    var ringVolume by mutableFloatStateOf(0f); private set
    var hudTick by mutableIntStateOf(0); private set

    init {
        reloadApps()
        refreshVolumes()
    }

    // ---------------------------------------------------------------- aplikasi

    fun reloadApps() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { repo.loadApps() }
            if (list.isEmpty()) return@launch
            apps = list
            reconcile(list)
        }
    }

    private fun reconcile(list: List<AppItem>) {
        val valid = list.map { it.key }.toSet()
        if (!prefs.firstRunDone) {
            val dock = defaultDock(list)
            dockKeys = dock
            homeKeys = list.map { it.key }.filter { it !in dock }.take(24)
            prefs.firstRunDone = true
        } else {
            val known = (prefs.getList("known") ?: emptyList()).toSet()
            val added = list.map { it.key }.filter { it !in known && it !in dockKeys }
            homeKeys = homeKeys.filter { it in valid } + added
            dockKeys = dockKeys.filter { it in valid }
        }
        prefs.putList("known", valid.toList())
        persistLists()
    }

    private fun defaultDock(list: List<AppItem>): List<String> {
        val pm = ctx.packageManager
        val intents = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")),
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
        )
        val keys = intents.mapNotNull { i ->
            val pkg = pm.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
            list.firstOrNull { it.packageName == pkg }?.key
        }.distinct().toMutableList()
        for (a in list) {
            if (keys.size >= 4) break
            if (a.key !in keys) keys.add(a.key)
        }
        return keys.take(4)
    }

    private fun persistLists() {
        prefs.putList("home", homeKeys)
        prefs.putList("dock", dockKeys)
    }

    fun toggleDock(app: AppItem) {
        if (app.key in dockKeys) {
            dockKeys = dockKeys - app.key
        } else {
            if (dockKeys.size >= 4) { toast("Dock penuh (maksimal 4 aplikasi)"); return }
            dockKeys = dockKeys + app.key
            homeKeys = homeKeys - app.key
        }
        persistLists()
    }

    fun toggleHome(app: AppItem) {
        homeKeys = if (app.key in homeKeys) homeKeys - app.key else homeKeys + app.key
        persistLists()
    }

    fun launch(app: AppItem) {
        try {
            val i = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(app.component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            ctx.startActivity(i)
        } catch (e: Exception) {
            toast("Tidak bisa membuka ${app.label}")
        }
    }

    fun openAppInfo(app: AppItem) {
        try {
            ctx.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: Exception) { toast("Gagal membuka info aplikasi") }
    }

    fun uninstall(app: AppItem) {
        try {
            ctx.startActivity(
                Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: Exception) { toast("Gagal mencopot aplikasi") }
    }

    fun openSettings(action: String) {
        try {
            ctx.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) { toast("Pengaturan tidak tersedia di HP ini") }
    }

    fun openWifiPanel() = openSettings(
        if (Build.VERSION.SDK_INT >= 29) Settings.Panel.ACTION_INTERNET_CONNECTIVITY else Settings.ACTION_WIFI_SETTINGS,
    )

    fun openWallpaperPicker() {
        try {
            ctx.startActivity(
                Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Pilih wallpaper")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: Exception) { toast("Gagal membuka pemilih wallpaper") }
    }

    fun onHomePressed() { homeSignal++ }

    // ---------------------------------------------------------------- ikon custom

    fun setCustomIcon(app: AppItem, uri: Uri) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { icons.setCustom(app, uri) }
            if (ok) iconVersion++ else toast("Gagal memuat gambar")
        }
    }

    fun resetIcon(app: AppItem) {
        icons.reset(app)
        iconVersion++
    }

    // ---------------------------------------------------------------- pengaturan

    fun updateSettings(block: (LauncherSettings) -> LauncherSettings) {
        settings = block(settings)
        prefs.saveSettings(settings)
    }

    // ---------------------------------------------------------------- cuaca

    fun refreshWeather(force: Boolean = false) {
        val w = weather
        if (!force && w != null && System.currentTimeMillis() - w.updatedAt < 30 * 60 * 1000) return
        viewModelScope.launch {
            val s = settings
            val r = withContext(Dispatchers.IO) { WeatherRepository.fetch(s.lat, s.lon, s.city) }
            if (r != null) {
                weather = r
                prefs.saveWeather(r)
            } else if (force) toast("Gagal memuat cuaca. Cek koneksi internet.")
        }
    }

    fun changeCity(name: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val g = withContext(Dispatchers.IO) { WeatherRepository.geocode(name.trim()) }
            if (g != null) {
                updateSettings { it.copy(city = g.first, lat = g.second, lon = g.third) }
                refreshWeather(true)
                onResult(true)
            } else onResult(false)
        }
    }

    // ---------------------------------------------------------------- foto widget

    fun photoFile() = File(ctx.filesDir, "widget_photo.jpg")

    fun setPhoto(uri: Uri) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                val bmp = ImageUtils.decodeFit(ctx, uri, 1280) ?: return@withContext false
                try {
                    FileOutputStream(photoFile()).use { bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it) }
                    true
                } catch (e: Exception) { false }
            }
            if (ok) photoVersion++ else toast("Gagal memuat foto")
        }
    }

    fun clearPhoto() {
        if (photoFile().delete()) {
            photoVersion++
            toast("Foto widget dihapus")
        }
    }

    // ---------------------------------------------------------------- volume

    private fun frac(stream: Int): Float {
        val max = am.getStreamMaxVolume(stream)
        return if (max > 0) am.getStreamVolume(stream).toFloat() / max else 0f
    }

    fun refreshVolumes() {
        mediaVolume = frac(AudioManager.STREAM_MUSIC)
        ringVolume = frac(AudioManager.STREAM_RING)
    }

    private fun setStream(stream: Int, f: Float) {
        try {
            val max = am.getStreamMaxVolume(stream)
            am.setStreamVolume(stream, (f.coerceIn(0f, 1f) * max).roundToInt(), 0)
        } catch (e: SecurityException) {
            // mode jangan ganggu bisa menolak perubahan volume dering
        }
        refreshVolumes()
    }

    fun setMediaVolume(f: Float) = setStream(AudioManager.STREAM_MUSIC, f)
    fun setRingVolume(f: Float) = setStream(AudioManager.STREAM_RING, f)

    fun onVolumeKey(up: Boolean) {
        am.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
            0,
        )
        refreshVolumes()
        hudTick++
    }

    fun touchHud() { hudTick++ }

    // ---------------------------------------------------------------- util

    fun toast(msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

    override fun onCleared() {
        torch.release()
        super.onCleared()
    }
}
