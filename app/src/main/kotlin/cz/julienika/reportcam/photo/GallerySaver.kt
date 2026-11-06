package cz.julienika.reportcam.photo

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/** Copies a finished photo into the shared gallery under Pictures/Report Cam. */
object GallerySaver {
    private const val ALBUM = "Report Cam"

    /**
     * Returns false if the gallery refused the write; the shared copy in the cache is unaffected. The gallery
     * row is named [displayName]: the file's own name, or the shared name of a kept copy (G-007).
     */
    fun save(
        context: Context,
        file: File,
        takenAtMillis: Long,
        displayName: String = file.name,
    ): Boolean {
        val resolver = context.contentResolver
        val values =
            ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATE_TAKEN, takenAtMillis)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$ALBUM")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                } else {
                    // Before Android 10 the row needs a real path; WRITE_EXTERNAL_STORAGE is held (maxSdk 28).
                    @Suppress("DEPRECATION")
                    val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), ALBUM)
                    dir.mkdirs()
                    @Suppress("DEPRECATION")
                    put(MediaStore.Images.Media.DATA, File(dir, displayName).absolutePath)
                }
            }
        val uri =
            try {
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            } catch (e: Exception) {
                null
            } ?: return false
        return try {
            resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
                ?: error("no output stream")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            }
            true
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            false
        }
    }
}
