package cz.julienika.reportcam.photo

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.text.Html
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cz.julienika.reportcam.settings.AppSettings
import cz.julienika.reportcam.settings.SettingsSource
import cz.julienika.reportcam.share.Sharing
import cz.julienika.reportcam.stamp.GeoFix
import cz.julienika.reportcam.stamp.LocationReading
import cz.julienika.reportcam.stamp.StampData
import cz.julienika.reportcam.stamp.StampKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.ZonedDateTime

/** The metadata and share intent as Android itself reads them, on the real runtime. */
@RunWith(AndroidJUnit4::class)
class MetadataRoundTripTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dir = File(context.cacheDir, "shared").apply { mkdirs() }
    private val noSettings =
        object : SettingsSource {
            override fun string(
                key: String,
                default: String,
            ) = default

            override fun bool(
                key: String,
                default: Boolean,
            ) = default

            override fun stringSet(
                key: String,
                default: Set<String>,
            ) = default
        }

    private fun jpeg(): File {
        val f = File(dir, "roundtrip_test.jpg")
        val bmp = Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GRAY) }
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        return f
    }

    private val text = "Žižkov – dvůr č. 4"

    private fun data(reading: LocationReading?): StampData {
        val now = ZonedDateTime.now()
        return StampData.EMPTY
            .with(StampKeys.CAPTURE_TIME, now)
            .with(StampKeys.USER_TEXT, text)
            .with(StampKeys.LOCATION, reading)
    }

    @Test
    fun exifAndXmpReadBackWithFullText() {
        val spec = AppSettings.from(noSettings).stampSpec
        val fix = GeoFix(50.087654, 14.421329, 6.5f, System.currentTimeMillis() - 4_000, false)
        val d = data(LocationReading.Fix(fix))
        val file = jpeg()
        ExifWriter.write(file, PhotoMetadata.of(spec, d))

        val exif = ExifInterface(file)
        val latLong = exif.latLong
        assertNotNull(latLong)
        assertEquals(50.087654, latLong!![0], 1e-5)
        assertEquals(14.421329, latLong[1], 1e-5)
        assertEquals(6.5, exif.getAttributeDouble(ExifInterface.TAG_GPS_H_POSITIONING_ERROR, 0.0), 0.01)
        assertNotNull(exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL))
        assertNotNull(exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL))
        assertTrue(exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION)!!.startsWith("Zizkov - dvur c. 4 | "))

        val xmp = String(exif.getAttributeBytes(ExifInterface.TAG_XMP)!!, Charsets.US_ASCII)
        val description = xmp.substringAfter("<rdf:li xml:lang=\"x-default\">").substringBefore("</rdf:li>")
        @Suppress("DEPRECATION") // the single-argument form is the one available at minSdk 26
        val decoded = Html.fromHtml(description).toString()
        assertTrue(decoded, decoded.startsWith(text))
        assertTrue(decoded, decoded.contains("50.08765, 14.42133 ±"))
        file.delete()
    }

    @Test
    fun deniedLocationWritesNoGps() {
        val spec = AppSettings.from(noSettings).stampSpec
        val file = jpeg()
        ExifWriter.write(file, PhotoMetadata.of(spec, data(null)))
        assertNull(ExifInterface(file).latLong)
        file.delete()
    }

    @Test
    fun directShareCarriesPhotoCaptionAndGrant() {
        val file = jpeg()
        val i = Sharing.sendIntent(context, file, "caption line", "org.telegram.messenger")
        assertEquals(Intent.ACTION_SEND, i.action)
        assertEquals("image/jpeg", i.type)
        assertEquals("org.telegram.messenger", i.`package`)
        assertEquals("caption line", i.getStringExtra(Intent.EXTRA_TEXT))
        @Suppress("DEPRECATION")
        val uri = i.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!
        assertEquals("content", uri.scheme)
        assertTrue(i.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        // The URI resolves through our FileProvider to the same bytes.
        val bytes = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
        assertEquals(file.length().toInt(), bytes.size)
        file.delete()
    }

    @Test
    fun captionOffSendsNoText() {
        val file = jpeg()
        val chooser = Sharing.sendIntent(context, file, null, null)
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        @Suppress("DEPRECATION")
        val inner = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertFalse(inner.hasExtra(Intent.EXTRA_TEXT))
        file.delete()
    }
}
