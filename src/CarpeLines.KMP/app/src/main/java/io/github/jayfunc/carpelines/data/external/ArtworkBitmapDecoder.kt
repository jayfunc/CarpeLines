package io.github.jayfunc.carpelines.data.external

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.net.toUri

/**
 * Reads the cover a media session published as a URI. The app-wide scheme and the lyrics page both
 * sample this bitmap, so they must agree on how it is decoded; `file` and `content` are the only
 * schemes the session metadata legitimately points at.
 */
internal fun decodeArtworkBitmap(context: Context, rawUri: String): Bitmap? = runCatching {
    val uri = rawUri.toUri()
    if (uri.scheme != "content" && uri.scheme != "file") return@runCatching null
    context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
}.getOrNull()
