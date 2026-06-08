package com.example.rgbcontrller

import com.example.rgbcontrller.data.bluetooth.Ws2812Protocol
import com.example.rgbcontrller.domain.model.LedMatrix
import com.example.rgbcontrller.domain.model.LedPixel
import com.example.rgbcontrller.domain.model.RgbColor
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun setOneLed_matchesProtocolExample() {
        val frame = Ws2812Protocol.setOneLed(
            id = 0,
            color = RgbColor(0, 0, 255),
            brightness = 1f,
        )

        assertEquals("AA 55 01 00 00 00 FF FF 01", frame.toHex())
    }

    @Test
    fun setAllLed_matchesProtocolExample() {
        val frame = Ws2812Protocol.setAllLed(
            color = RgbColor(255, 0, 0),
            brightness = 128 / 255f,
        )

        assertEquals("AA 55 02 FF 00 00 80 7D", frame.toHex())
    }

    @Test
    fun setFrame8_sendsThirtySixByteFrameWithChecksum() {
        val colors = listOf(
            RgbColor(255, 0, 0),
            RgbColor(0, 255, 0),
            RgbColor(0, 0, 255),
            RgbColor(255, 255, 0),
            RgbColor(0, 255, 255),
            RgbColor(128, 0, 255),
            RgbColor(255, 255, 255),
            RgbColor(0, 0, 0),
        )
        val matrix = LedMatrix(
            rows = 2,
            columns = 4,
            pixels = colors.mapIndexed { index, color ->
                LedPixel(id = index, color = color, brightness = if (index == 7) 0f else 1f)
            },
        )

        val frame = Ws2812Protocol.setFrame8(matrix)

        assertEquals(36, frame.size)
        assertEquals(
            "AA 55 10 FF 00 00 FF 00 FF 00 FF 00 00 FF FF FF FF 00 FF 00 FF FF FF 80 00 FF FF FF FF FF FF 00 00 00 00 90",
            frame.toHex(),
        )
    }

    private fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }
}
