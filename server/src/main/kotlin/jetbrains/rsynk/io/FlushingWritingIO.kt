package jetbrains.rsynk.io

import java.io.OutputStream
import java.util.concurrent.atomic.AtomicLong

class FlushingWritingIO(private val output: OutputStream) : WritingIO {

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
