package io.github.jayfunc.carpelines.presentation.components.lyrics.card

import android.app.Activity
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.core.content.FileProvider
import java.io.File
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Captures the composed card layer and hands it to the gallery or the system share sheet. */
internal object LyricsCardExporter {

    suspend fun share(context: Context, layer: GraphicsLayer, chooserTitle: String): Boolean =
        runCatching {
            val bitmap = capture(layer)
            val uri = withContext(Dispatchers.IO) { writeCache(context, bitmap) }
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri(null, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(send, chooserTitle)
            if (context !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            true
        }.getOrDefault(false)

    suspend fun save(context: Context, layer: GraphicsLayer): Boolean =
        runCatching {
            val bitmap = capture(layer)
            withContext(Dispatchers.IO) { writeToPictures(context, bitmap) }
            true
        }.getOrDefault(false)

    /** Recorded layers come back as hardware bitmaps, which cannot be PNG-encoded directly. */
    private suspend fun capture(layer: GraphicsLayer): Bitmap {
        val captured = layer.toImageBitmap().asAndroidBitmap()
        return if (captured.config == Bitmap.Config.HARDWARE) {
            captured.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            captured
        }
    }

    private fun writeCache(context: Context, bitmap: Bitmap): android.net.Uri {
        val directory = File(context.cacheDir, "lyrics_cards").apply { mkdirs() }
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        directory.listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < cutoff) file.delete()
        }
        val file = File(directory, "carpelines-lyrics-${System.currentTimeMillis()}.png")
        file.outputStream().use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) { "PNG encoding failed" }
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }

    private fun writeToPictures(context: Context, bitmap: Bitmap) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "carpelines-lyrics-${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/CarpeLines",
                )
            }
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("MediaStore rejected the image")
        context.contentResolver.openOutputStream(uri)?.use { stream: OutputStream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) { "PNG encoding failed" }
        } ?: error("Could not open the MediaStore stream")
    }
}
