package io.github.jayfunc.carpelines.data.audio

import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpectrumBandMapperTest {

    @Test
    fun magnitudesReadsTheVisualizerFftLayout() {
        // [R0, R(n/2), R1, I1, R2, I2, ...]: DC and Nyquist are real-only, the rest interleaved.
        val fft = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 80)
        val magnitudes = SpectrumBandMapper.magnitudes(fft)

        assertEquals(5, magnitudes.size)
        assertEquals(10f / 127f, magnitudes[0], 1e-6f)
        assertEquals(20f / 127f, magnitudes[4], 1e-6f)
        assertEquals(sqrt(30f * 30f + 40f * 40f) / 127f, magnitudes[1], 1e-6f)
        assertEquals(sqrt(50f * 50f + 60f * 60f) / 127f, magnitudes[2], 1e-6f)
        assertEquals(sqrt(70f * 70f + 80f * 80f) / 127f, magnitudes[3], 1e-6f)
    }

    @Test
    fun magnitudesTreatCaptureBytesAsSigned() {
        val fft = byteArrayOf(0, 0, (-50).toByte(), (-60).toByte())
        val magnitudes = SpectrumBandMapper.magnitudes(fft)

        assertEquals(sqrt(50f * 50f + 60f * 60f) / 127f, magnitudes[1], 1e-6f)
    }

    @Test
    fun aToneLandsInItsLogarithmicBand() {
        val sampleRate = 48_000
        val captureSize = 1_024
        val magnitudes = FloatArray(captureSize / 2 + 1)
        val toneBin = (1_000f * captureSize / sampleRate).toInt()
        magnitudes[toneBin] = 1f
        magnitudes[toneBin + 1] = 1f

        val bands = SpectrumBandMapper.mapToBands(magnitudes, sampleRate, 32)

        val loudest = bands.indices.maxByOrNull { bands[it] } ?: -1
        assertTrue("1000 Hz should fall near band 17, was $loudest", loudest in 16..18)
        assertTrue(bands[loudest] >= 0.9f)
    }

    @Test
    fun silenceMapsToZeroBands() {
        val bands = SpectrumBandMapper.mapToBands(FloatArray(513), 48_000, 32)

        assertEquals(32, bands.size)
        assertTrue(bands.all { it == 0f })
    }

    @Test
    fun bassEnergyAveragesTheLowestBands() {
        val bands = floatArrayOf(0.8f, 0.6f, 0.4f, 0.2f)

        assertEquals(0.6f, SpectrumBandMapper.bassEnergy(bands), 1e-6f)
    }
}
