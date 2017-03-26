package jetbrains.rsynk.io

import java.io.IOException
import java.io.InputStream
import java.util.concurrent.atomic.AtomicLong

class BasicReadingIO(private val input: InputStream) : ReadingIO {

    private val buf = ByteArray(4096)
    private val readFromClient = AtomicLong(0)

    /**
     * Blocks until exactly {@code len} bytes are received
     * */
    override fun readBytes(len: Int): ByteArray {
        val bufferToUse = if (len > buf.size) ByteArray(len) else buf
        val read = input.read(bufferToUse, 0, len)
        if (read <= 0 || read != len) {
            throw IOException("Cannot read requested amount of data: only $read bytes of $len were read")
        }
        readFromClient.addAndGet(read.toLong())
        return bufferToUse.sliceArray(0..len - 1)
    }

    override val bytesRead: Long
        get() = readFromClient.get()
}
