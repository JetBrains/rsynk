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
package jetbrains.rsynk.rsync.data

import jetbrains.rsynk.rsync.extensions.toLittleEndianBytes
import java.nio.ByteBuffer
import java.nio.ByteOrder


object VarintEncoder {

    private fun Long.toLittleEndianBytes(): ByteArray = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(this).array()

    fun shortint(i: Int): ByteBuffer {
        val bytes = i.toLittleEndianBytes()
        return ByteBuffer.wrap(byteArrayOf(bytes[0], bytes[1]))
    }

    fun varint(i: Int): ByteBuffer {
        return write_var_number(i.toLittleEndianBytes(), 1)
    }

    fun varlong(l: Long, minBytes: Int): ByteBuffer {
        return write_var_number(l.toLittleEndianBytes(), minBytes)
    }

    private fun write_var_number(_bytes: ByteArray, minBytes: Int): ByteBuffer {
        var count = _bytes.size
        val bytes = byteArrayOf(0) + _bytes
        while (count > minBytes && bytes[count] == 0.toByte()) {
            count--
        }
        val firstByte = 0xFF and 1 shl (7 - count + minBytes)

        if (0xFF and bytes[count].toInt() >= firstByte) {
            count++
            bytes[0] = (firstByte - 1).inv().toByte()
        } else if (count > minBytes) {
            bytes[0] = (bytes[count].toInt() or (firstByte * 2 - 1).inv()).toByte()
        } else {
            bytes[0] = bytes[count]
        }
        return ByteBuffer.wrap(bytes, 0, count).order(ByteOrder.LITTLE_ENDIAN)
    }
}
