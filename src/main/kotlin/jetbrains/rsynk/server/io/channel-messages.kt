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

internal enum class ChannelMessageTag(val code: Int) {
    Data(0),
    ErrorXfer(1),
    Info(2),
    Error(3),
    Warning(4),
    ErrorSocket(5),
    Log(6),
    Client(7),
    ErrorUtf(8),
    Redo(9),
    Flist(20),
    FlistEof(21),
    IOError(22),
    NOP(42),
    Done(86),
    Success(100),
    Deleted(101),
    NoSend(102)
}

private val msgTagOffset = 7
private val msgLengthMask = 0xFFFFFF

internal data class ChannelMessageHeader(val tag: ChannelMessageTag,
                                val length: Int)


internal object ChannelMessageHeaderDecoder {

    fun decodeHeader(tag: Int): ChannelMessageHeader? {
        val length = tag and msgLengthMask
        val tagObject = tagFromInt((tag shr 24) - msgTagOffset) ?: return null
        return ChannelMessageHeader(tagObject, length)
    }

    fun encodeHeader(header: ChannelMessageHeader): Int {
        return msgTagOffset + header.tag.code shl 24 or header.length
    }

    private fun tagFromInt(code: Int): ChannelMessageTag? {
        ChannelMessageTag.values().forEach {
            if (code == it.code) {
                return it
            }
        }
        return null
    }
}

