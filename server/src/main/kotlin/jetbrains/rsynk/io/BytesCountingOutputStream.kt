package jetbrains.rsynk.io

import java.io.OutputStream
import java.util.concurrent.atomic.AtomicLong

class BytesCountingOutputStream(
        private val host: OutputStream
) : OutputStream() {

    private val bytesWrittenCounter = AtomicLong(0)

    val bytesWritten: Long
        get() = bytesWrittenCounter.get()

    override fun write(b: Int) {
        bytesWrittenCounter.incrementAndGet()
        host.write(b)
    }

    override fun write(b: ByteArray,
                       off: Int,
                       len: Int) {
        bytesWrittenCounter.addAndGet(len.toLong())
        host.write(b, off, len)
    }

    override fun flush() {
        host.flush()
    }

    override fun close() {
        host.close()
    }
}