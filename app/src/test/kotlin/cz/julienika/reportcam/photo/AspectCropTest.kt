package cz.julienika.reportcam.photo

import org.junit.Assert.assertEquals
import org.junit.Test

class AspectCropTest {
    @Test
    fun fullKeepsTheWholePhoto() {
        assertEquals(CropRect(0, 0, 960, 1280), AspectCrop.centered(960, 1280, AspectRatio.FULL))
    }

    @Test
    fun squareTrimsTheLongEdgeAndCentres() {
        assertEquals(CropRect(0, 160, 960, 960), AspectCrop.centered(960, 1280, AspectRatio.SQUARE))
        assertEquals(CropRect(160, 0, 960, 960), AspectCrop.centered(1280, 960, AspectRatio.SQUARE))
    }

    @Test
    fun theShapeFollowsTheWayThePhoneIsHeld() {
        // 2:3 on a portrait photo is 2 wide, 3 high; on a landscape one 3 wide, 2 high.
        assertEquals(CropRect(53, 0, 853, 1280), AspectCrop.centered(960, 1280, AspectRatio.R2_3))
        assertEquals(CropRect(0, 53, 1280, 853), AspectCrop.centered(1280, 960, AspectRatio.R2_3))
    }

    @Test
    fun narrowerThanTheSensorTrimsTheShortEdge() {
        val r = AspectCrop.centered(960, 1280, AspectRatio.R9_16)
        assertEquals(1280, r.height)
        assertEquals(720, r.width)
        assertEquals(120, r.left)
    }

    @Test
    fun widerThanTheSensorTrimsTheLongEdge() {
        val r = AspectCrop.centered(960, 1280, AspectRatio.R4_5)
        assertEquals(960, r.width)
        assertEquals(1200, r.height)
        assertEquals(40, r.top)
    }

    @Test
    fun theGuideAndThePhotoCutTheSameShare() {
        // A preview shown at 1080x1440 dims the same fraction the pipeline crops from 960x1280.
        val (gw, gh) = AspectCrop.rect(1080f, 1440f, AspectRatio.R9_16)
        val photo = AspectCrop.centered(960, 1280, AspectRatio.R9_16)
        assertEquals(photo.width / 960f, gw / 1080f, 0.002f)
        assertEquals(photo.height / 1280f, gh / 1440f, 0.002f)
    }

    @Test
    fun unknownStoredIdFallsBackToFull() {
        assertEquals(AspectRatio.FULL, AspectRatio.of("7:5"))
        assertEquals(AspectRatio.SQUARE, AspectRatio.of("1:1"))
    }
}
