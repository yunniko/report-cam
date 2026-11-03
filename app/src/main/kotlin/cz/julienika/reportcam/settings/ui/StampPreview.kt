package cz.julienika.reportcam.settings.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import cz.julienika.reportcam.R
import cz.julienika.reportcam.settings.AppSettings
import cz.julienika.reportcam.stamp.Corner
import cz.julienika.reportcam.stamp.FramePlacement
import cz.julienika.reportcam.stamp.GeoFix
import cz.julienika.reportcam.stamp.LocationReading
import cz.julienika.reportcam.stamp.OverlayPlacement
import cz.julienika.reportcam.stamp.Place
import cz.julienika.reportcam.stamp.StampData
import cz.julienika.reportcam.stamp.StampKeys
import cz.julienika.reportcam.stamp.StampRenderer
import cz.julienika.reportcam.stamp.StampSpec
import java.time.ZonedDateTime

/**
 * Settings previews drawn by the real stamp renderer on a crop of the Owner's photo (`preview_photo.jpg`,
 * see README), with the user's own fields, so a preview shows what a photo will get.
 */
internal object StampPreview {
    private var photo: Bitmap? = null

    private fun base(c: Context): Bitmap =
        photo ?: BitmapFactory
            .decodeResource(c.resources, R.drawable.preview_photo, BitmapFactory.Options().apply { inSampleSize = 2 })
            .also { photo = it }

    private fun sample(s: AppSettings): StampData {
        val now = ZonedDateTime.now()
        return StampData.EMPTY
            .with(StampKeys.CAPTURE_TIME, now)
            .with(StampKeys.USER_TEXT, s.userText.ifBlank { "Report Cam" })
            .with(StampKeys.LOCATION, LocationReading.Fix(GeoFix(50.08765, 14.42133, 4f, now.toInstant().toEpochMilli())))
            .with(StampKeys.PLACE, Place(street = "Karlova", houseNumber = "12", city = "Praha", fullAddress = "Karlova 12, 110 00 Praha 1"))
    }

    /** The overlay in the user's corner, or bottom left when they use a frame. */
    fun overlay(
        c: Context,
        s: AppSettings,
    ): Bitmap {
        val placement = s.stampSpec.placement as? OverlayPlacement ?: OverlayPlacement(Corner.BOTTOM_START)
        return render(c, s, s.stampSpec.copy(placement = placement))
    }

    /** The frame as the user set it, or a bottom band with a medium border when they use an overlay. */
    fun frame(
        c: Context,
        s: AppSettings,
    ): Bitmap {
        val placement =
            s.stampSpec.placement as? FramePlacement
                ?: FramePlacement(top = false, borderFraction = AppSettings.FRAME_BORDERS.getValue("medium"))
        return render(c, s, s.stampSpec.copy(placement = placement))
    }

    private fun render(
        c: Context,
        s: AppSettings,
        spec: StampSpec,
    ): Bitmap = StampRenderer.render(base(c).copy(Bitmap.Config.ARGB_8888, true), spec, sample(s))
}
