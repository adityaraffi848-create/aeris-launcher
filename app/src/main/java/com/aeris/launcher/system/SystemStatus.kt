package com.aeris.launcher.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Status sistem yang dipantau untuk status bar ala iOS. */
@Stable
class SystemStatus {
    var timeText by mutableStateOf("")
    var battery by mutableIntStateOf(100)
    var charging by mutableStateOf(false)
    var wifi by mutableStateOf(false)
    var wifiLevel by mutableIntStateOf(0)      // 0..3
    var cellular by mutableStateOf(false)
    var cellLevel by mutableIntStateOf(0)      // 0..4
}

@Suppress("DEPRECATION")
private fun readNet(context: Context, st: SystemStatus) {
    try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        st.wifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        st.cellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        if (st.wifi) {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val rssi = wm.connectionInfo?.rssi ?: -100
            st.wifiLevel = when {
                rssi >= -60 -> 3
                rssi >= -72 -> 2
                else -> 1
            }
        }
    } catch (e: Exception) {
        // abaikan; status jaringan tidak kritis
    }
}

@Suppress("DEPRECATION")
@Composable
fun rememberSystemStatus(): SystemStatus {
    val context = LocalContext.current
    val st = remember { SystemStatus() }

    DisposableEffect(Unit) {
        val pattern = if (DateFormat.is24HourFormat(context)) "H:mm" else "h:mm"
        val fmt = SimpleDateFormat(pattern, Locale.getDefault())
        fun updateTime() { st.timeText = fmt.format(Date()) }
        updateTime()

        val timeRx = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) = updateTime()
        }
        val timeFilter = IntentFilter(Intent.ACTION_TIME_TICK).apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        try { context.registerReceiver(timeRx, timeFilter) } catch (e: Exception) { }

        val batRx = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                if (i == null) return
                val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                if (level >= 0 && scale > 0) st.battery = level * 100 / scale
                val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                st.charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
        val sticky = try {
            context.registerReceiver(batRx, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        } catch (e: Exception) {
            null
        }
        if (sticky != null) batRx.onReceive(context, sticky)

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netCb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = readNet(context, st)
            override fun onLost(network: Network) = readNet(context, st)
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = readNet(context, st)
        }
        try { cm.registerDefaultNetworkCallback(netCb) } catch (e: Exception) { }
        readNet(context, st)

        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val phoneListener = object : PhoneStateListener() {
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                st.cellLevel = signalStrength?.level ?: 0
            }
        }
        try { tm?.listen(phoneListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS) } catch (e: Exception) { }

        onDispose {
            try { context.unregisterReceiver(timeRx) } catch (e: Exception) { }
            try { context.unregisterReceiver(batRx) } catch (e: Exception) { }
            try { cm.unregisterNetworkCallback(netCb) } catch (e: Exception) { }
            try { tm?.listen(phoneListener, PhoneStateListener.LISTEN_NONE) } catch (e: Exception) { }
        }
    }

    // kekuatan sinyal wifi berubah tanpa broadcast, jadi dicek berkala
    LaunchedEffect(Unit) {
        while (true) {
            readNet(context, st)
            delay(5000)
        }
    }
    return st
}
