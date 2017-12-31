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
package jetbrains.rsynk.rsync.data

import java.security.MessageDigest
import java.util.*

object RollingChecksum {

    fun calculate(bytes: ByteArray, offset: Int, length: Int): Int {
        var s1 = 0
        var s2 = 0
        var index = 0

        for (i in 0..(length - 4) step 4) {

            s2 += 4 * (s1 + bytes[offset + i + 0]) +
                    3 * bytes[offset + i + 1] +
                    2 * bytes[offset + i + 2] +
                    1 * bytes[offset + i + 3]

            s1 += bytes[offset + i + 0] +
                    bytes[offset + i + 1] +
                    bytes[offset + i + 2] +
                    bytes[offset + i + 3]

            index += 4
        }
        while (index < length) {
            s1 += bytes[offset + index]
            s2 += s1
            index++
        }
        return getInt(s1, s2)
    }

    fun rollBack(checksum: Int, blockLength: Int, value: Byte): Int {
        val lowestBytes = checksum.twoLowestBytes - value
        val highestBytes = checksum.twoHighestBytes - blockLength * value
        return getInt(lowestBytes, highestBytes)
    }

    fun rollForward(checksum: Int, value: Byte): Int {
        val lowestBytes = checksum.twoLowestBytes + value
        val highestBytes = checksum.twoHighestBytes + lowestBytes
        return getInt(lowestBytes, highestBytes)
    }

    private val Int.twoLowestBytes
        get() = 0xFFFF and this

    private val Int.twoHighestBytes
        get() = this ushr 16

    private fun getInt(twoLowestBytes: Int, twoHighestBytes: Int): Int {
        return (twoLowestBytes and 0xFFFF) or (twoHighestBytes shl 16)
    }

}

object LongChecksum {
    fun newMessageDigestInstance() = MessageDigest.getInstance("md5")!!
}

object ChecksumSeedGenerator {

    private val seed = System.currentTimeMillis()

    fun newSeed(): Int {
        val random = Random(seed)
        return Math.abs(random.nextInt())
    }
}

data class ChecksumHeader(val chunkCount: Int,
                          val blockLength: Int,
                          val digestLength: Int,
                          val remainder: Int
) {
    val isNewFile = blockLength == 0

    fun getChunkLength(index: Int): Int {
        val isLast = (chunkCount - 1 == index)

        if (isLast && remainder > 0) {
            return remainder
        }
        return blockLength
    }
}

data class RollingChecksumChunk(
        val checksum: Int
)

data class LongChecksumChunk(
        val checksum: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (other !is LongChecksumChunk) {
            return false
        }
        return Arrays.equals(this.checksum, other.checksum)
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(checksum) * 42
    }
}

data class ChecksumChunk(
        val chunkIndex: Int,
        val rollingChecksumChunk: RollingChecksumChunk,
        val longChecksumChunk: LongChecksumChunk
)

data class Checksum(val header: ChecksumHeader) {

    private val rollingChecksumToChunk = HashMap<Int, ArrayList<ChecksumChunk>>()

    operator fun plusAssign(chunk: ChecksumChunk) {
        val list = rollingChecksumToChunk[chunk.rollingChecksumChunk.checksum]
        if (list == null) {
            rollingChecksumToChunk.put(chunk.rollingChecksumChunk.checksum,
                    ArrayList<ChecksumChunk>().apply { add(chunk) })
            return
        }
        list.add(chunk)
    }

    fun getChunks(chunkRollingChecksum: Int): List<ChecksumChunk> {
        return rollingChecksumToChunk[chunkRollingChecksum] ?: emptyList()
    }
}

class ChecksumMatcher(private val checksum: Checksum) {

    fun getMatches(chunkRollingChecksum: Int,
                   length: Int,
                   preferredChunkIndex: Int): List<ChecksumChunk> {
        val chunks = checksum.getChunks(chunkRollingChecksum)
        if (chunks.isEmpty()) {
            return emptyList()
        }
        val lowerBoundIndex = chunks.sortedBy { it.chunkIndex }.lowerBound(preferredChunkIndex)
        val initialIndex = if (lowerBoundIndex < 0) {
            val insertionPoint = -lowerBoundIndex - 1
            Math.min(chunks.size - 1, insertionPoint)
        } else {
            lowerBoundIndex
        }

        if (initialIndex >= chunks.size) {
            return chunks.filter { checksum.header.getChunkLength(it.chunkIndex) == length }
        }

        val result = ArrayList<ChecksumChunk>()
        result.add(chunks[initialIndex])

        for (i in 0 until chunks.size) {
            if (i == initialIndex) {
                continue
            }
            if (checksum.header.getChunkLength(chunks[i].chunkIndex) != length) {
                continue
            }
            result.add(chunks[i])
        }
        return result
    }

    /**
     * Find index of element whose [ChecksumChunk.chunkIndex]
     * is lower or equal to [preferredChunkIndex].
     * Returns -1 if there's no such element.
     */
    private fun List<ChecksumChunk>.lowerBound(preferredChunkIndex: Int): Int {
        var left = 0
        var right = size - 1

        while (left <= right) {
            val middle = (left + right) / 2
            val correspondingIdx = this[middle].chunkIndex
            if (correspondingIdx == preferredChunkIndex) {
                return middle
            }
            if (correspondingIdx < preferredChunkIndex) {
                left = middle + 1
            } else {
                right = middle - 1
            }
        }

        return -left - 1
    }
}
