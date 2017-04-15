package jetbrains.rsynk.io

import java.nio.ByteBuffer
import java.nio.ByteOrder


object VarintEncoder {

    private fun Long.toLittleEndianBytes(): ByteArray = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(this).array()

    fun varlong(l: Long, minBytes: Int): ByteBuffer {
        return write_var_number(l.toLittleEndianBytes(), minBytes)
    }

    private fun write_var_number(_bytes: ByteArray, minBytes: Int): ByteBuffer {
        var count = _bytes.size
        val bytes = byteArrayOf(0) + _bytes
        while (count > minBytes && bytes[count] == 0.toByte()) {
            count--
        }
        val firstByte = 0xFF and 1.shl(7 - count + minBytes)

        if (0xFF and bytes[count].toInt() >= firstByte) {
            count++
            bytes[0] = (firstByte - 1).inv().toByte()
        } else if (count > minBytes) {
            bytes[0] = (bytes[count].toInt() or (firstByte * 2 - 1).inv()).toByte()
        } else {
            bytes[0] = bytes[count]
        }
        return ByteBuffer.wrap(bytes, 0, count).order(ByteOrder.LITTLE_ENDIAN)
    }
}
