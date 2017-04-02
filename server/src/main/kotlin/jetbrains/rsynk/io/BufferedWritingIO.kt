package jetbrains.rsynk.io

import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.Channels
import java.util.concurrent.atomic.AtomicLong

class BufferedWritingIO(output: OutputStream) : WriteIO {

    val buffer = ByteBuffer.allocateDirect(1024 * 10).order(ByteOrder.LITTLE_ENDIAN) //TODO capacity to settings
    val channel = Channels.newChannel(output)

    override fun writeChar(c: Char) {
        if (buffer.remaining() < 2) {
            flush()
        }
        buffer.putChar(c)
    }

    override fun writeInt(i: Int) {
        if (buffer.remaining() < 4) {
            flush()
        }
        buffer.putInt(i)
    }

    override fun writeByte(b: Byte) {
        if (buffer.remaining() < 1) {
            flush()
        }
        buffer.put(b)
    }

    override fun writeBytes(bytes: ByteBuffer) {
        while (bytes.hasRemaining()) {
            val bufferSpace = buffer.remaining()
            if (bufferSpace == 0) {
                flush()
            } else {
                val slice = bytes.duplicate().position(bytes.position()).limit(bufferSpace) as ByteBuffer
                buffer.put(slice)
                bytes.position(slice.position())
            }
        }
    }

    override fun flush() {
        if (buffer.position() > 0) {
            written.addAndGet(buffer.position().toLong())
            buffer.flip()
            while (buffer.hasRemaining()) {
                channel.write(buffer)
            }
            buffer.clear()
        }
    }

    val written = AtomicLong(0)

    override val writtenBytes: Long
        get() = written.get()
}