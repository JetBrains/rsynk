package jetbrains.rsynk.io

import java.nio.ByteBuffer

class AutoFlushingWriter(
        private val writer: WriteIO
) : WriteIO {
    override fun writeChar(c: Char) {
        writer.writeChar(c)
        writer.flush()
    }

    override fun writeInt(i: Int) {
        writer.writeInt(i)
        writer.flush()
    }

    override fun writeByte(b: Byte) {
        writer.writeByte(b)
        writer.flush()
    }

    override fun writeBytes(bytes: ByteBuffer) {
        writer.writeBytes(bytes)
        writer.flush()
    }

    override val writtenBytes: Long
        get() = writer.writtenBytes

    override fun flush() {
        writer.flush()
    }

}
