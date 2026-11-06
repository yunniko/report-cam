package cz.julienika.reportcam.helpers

import android.content.Context
import android.provider.MediaStore
import org.junit.Assert.assertEquals

/** Gallery rows named [name] (GallerySaver writes one per saved version, D017). */
fun Context.galleryCopies(name: String): Int = galleryCount("${MediaStore.Images.Media.DISPLAY_NAME}=?", arrayOf(name))

/** Gallery rows taken at [takenAtMillis]: finds a copy whose name the test cannot know in advance. */
fun Context.galleryCopiesTakenAt(takenAtMillis: Long): Int =
    galleryCount("${MediaStore.Images.Media.DATE_TAKEN}=?", arrayOf(takenAtMillis.toString()))

private fun Context.galleryCount(
    where: String,
    args: Array<String>,
): Int =
    contentResolver
        .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Images.Media._ID), where, args, null)!!
        .use { it.count }

/** Waits for the gallery writes in flight, then checks no more arrive. */
fun Context.assertGalleryCopies(
    expected: Int,
    name: String,
) {
    val end = System.currentTimeMillis() + 5_000
    while (galleryCopies(name) < expected && System.currentTimeMillis() < end) Thread.sleep(100)
    Thread.sleep(1_000)
    assertEquals("gallery copies of $name", expected, galleryCopies(name))
}

/** The app owns its gallery rows, so a test may remove the ones it made. */
fun Context.deleteGalleryCopies(names: Iterable<String>) {
    for (name in names) {
        contentResolver.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "${MediaStore.Images.Media.DISPLAY_NAME}=?", arrayOf(name))
    }
}

fun Context.deleteGalleryCopiesTakenAt(takenAtMillis: Long) {
    contentResolver.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "${MediaStore.Images.Media.DATE_TAKEN}=?", arrayOf(takenAtMillis.toString()))
}
