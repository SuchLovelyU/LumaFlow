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
        repeat(LedCount) { physicalId ->
            val pixel = pixelsById[matrix.logicalIdForPhysicalLed(physicalId)]
            val brightness = pixel?.brightness?.coerceIn(0f, 1f) ?: 0f
            val offset = physicalId * 4
            payload[offset] = pixel?.color?.redByte()?.scaledBy(brightness) ?: 0
            payload[offset + 1] = pixel?.color?.greenByte()?.scaledBy(brightness) ?: 0
            payload[offset + 2] = pixel?.color?.blueByte()?.scaledBy(brightness) ?: 0
            payload[offset + 3] = if (pixel == null) 0 else 255
        }
        return buildFrame(CmdSetFrame8, payload)
    }

    private fun LedMatrix.logicalIdForPhysicalLed(physicalId: Int): Int {
        if (rows <= 0 || columns <= 0 || physicalId >= ledCount) return physicalId
        val row = physicalId / columns
        val physicalColumn = physicalId % columns
        val mirroredColumn = columns - 1 - physicalColumn
        val logicalColumn = if (row % 2 == 0) {
            mirroredColumn
        } else {
            columns - 1 - mirroredColumn
        }
        return row * columns + logicalColumn
    }

    private fun RgbColor.redByte(): Int = red.coerceIn(0, 255)
    private fun RgbColor.greenByte(): Int = green.coerceIn(0, 255)
    private fun RgbColor.blueByte(): Int = blue.coerceIn(0, 255)

    private fun Int.scaledBy(brightness: Float): Int = (this * brightness.coerceIn(0f, 1f)).toInt().coerceIn(0, 255)

    private fun Float.toByteValue(): Int = (coerceIn(0f, 1f) * 255f).toInt().coerceIn(0, 255)
}
