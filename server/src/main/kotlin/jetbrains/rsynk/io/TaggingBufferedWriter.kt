package jetbrains.rsynk.io

import java.io.OutputStream

class TaggingBufferedWriter(output: OutputStream,
                            bufferSize: Int
) : BufferedWriter(output, bufferSize) {

    private companion object {
        private val tagSize = 4
        private val defaultTagOffset = 0
    }

    private var tagOffset = 0

    private fun updateTagOffsetAndBufferPosition(position: Int) {
        tagOffset = position
        super.buffer.position(tagOffset + tagSize)
    }

    private fun getNumberOfUntaggedBytes(): Int {
        val dataStartOffset = tagOffset + tagSize
        val numBytesUntagged = super.buffer.position() - dataStartOffset
        return numBytesUntagged
    }

    private fun getNumberOfBufferedBytes(): Int {
        return super.buffer.position() - tagOffset
    }

    private fun writeMessageHeader(offset: Int, header: ChannelMessageHeader) {
        val tag = ChannelMessageHeaderDecoder.encodeHeader(header)
        super.buffer.putInt(offset, tag)
    }

    private fun tagData() {
        val untaggedBytesCnt = getNumberOfUntaggedBytes()
        val header = ChannelMessageHeader(ChannelMessageTag.Data, untaggedBytesCnt)
        writeMessageHeader(tagOffset, header)
    }

    override fun flush() {

        val bufferedBytes = getNumberOfBufferedBytes()
        val untaggedBytes = getNumberOfUntaggedBytes()

        if (bufferedBytes <= 0) {
            return
        }

        if (untaggedBytes > 0) {
            tagData()
        } else {
            super.buffer.position(super.buffer.position() - tagSize)
        }

        super.flush()
        updateTagOffsetAndBufferPosition(defaultTagOffset)
    }
}
