package jetbrains.rsynk.server.io

import java.io.InputStream
import java.util.concurrent.atomic.AtomicLong

class BytesCountingInputStream(
        private val host: InputStream
) : InputStream() {

    private val bytesReadCounter = AtomicLong(0)

    val bytesRead: Long
        get() = bytesReadCounter.get()

    override fun read(): Int {
        bytesReadCounter.incrementAndGet()
        return host.read()
    }

    override fun read(b: ByteArray,
                      off: Int,
                      len: Int): Int {
        bytesReadCounter.addAndGet(len.toLong())
        return host.read(b, off, len)
    }

    override fun available(): Int {
        return host.available()
    }

    override fun close() {
        host.close()
    }
}
