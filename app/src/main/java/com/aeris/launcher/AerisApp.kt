package com.aeris.launcher

import android.app.Application
import android.util.Log
import java.io.File

class AerisApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                var root: Throwable = error
                while (root.cause != null && root.cause !== root) root = root.cause!!
                val summary = "PENYEBAB: ${root.javaClass.name}: ${root.message}\n" +
                    root.stackTrace.take(8).joinToString("\n") { "   di $it" }
                File(filesDir, "crash.txt").writeText(
                    summary + "\n\nThread: ${thread.name}\n\n=== LENGKAP ===\n" + Log.getStackTraceString(error),
                )
            } catch (t: Throwable) {
                // jangan sampai pencatat crash ikut crash
            }
            previous?.uncaughtException(thread, error)
        }
    }
}
