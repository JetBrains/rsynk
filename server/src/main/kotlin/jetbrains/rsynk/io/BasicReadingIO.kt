/**
 * Copyright 2016 - 2017 JetBrains s.r.o.
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
            throw IOException("Cannot read $len byte(s): EOF received")
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
