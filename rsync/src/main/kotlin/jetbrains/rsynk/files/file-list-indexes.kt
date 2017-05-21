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
package jetbrains.rsynk.files

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.function.Consumer
import java.util.function.Supplier

class FileListIndexDecoder {

    private var lastPositive = -1
    private var lastNegative = 1

    fun readAndDecode(reader: Supplier<Byte>): Int {

        val buffer = IntArray(4)
        buffer[0] = reader.get().toInt()

        if (buffer[0] == 0) {
            return FileListsCode.done.code
        }

        val isNegative = if ((0xFF and buffer[0]) == 0xFF) {
            buffer[0] = reader.get().toInt()
            true
        } else {
            false
        }

        val lastIndex = if (isNegative) lastNegative else lastPositive

        val index = if (0xFF and buffer[0] == 0xFE) {

            buffer[0] = reader.get().toInt()
            buffer[1] = reader.get().toInt()

            if ((0x80 and buffer[0]) != 0) {
                buffer[3] = (0x80.inv() and buffer[0])
                buffer[0] = buffer[1]

                buffer[1] = reader.get().toInt()
                buffer[2] = reader.get().toInt()

                val bytes = byteArrayOf(buffer[0].toByte(), buffer[1].toByte(), buffer[2].toByte(), buffer[3].toByte())
                ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).int
            } else {
                (0xFF and buffer[0]).shl(8) + (0xFF and buffer[1]) + lastIndex
            }

        } else {
            (0xFF and buffer[0]) + lastIndex
        }

        if (isNegative) {
            lastNegative = index
            return -index
        } else {
            lastPositive = index
            return index
        }
    }
}

class FileListIndexEncoder {

    private var lastPositive = 1
    private var lastNegative = -1

    fun encodeAndSend(index: Int, writer: Consumer<Byte>) {

        if (index == FileListsCode.done.code) {
            writer.accept(0)
            return
        }

        val positiveIndexValue = Math.abs(index)

        val diff = if (index >= 0) {
            val diff = positiveIndexValue - lastPositive
            lastPositive = positiveIndexValue
            diff
        } else {
            val diff = positiveIndexValue - lastNegative
            lastNegative = positiveIndexValue
            writer.accept(0xFF)
            diff
        }

        when {
            diff in 1..(0xFE - 1) -> {
                writer.accept(diff.toByte())
            }

            diff < 0 || diff > 0x7FFF -> {
                writer.accept(0xFE)
                writer.accept(positiveIndexValue.shr(24) or 0x80)
                writer.accept(positiveIndexValue)
                writer.accept(positiveIndexValue.shr(8))
                writer.accept(positiveIndexValue.shr(16))
            }

            else -> {
                writer.accept(0xFE)
                writer.accept(diff.shr(8))
                writer.accept(diff)
            }
        }
    }

    private fun Consumer<Byte>.accept(intValue: Int) {
        accept(intValue.toByte())
    }
}

