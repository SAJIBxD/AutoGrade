package com.autograde.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

/**
 * Copies a picked content:// [uri] into the cache directory so it can be sent
 * as a multipart body. Returns null when the stream cannot be read.
 *
 * The original display name is kept (sanitised) so the server-side filename —
 * and therefore the MIME sniffing done by the backend — stays meaningful.
 */
fun Context.uriToFile(uri: Uri, prefix: String = "upload"): File? {
    return try {
        val displayName = queryDisplayName(uri) ?: "${prefix}_${System.currentTimeMillis()}"
        val safe = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "${prefix}_file" }
        val out = File(cacheDir, "${prefix}_${System.currentTimeMillis()}_$safe")
        contentResolver.openInputStream(uri)?.use { input ->
            out.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        out.takeIf { it.exists() && it.length() > 0 }
    } catch (_: Exception) {
        null
    }
}

private fun Context.queryDisplayName(uri: Uri): String? = try {
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
    }
} catch (_: Exception) {
    null
}

/** Human readable size, e.g. `1.2 MB` / `240 KB`. */
fun humanSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024 -> "%d KB".format(bytes / 1024)
    else -> "$bytes B"
}
