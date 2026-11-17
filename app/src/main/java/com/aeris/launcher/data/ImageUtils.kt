package com.aeris.launcher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri

object ImageUtils {

    private fun rotation(context: Context, uri: Uri): Int = try {
        context.contentResolver.openInputStream(uri)?.use { s ->
            when (ExifInterface(s).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: 0
    } catch (e: Exception) {
        0
    }

    /** Decode gambar, kecilkan sampai sisi terpanjang <= [maxSide], putar sesuai EXIF. */
    fun decodeFit(context: Context, uri: Uri, maxSide: Int): Bitmap? {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val src = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return null
            val scale = maxSide.toFloat() / maxOf(src.width, src.height)
            val m = Matrix()
            if (scale < 1f) m.postScale(scale, scale)
            val rot = rotation(context, uri)
            if (rot != 0) m.postRotate(rot.toFloat())
            return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
        } catch (e: Exception) {
            return null
        }
    }

    /** Crop tengah jadi persegi lalu skala ke [target] px (untuk ikon custom). */
    fun decodeSquare(context: Context, uri: Uri, target: Int): Bitmap? {
        val src = decodeFit(context, uri, target * 3) ?: return null
        return try {
            val side = minOf(src.width, src.height)
            val x = (src.width - side) / 2
            val y = (src.height - side) / 2
            val cropped = Bitmap.createBitmap(src, x, y, side, side)
            Bitmap.createScaledBitmap(cropped, target, target, true)
        } catch (e: Exception) {
            null
        }
    }
}
