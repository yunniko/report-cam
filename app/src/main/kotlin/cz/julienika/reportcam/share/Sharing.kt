package cz.julienika.reportcam.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.FileProvider
import java.io.File

object Sharing {
    fun isInstalled(
        context: Context,
        pkg: String,
    ): Boolean =
        try {
            context.packageManager.getPackageInfo(pkg, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

    /** An ACTION_SEND for one JPEG, with the stamp text as caption; [pkg] null means "let the user choose". */
    fun sendIntent(
        context: Context,
        file: File,
        caption: String?,
        pkg: String?,
    ): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val send =
            Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                if (!caption.isNullOrBlank()) putExtra(Intent.EXTRA_TEXT, caption)
                // ClipData carries the read grant through the chooser to whichever app is picked.
                clipData = ClipData.newRawUri(null, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (pkg != null) setPackage(pkg)
            }
        return if (pkg != null) send else Intent.createChooser(send, null)
    }
}

/** Shared copies live in the cache only long enough for the messenger to read them. */
object SharedFiles {
    const val MAX_AGE_MS = 24 * 60 * 60 * 1000L

    /** Deletes photos older than [maxAgeMs]; returns how many went. */
    fun prune(
        dir: File,
        nowMillis: Long,
        maxAgeMs: Long = MAX_AGE_MS,
    ): Int {
        val old = dir.listFiles()?.filter { it.isFile && nowMillis - it.lastModified() > maxAgeMs }.orEmpty()
        return old.count { it.delete() }
    }
}
