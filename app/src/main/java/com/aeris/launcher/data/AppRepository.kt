package com.aeris.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.net.Uri
import android.util.LruCache
import java.io.File
import java.io.FileOutputStream

val CATEGORY_ORDER = listOf("Sosial", "Hiburan", "Utilitas", "Belanja & Uang", "Game", "Lainnya")

class AppRepository(private val context: Context) {
    private val pm = context.packageManager

    fun loadApps(): List<AppItem> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .map { ri ->
                val ai = ri.activityInfo
                val cn = ComponentName(ai.packageName, ai.name)
                AppItem(
                    key = cn.flattenToString(),
                    packageName = ai.packageName,
                    className = ai.name,
                    label = ri.loadLabel(pm).toString(),
                    category = categorize(ai.packageName, ai.applicationInfo),
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    private fun has(p: String, words: List<String>) = words.any { p.contains(it) }

    private fun categorize(pkg: String, info: ApplicationInfo): String {
        val p = pkg.lowercase()
        val c = info.category
        return when {
            c == ApplicationInfo.CATEGORY_GAME -> "Game"
            c == ApplicationInfo.CATEGORY_SOCIAL ||
                has(p, listOf("whatsapp", "telegram", "instagram", "facebook", "twitter", "tiktok", "discord", "messenger", "line.android", "snapchat", "mms", "messaging")) -> "Sosial"
            c == ApplicationInfo.CATEGORY_AUDIO || c == ApplicationInfo.CATEGORY_VIDEO || c == ApplicationInfo.CATEGORY_IMAGE ||
                has(p, listOf("music", "spotify", "youtube", "netflix", "video", "player", "camera", "gallery", "photo")) -> "Hiburan"
            has(p, listOf("bank", "pay", "gojek", "grab", "shopee", "tokopedia", "dana", "ovo", "lazada", "bca", "mandiri", "bri", "wallet")) -> "Belanja & Uang"
            c == ApplicationInfo.CATEGORY_PRODUCTIVITY || c == ApplicationInfo.CATEGORY_NEWS || c == ApplicationInfo.CATEGORY_MAPS ||
                has(p, listOf("settings", "calculator", "clock", "files", "contacts", "dialer", "phone", "calendar", "chrome", "browser", "mail", "notes")) -> "Utilitas"
            else -> "Lainnya"
        }
    }
}

/** Memuat ikon aplikasi (atau ikon custom buatan pengguna) sebagai bitmap persegi. */
class IconRepository(private val context: Context) {
    private val pm = context.packageManager
    private val cache = LruCache<String, Bitmap>(160)
    private val dir = File(context.filesDir, "icons").apply { mkdirs() }
    private val px = 192

    private fun customFile(app: AppItem) = File(dir, app.key.replace(Regex("[^A-Za-z0-9._-]"), "_") + ".png")

    fun hasCustom(app: AppItem) = customFile(app).exists()

    fun load(app: AppItem): Bitmap {
        cache.get(app.key)?.let { return it }
        val f = customFile(app)
        val bmp = (if (f.exists()) BitmapFactory.decodeFile(f.path) else null) ?: renderSystemIcon(app)
        cache.put(app.key, bmp)
        return bmp
    }

    fun setCustom(app: AppItem, uri: Uri): Boolean {
        val bmp = ImageUtils.decodeSquare(context, uri, 256) ?: return false
        return try {
            FileOutputStream(customFile(app)).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            cache.remove(app.key)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun reset(app: AppItem) {
        customFile(app).delete()
        cache.remove(app.key)
    }

    private fun renderSystemIcon(app: AppItem): Bitmap {
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        try {
            drawSystemIcon(app, bmp)
        } catch (e: Exception) {
            Canvas(bmp).drawColor(0xFFE5E5EA.toInt())
        }
        return bmp
    }

    private fun drawSystemIcon(app: AppItem, bmp: Bitmap) {
        val d = try { pm.getActivityIcon(app.component) } catch (e: Exception) { pm.defaultActivityIcon }
        val c = Canvas(bmp)
        if (d is AdaptiveIconDrawable) {
            // gambar layer penuh (tanpa mask bawaan HP) supaya bentuk ikon seragam
            val off = (px * 0.25f).toInt()
            d.background?.let { it.setBounds(-off, -off, px + off, px + off); it.draw(c) }
            d.foreground?.let { it.setBounds(-off, -off, px + off, px + off); it.draw(c) }
        } else {
            c.drawColor(0xFFF2F2F7.toInt())
            val pad = (px * 0.15f).toInt()
            d.setBounds(pad, pad, px - pad, px - pad)
            d.draw(c)
        }
    }
}
