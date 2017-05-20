package jetbrains.rsynk.io

import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicLong

class BasicReadingIO(private val input: InputStream) : ReadingIO {

    private val readBytes = AtomicLong(0)

    override fun readBytes(len: Int): ByteArray {
        val buf = ByteArray(len)

        val read = input.read(buf)

        if (read == -1) {
            throw IOException("Cannot read $len bytes: EOF received")
        }

        if (read != len) {
            throw IOException("Cannot read requested amount of data: only $read bytes of $len were read")
        }

        readBytes.addAndGet(read.toLong())
        return buf
    }

    override fun readInt(): Int {
        return ByteBuffer.wrap(readBytes(4)).order(ByteOrder.LITTLE_ENDIAN).int
    }

    override fun readChar(): Char {
        return ByteBuffer.wrap(readBytes(2)).order(ByteOrder.LITTLE_ENDIAN).char
    }

    override fun bytesRead(): Long = readBytes.get()
}
