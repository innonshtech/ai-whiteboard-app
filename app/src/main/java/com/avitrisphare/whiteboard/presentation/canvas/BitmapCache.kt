package com.avitrisphare.whiteboard.presentation.canvas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import java.io.File

/**
 * Thread-safe memory cache for decoded bitmaps (images, custom backgrounds, sticker icons).
 * Prevents redundant disk I/O and BitmapFactory decoding on the UI render thread.
 */
object BitmapCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    // Use 1/8th of available runtime memory for bitmap cache
    private val cacheSize = (maxMemory / 8).coerceIn(16 * 1024, 64 * 1024)

    private val lruCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun get(key: String): Bitmap? {
        if (key.isBlank()) return null
        synchronized(lruCache) {
            val bmp = lruCache.get(key)
            if (bmp != null && !bmp.isRecycled) {
                return bmp
            }
            if (bmp != null && bmp.isRecycled) {
                lruCache.remove(key)
            }
            return null
        }
    }

    fun put(key: String, bitmap: Bitmap) {
        if (key.isBlank() || bitmap.isRecycled) return
        synchronized(lruCache) {
            lruCache.put(key, bitmap)
        }
    }

    fun decodeSampledBitmap(
        context: Context,
        uriString: String,
        maxDim: Int = 2048
    ): Bitmap? {
        if (uriString.isBlank()) return null
        val cached = get(uriString)
        if (cached != null) return cached

        return try {
            val openStream: () -> java.io.InputStream? = {
                when {
                    uriString.startsWith("/") -> File(uriString).inputStream()
                    uriString.startsWith("file://") -> File(Uri.parse(uriString).path ?: uriString).inputStream()
                    uriString.startsWith("content://") -> context.contentResolver.openInputStream(Uri.parse(uriString))
                    else -> {
                        val file = File(uriString)
                        if (file.exists()) file.inputStream()
                        else context.contentResolver.openInputStream(Uri.parse(uriString))
                    }
                }
            }

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            openStream()?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sample = 1
            while (bounds.outWidth / sample > maxDim || bounds.outHeight / sample > maxDim) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = openStream()?.use { BitmapFactory.decodeStream(it, null, opts) }
            if (decoded != null) {
                put(uriString, decoded)
            }
            decoded
        } catch (_: Exception) {
            null
        }
    }

    fun clear() {
        synchronized(lruCache) {
            lruCache.evictAll()
        }
    }
}

