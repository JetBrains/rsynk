package jetbrains.rsynk.io

import java.nio.ByteBuffer

interface WritingIO {
    fun writeBytes(bytes: ByteArray, offset: Int, len: Int)

    fun writeChar(c: Char)

    fun writeInt(i: Int)

    fun writeByte(b: Byte)

    fun writeBytes(bytes: ByteBuffer)

    fun writeBytes(bytes: ByteArray) {
        writeBytes(bytes, 0, bytes.size)
    }

    val writtenBytes: Long
}
