package com.aeris.launcher.system

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class TorchController(context: Context) {
    private val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private val camId: String? = try {
        cm.cameraIdList.firstOrNull { id ->
            val ch = cm.getCameraCharacteristics(id)
            ch.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                ch.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }
    } catch (e: Exception) {
        null
    }

    var on by mutableStateOf(false)
        private set

    val available: Boolean get() = camId != null

    private val callback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == camId) on = enabled
        }
    }

    init {
        try { cm.registerTorchCallback(callback, null) } catch (e: Exception) { }
    }

    fun toggle() {
        val id = camId ?: return
        try { cm.setTorchMode(id, !on) } catch (e: Exception) { }
    }

    fun release() {
        try { cm.unregisterTorchCallback(callback) } catch (e: Exception) { }
    }
}
