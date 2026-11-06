package io.github.jayfunc.carpelines.ui.glancewidget

import android.graphics.Bitmap
import android.util.LruCache

/** Decoded artwork is reused across Glance recompositions, which otherwise re-read the file. */
object AlbumArtBitmapCache {
    private const val CACHE_SIZE_BYTES = 4 * 1024 * 1024
    private val lruCache = object : LruCache<String, Bitmap>(CACHE_SIZE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun getBitmap(key: String): Bitmap? = lruCache.get(key)

    fun putBitmap(key: String, bitmap: Bitmap) {
        if (getBitmap(key) == null) lruCache.put(key, bitmap)
    }
}
