package jetbrains.rsynk.io

import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicLong

class RsyncBufferedOutput(
        private val output: OutputStream
) : RsyncDataOutput {

    val buffer: ByteBuffer = ByteBuffer.allocate(1024 * 8).order(ByteOrder.LITTLE_ENDIAN)

    private val written = AtomicLong(0)

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
                val subsequence = bytes.duplicate()
                        .position(bytes.position())
                        .limit(Math.min(bytes.position() + bufferSpace, bytes.limit())) as ByteBuffer
                buffer.put(subsequence)
                bytes.position(subsequence.position())
            }
        }
    }

    override fun flush() {
        if (buffer.position() > 0) {
            written.addAndGet(buffer.position().toLong())
            buffer.flip()

            val bytes = buffer.array()
            val off = buffer.position()
            val len = buffer.limit()
            output.write(bytes, off, len)
            output.flush()

            buffer.clear()
        }
    }

    override val writtenBytes: Long
        get() = written.get()
}

class RsyncTaggingOutput(
        output: OutputStream
) : RsyncDataOutput {

    private val bufferedOutput = RsyncBufferedOutput(output)

    private var tagOffset = 0

    init {
        bufferedOutput.buffer.position(tagOffset + tagSize)
    }

    private fun updateTagOffsetAndBufferPosition(position: Int) {
        tagOffset = position
        bufferedOutput.buffer.position(tagOffset + tagSize)
    }

    private fun getNumberOfUntaggedBytes(): Int {
        val dataStartOffset = tagOffset + tagSize
        val numBytesUntagged = bufferedOutput.buffer.position() - dataStartOffset
        return numBytesUntagged
    }

    private fun getNumberOfBufferedBytes(): Int {
        return bufferedOutput.buffer.position() - tagSize
    }

    private fun writeMessageHeader(offset: Int, header: ChannelMessageHeader) {
        val tag = ChannelMessageHeaderDecoder.encodeHeader(header)
        bufferedOutput.buffer.putInt(offset, tag)
    }

    private fun tagData(untaggedBytes: Int) {
        val header = ChannelMessageHeader(ChannelMessageTag.Data, untaggedBytes)
        writeMessageHeader(tagOffset, header)
    }

    override fun writeChar(c: Char) {
        bufferedOutput.writeChar(c)
    }

    override fun writeInt(i: Int) {
        bufferedOutput.writeInt(i)
    }

    override fun writeByte(b: Byte) {
        bufferedOutput.writeByte(b)
    }

    override fun writeBytes(bytes: ByteBuffer) {
        bufferedOutput.writeBytes(bytes)
    }

    override val writtenBytes: Long = bufferedOutput.writtenBytes

    private companion object {
        private val tagSize = 4
        private val defaultTagOffset = 0
    }

    override fun flush() {

        val bufferedBytes = getNumberOfBufferedBytes()
        val untaggedBytes = getNumberOfUntaggedBytes()

        if (bufferedBytes <= 0) {
            return
        }

        if (untaggedBytes > 0) {
            tagData(untaggedBytes)
        } else {
            bufferedOutput.buffer.position(bufferedOutput.buffer.position() - tagSize)
        }

        bufferedOutput.flush()
        updateTagOffsetAndBufferPosition(defaultTagOffset)
    }
}
