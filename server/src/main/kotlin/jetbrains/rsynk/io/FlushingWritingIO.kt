package jetbrains.rsynk.io

import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicLong

class FlushingWritingIO(private val output: OutputStream) : WritingIO {
    override fun writeChar(c: Char) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun writeInt(i: Int) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun writeByte(b: Byte) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun writeBytes(bytes: ByteBuffer) {
        throw UnsupportedOperationException("not implemented")
    }

    val written = AtomicLong(0)
    /**
     * Writes {@code bytes} buffer with given {@code offset} and
     * {@len} and forces them to be written
     */
    override fun writeBytes(bytes: ByteArray, offset: Int, len: Int) {
        output.write(bytes, offset, len)
        output.flush()
    }

    override val writtenBytes: Long
        get() = written.get()
}
