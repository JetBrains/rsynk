package jetbrains.rsynk.io

import java.nio.ByteBuffer

interface WriteIO {
    fun writeChar(c: Char)

    fun writeInt(i: Int)

    fun writeByte(b: Byte)

    fun writeBytes(bytes: ByteBuffer)

    val writtenBytes: Long

    fun flush()
}
