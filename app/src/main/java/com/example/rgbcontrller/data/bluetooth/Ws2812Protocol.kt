package com.example.rgbcontrller.data.bluetooth

import com.example.rgbcontrller.domain.model.LedMatrix
import com.example.rgbcontrller.domain.model.RgbColor

object Ws2812Protocol {
    private const val Header0 = 0xAA
    private const val Header1 = 0x55
    private const val CmdSetOne = 0x01
    private const val CmdSetAll = 0x02
    private const val CmdSetFrame8 = 0x10
    private const val LedCount = 8

    fun buildFrame(cmd: Int, payload: IntArray): ByteArray {
        val out = ByteArray(2 + 1 + payload.size + 1)
        out[0] = Header0.toByte()
        out[1] = Header1.toByte()
        out[2] = cmd.toByte()

        var checksum = cmd and 0xFF
        payload.forEachIndexed { index, value ->
            val byteValue = value and 0xFF
            out[3 + index] = byteValue.toByte()
            checksum = checksum xor byteValue
        }

        out[out.lastIndex] = checksum.toByte()
        return out
    }

    fun setOneLed(id: Int, color: RgbColor, brightness: Float): ByteArray {
        require(id in 0 until LedCount) { "LED id must be in 0..7." }
        return buildFrame(
            CmdSetOne,
            intArrayOf(id, color.redByte(), color.greenByte(), color.blueByte(), brightness.toByteValue()),
        )
    }

    fun setAllLed(color: RgbColor, brightness: Float): ByteArray {
        return buildFrame(
            CmdSetAll,
            intArrayOf(color.redByte(), color.greenByte(), color.blueByte(), brightness.toByteValue()),
        )
    }

    fun setFrame8(matrix: LedMatrix): ByteArray {
        val pixelsById = matrix.pixels.associateBy { it.id }
        val payload = IntArray(LedCount * 4)
        repeat(LedCount) { id ->
            val pixel = pixelsById[id]
            val offset = id * 4
            payload[offset] = pixel?.color?.redByte() ?: 0
            payload[offset + 1] = pixel?.color?.greenByte() ?: 0
            payload[offset + 2] = pixel?.color?.blueByte() ?: 0
            payload[offset + 3] = pixel?.brightness?.toByteValue() ?: 0
        }
        return buildFrame(CmdSetFrame8, payload)
    }

    private fun RgbColor.redByte(): Int = red.coerceIn(0, 255)
    private fun RgbColor.greenByte(): Int = green.coerceIn(0, 255)
    private fun RgbColor.blueByte(): Int = blue.coerceIn(0, 255)

    private fun Float.toByteValue(): Int = (coerceIn(0f, 1f) * 255f).toInt().coerceIn(0, 255)
}
