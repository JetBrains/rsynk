/**
 * Copyright 2016 - 2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrains.rsynk.server.io

import mu.KLogging
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicLong

private interface BytesReader {
    fun readBytes(len: Int): ByteArray
    fun readBytes(buf: ByteArray, off: Int, len: Int)
    val bytesRead: Long
}

private class BytesReaderImpl(private val input: InputStream) : BytesReader {

    private val readBytes = AtomicLong(0)

    override fun readBytes(len: Int): ByteArray {
        val buf = ByteArray(len)

        val read = input.read(buf)

        if (read == -1) {
            throw IOException("Cannot read $len byte(s): EOF received")
        }

        if (read != len) {
            throw IOException("Cannot read requested amount of data: only $read bytes of $len were read")
        }

        readBytes.addAndGet(read.toLong())
        return buf
    }

    override fun readBytes(buf: ByteArray, off: Int, len: Int) {
        val read = input.read(buf, off, len)

        if (read == -1) {
            throw IOException("Cannot read $len byte(s): EOF received")
        }

        if (read != len) {
            throw IOException("Cannot read requested amount of data: only $read bytes of $len were read")
        }

        readBytes.addAndGet(read.toLong())
    }

    override val bytesRead: Long = readBytes.get()
}

private interface RsyncBytesReader : BytesReader {
    fun readNextAvailable(buf: ByteArray, off: Int): Int
    val bytesAvailable: Int
}

private class RsyncBytesReaderImpl(
        private val input: BytesReader
) : RsyncBytesReader, BytesReader by input {

    companion object : KLogging()

    private var available: Int = 0

    override val bytesAvailable: Int
        get() = available

    override fun readBytes(len: Int): ByteArray {
        val bytes = ByteArray(len)
        var read = 0
        while (read < len) {
            read += readNextAvailable(bytes, read)
        }
        return bytes
    }

    override fun readNextAvailable(buf: ByteArray, off: Int): Int {

        while (available == 0) {
            available = readNextMessage()
        }

        val len = Math.min(available, buf.size - off)
        input.readBytes(buf, off, len)
        available -= len
        return len
    }

    private fun readNextMessage(): Int {
        val tag = readInt(input.readBytes(intSize), 0)
        val dataHeader = ChannelMessageHeaderDecoder.decodeHeader(tag) ?: throw IOException("Cannot decode header $tag")

        if (dataHeader.tag == ChannelMessageTag.Data) {
            return dataHeader.length
        }

        logger.error { "Received message: $dataHeader" }
        return 0
    }
}

const private val charSize = 2
const private val intSize = 4


private fun readChar(buffer: ByteArray, off: Int): Char {
    return ByteBuffer.wrap(buffer, off, charSize).order(ByteOrder.LITTLE_ENDIAN).char
}

private fun readInt(buffer: ByteArray, off: Int): Int {
    return ByteBuffer.wrap(buffer, off, intSize).order(ByteOrder.LITTLE_ENDIAN).int
}

class RsyncInput(
        input: InputStream
) : RsyncDataInput {

    private val bytesReader = BytesReaderImpl(input)

    override fun readBytes(len: Int): ByteArray {
        return bytesReader.readBytes(len)
    }

    override fun readBytes(dest: ByteArray, off: Int, len: Int) {
        bytesReader.readBytes(dest, off, len)
    }

    override fun readInt(): Int {
        return readInt(bytesReader.readBytes(intSize), 0)
    }

    override fun readChar(): Char {
        return readChar(bytesReader.readBytes(charSize), 0)
    }

    override fun bytesRead(): Long {
        return bytesReader.bytesRead
    }

    override fun bytesAvailable(): Int { /** TODO: Shouldn't it be in [RsyncDataInput]? */
        return 0
    }
}


class RsyncTaggingInput(
        input: InputStream
) : RsyncDataInput {

    private val buffer = ByteArray(1024 * 8)
    private val byteReader = RsyncBytesReaderImpl(BytesReaderImpl(input))

    private var readPtr = 0
    private var fetchedBytesPtr = 0

    override fun readBytes(len: Int): ByteArray {
        val result = ByteArray(len)
        readBytes(result, 0, len)
        return result
    }

    override fun readBytes(dest: ByteArray, off: Int, len: Int) {
        ensureFetched(len)
        val written = writeFetchedBytes(dest, off, len)
        if (written < len) {
            byteReader.readBytes(dest, off + written, len - written)
        }
    }

    override fun readInt(): Int {
        ensureFetched(intSize)
        readInt(buffer, readPtr).let {
            readPtr += intSize
            return it
        }
    }

    override fun readChar(): Char {
        ensureFetched(charSize)
        readChar(buffer, readPtr).let {
            readPtr += charSize
            return it
        }
    }

    override fun bytesAvailable(): Int = byteReader.bytesAvailable + fetchedBytesPtr - readPtr

    override fun bytesRead(): Long = byteReader.bytesRead

    private fun writeFetchedBytes(dest: ByteArray, off: Int, len: Int): Int {
        val fetchedLength = Math.min(len, fetchedBytesPtr - readPtr)
        System.arraycopy(buffer, readPtr, dest, off, fetchedLength)
        readPtr += fetchedLength
        return fetchedLength
    }

    private fun ensureFetched(len: Int) {
        if (buffer.size - fetchedBytesPtr < len) {
            System.arraycopy(buffer, readPtr, buffer, 0, fetchedBytesPtr - readPtr)
            fetchedBytesPtr -= readPtr
            readPtr = 0
        }

        if (buffer.size - fetchedBytesPtr < len) {
            throw IOException("Buffer is not big enough: fetchedBytesPtr=$fetchedBytesPtr, len=$len")
        }


        while (fetchedBytesPtr - readPtr < len) {
            val read = byteReader.readNextAvailable(buffer, fetchedBytesPtr)
            fetchedBytesPtr += read
        }
    }
}
