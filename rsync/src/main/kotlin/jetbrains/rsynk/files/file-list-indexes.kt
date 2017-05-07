package jetbrains.rsynk.files

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.function.Supplier

class FileListIndexDecoder {

    private var lastPositive = 1
    private var lastNegative = -1

    fun readAndDecode(reader: Supplier<Byte>): Int {

        val buffer = IntArray(4)
        buffer[0] = reader.get().toInt()

        if (buffer[0] == 0) {
            return FileListsCode.done.code
        }

        val isNegative = if ((0xFF and buffer[0]) == 0xFF) {
            buffer[0] = reader.get().toInt()
            true
        } else {
            false
        }

        val lastIndex = if (isNegative) lastNegative else lastPositive

        val index = if (0xFF and buffer[0] == 0xFE) {

            buffer[0] = reader.get().toInt()
            buffer[1] = reader.get().toInt()

            if ((0x80 and buffer[0]) != 0) {
                buffer[3] = (0x80.inv() and buffer[0])
                buffer[0] = buffer[1]

                buffer[1] = reader.get().toInt()
                buffer[2] = reader.get().toInt()

                val bytes = byteArrayOf(buffer[0].toByte(), buffer[1].toByte(), buffer[2].toByte(), buffer[3].toByte())
                ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).int
            } else {
                (0xFF and buffer[0]).shl(8) + (0xFF and buffer[1]) + lastIndex
            }

        } else {
            (0xFF and buffer[0]) + lastIndex
        }

        if (isNegative) {
            lastNegative = index
            return -index
        } else {
            lastPositive = index
            return index
        }
    }
}
