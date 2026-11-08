package io.github.jayfunc.carpelines.utils

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import java.io.File

/**
 * Maps a folder picked with the system `OpenDocumentTree` picker back to the absolute path
 * MediaStore reports for the same directory.
 *
 * The local scanner works on MediaStore `DATA` paths, so it cannot consume the tree URI itself.
 * Primary storage ("primary:Music") and removable volumes ("1234-ABCD:Music") are the two shapes
 * the picker returns for folders the scanner can see; anything else belongs to another provider
 * and yields null.
 */
fun Context.resolveTreeUriPath(uri: Uri): String? = runCatching {
    val documentId = DocumentsContract.getTreeDocumentId(uri)
    val (volumeId, relativePath) = documentId.split(':', limit = 2)
        .takeIf { it.size == 2 } ?: return@runCatching null

    val volumeRoot = when {
        volumeId.equals("primary", ignoreCase = true) ->
            Environment.getExternalStorageDirectory()
        else -> storageVolumeRoot(volumeId)
    } ?: return@runCatching null

    File(volumeRoot, relativePath).absolutePath
}.getOrNull()

private fun Context.storageVolumeRoot(volumeId: String): File? {
    val storageManager = getSystemService(StorageManager::class.java) ?: return null
    val volume = storageManager.storageVolumes.firstOrNull { volume ->
        volume.uuid?.equals(volumeId, ignoreCase = true) == true
    } ?: return null

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        return volume.directory
    }
    @Suppress("DEPRECATION")
    return File("/storage/${volume.uuid}")
}
