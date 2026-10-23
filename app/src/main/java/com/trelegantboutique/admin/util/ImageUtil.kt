package com.trelegantboutique.admin.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream

/** Shrinks a photo picked from the phone to a JPEG no larger than ~1600 px, so uploads are quick. */
object ImageUtil {

    fun toJpeg(context: Context, uri: Uri, maxSide: Int = 1600): ByteArray? {
        return try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options()
            bounds.inJustDecodeBounds = true
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / sample > maxSide * 2 || bounds.outHeight / sample > maxSide * 2) sample *= 2
            val opts = BitmapFactory.Options()
            opts.inSampleSize = sample
            val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null

            var degrees = 0
            try {
                resolver.openInputStream(uri)?.use { s ->
                    val exif = ExifInterface(s)
                    val o = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    degrees = when (o) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    }
                }
            } catch (e: Exception) {
                degrees = 0
            }

            val longest = Math.max(decoded.width, decoded.height)
            val scale = if (longest > maxSide) maxSide.toFloat() / longest.toFloat() else 1f
            val matrix = Matrix()
            if (scale < 1f) matrix.postScale(scale, scale)
            if (degrees != 0) matrix.postRotate(degrees.toFloat())
            val finalBmp = if (scale < 1f || degrees != 0) {
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            } else decoded

            val out = ByteArrayOutputStream()
            finalBmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
            out.toByteArray()
        } catch (e: Throwable) {
            null
        }
    }
}

/** Main colour of a photo on the phone (0xRRGGBB), or null if it cannot be read. */
fun dominantColorOf(context: android.content.Context, uri: android.net.Uri): Int? {
    return try {
        val opts = android.graphics.BitmapFactory.Options()
        opts.inSampleSize = 8
        val bmp = context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, opts) } ?: return null
        dominantColorOf(bmp)
    } catch (e: Throwable) {
        null
    }
}

/** Main colour of a bitmap (0xRRGGBB): looks at a 44 x 44 copy, like the website. */
fun dominantColorOf(bitmap: android.graphics.Bitmap): Int {
    val src = if (bitmap.config == android.graphics.Bitmap.Config.HARDWARE) bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false) else bitmap
    val small = android.graphics.Bitmap.createScaledBitmap(src, 44, 44, true)
    val px = IntArray(44 * 44)
    small.getPixels(px, 0, 44, 0, 0, 44, 44)
    return ColorTools.dominant(px)
}

/** Main colour of a photo already on the website (by its address), using Coil to fetch it. */
suspend fun dominantColorOfUrl(context: android.content.Context, url: String): Int? {
    if (url.isBlank()) return null
    return try {
        val loader = coil.ImageLoader(context)
        val req = coil.request.ImageRequest.Builder(context).data(url).allowHardware(false).build()
        val result = loader.execute(req)
        val d = result.drawable as? android.graphics.drawable.BitmapDrawable ?: return null
        dominantColorOf(d.bitmap)
    } catch (e: Throwable) {
        null
    }
}
