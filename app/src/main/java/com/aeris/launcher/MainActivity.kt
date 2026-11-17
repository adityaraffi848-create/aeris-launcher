package com.aeris.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.snapshotFlow
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.aeris.launcher.ui.LauncherScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: LauncherViewModel by viewModels()

    private val pkgReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) = vm.reloadApps()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        ContextCompat.registerReceiver(this, pkgReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        // tombol Home ditekan saat launcher sudah terbuka -> kembali ke halaman utama
        addOnNewIntentListener { intent ->
            if (intent.hasCategory(Intent.CATEGORY_HOME)) vm.onHomePressed()
        }

        lifecycleScope.launch {
            snapshotFlow { vm.settings.hideSystemStatusBar }.collect { applySystemBars(it) }
        }

        setContent { LauncherScreen(vm, statusDp) }
    }

    private fun applySystemBars(hideStatus: Boolean) {
        val c = WindowInsetsControllerCompat(window, window.decorView)
        c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (hideStatus) c.hide(WindowInsetsCompat.Type.statusBars()) else c.show(WindowInsetsCompat.Type.statusBars())
    }

    override fun onResume() {
        super.onResume()
        applySystemBars(vm.settings.hideSystemStatusBar)
        vm.refreshVolumes()
        vm.refreshWeather()
    }

    /** Tombol volume diambil alih supaya muncul pop up volume vertikal milik Aeris. */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val code = event.keyCode
        if (vm.settings.customVolumeHud && (code == KeyEvent.KEYCODE_VOLUME_UP || code == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            if (event.action == KeyEvent.ACTION_DOWN) vm.onVolumeKey(code == KeyEvent.KEYCODE_VOLUME_UP)
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        try { unregisterReceiver(pkgReceiver) } catch (e: Exception) { }
        super.onDestroy()
    }
}
