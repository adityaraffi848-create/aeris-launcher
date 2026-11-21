package com.aeris.launcher

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.snapshotFlow
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.aeris.launcher.ui.LauncherScreen
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    private val vm: LauncherViewModel by viewModels()
    private var crashMode = false
    private var receiverRegistered = false

    private val pkgReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) = vm.reloadApps()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Kalau sebelumnya crash, tampilkan laporannya (bukan UI biasa)
        val crashFile = File(filesDir, "crash.txt")
        if (crashFile.exists()) {
            showCrashReport(crashFile)
            return
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false

        val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusPx = if (resId > 0) resources.getDimensionPixelSize(resId) else 0
        val statusDp = if (statusPx > 0) statusPx / resources.displayMetrics.density else 28f

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        try {
            registerReceiver(pkgReceiver, filter)
            receiverRegistered = true
        } catch (e: Exception) {
            // tanpa receiver, daftar aplikasi hanya diperbarui saat launcher dibuka ulang
        }

        // tombol Home ditekan saat launcher sudah terbuka -> kembali ke halaman utama
        addOnNewIntentListener { intent ->
            if (intent.hasCategory(Intent.CATEGORY_HOME)) vm.onHomePressed()
        }

        lifecycleScope.launch {
            snapshotFlow { vm.settings.hideSystemStatusBar }.collect { applySystemBars(it) }
        }

        setContent { LauncherScreen(vm, statusDp) }
    }

    private fun showCrashReport(file: File) {
        crashMode = true
        val trace = try { file.readText() } catch (e: Exception) { "(laporan tidak bisa dibaca)" }
        val pad = (16 * resources.displayMetrics.density).toInt()

        val title = TextView(this).apply {
            text = "Aeris berhenti karena error"
            textSize = 18f
            setTextColor(Color.WHITE)
        }
        val hint = TextView(this).apply {
            text = "Screenshot layar ini (bagian atas) lalu kirim ke Claude."
            textSize = 13f
            setTextColor(0xFF9AA0A6.toInt())
            setPadding(0, pad / 2, 0, pad / 2)
        }
        val copy = Button(this).apply {
            text = "Salin laporan"
            setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("crash", trace))
            }
        }
        val retry = Button(this).apply {
            text = "Hapus laporan & coba lagi"
            setOnClickListener { file.delete(); recreate() }
        }
        val body = TextView(this).apply {
            text = trace
            textSize = 10f
            typeface = Typeface.MONOSPACE
            setTextColor(0xFFDDDDDD.toInt())
            setTextIsSelectable(true)
            setPadding(0, pad, 0, 0)
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 3, pad, pad)
            addView(title); addView(hint); addView(copy); addView(retry); addView(body)
        }
        setContentView(ScrollView(this).apply {
            setBackgroundColor(0xFF101014.toInt())
            addView(col)
        })
    }

    private fun applySystemBars(hideStatus: Boolean) {
        val c = WindowInsetsControllerCompat(window, window.decorView)
        c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (hideStatus) c.hide(WindowInsetsCompat.Type.statusBars()) else c.show(WindowInsetsCompat.Type.statusBars())
    }

    override fun onResume() {
        super.onResume()
        if (crashMode) return
        applySystemBars(vm.settings.hideSystemStatusBar)
        vm.refreshVolumes()
        vm.refreshWeather()
    }

    /** Tombol volume diambil alih supaya muncul pop up volume vertikal milik Aeris. */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (crashMode) return super.dispatchKeyEvent(event)
        val code = event.keyCode
        if (vm.settings.customVolumeHud && (code == KeyEvent.KEYCODE_VOLUME_UP || code == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            if (event.action == KeyEvent.ACTION_DOWN) vm.onVolumeKey(code == KeyEvent.KEYCODE_VOLUME_UP)
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        if (receiverRegistered) {
            try { unregisterReceiver(pkgReceiver) } catch (e: Exception) { }
        }
        super.onDestroy()
    }
}
