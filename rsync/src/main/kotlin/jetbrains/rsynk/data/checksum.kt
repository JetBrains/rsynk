package jetbrains.rsynk.data

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

    private val  Int.twoHighestBytes
        get() = this ushr 16

    private fun getInt(twoLowestBytes: Int, twoHighestBytes: Int): Int {
        return (twoLowestBytes and 0xFFFF) or (twoHighestBytes shl 16)
    }

}

object LongChecksum {
    fun newMessageDigestInstance() = MessageDigest.getInstance("md5")!!
}

object ChecksumUtil {

    private val seed = System.currentTimeMillis()
    private val minDigestLength = 10
    private val maxDigestLength = 12

    fun newSeed(): Int {
        val random = Random(seed)
        return Math.abs(random.nextInt())
    }

    fun getFileDigestLength(fileSize: Long, blockLength: Int): Int {

        val digestLength = (10 + 2 * log2(fileSize.toDouble() - log2(blockLength.toDouble())).toInt() - 24) / 8

        if (digestLength < minDigestLength) {
            return minDigestLength
        }

        if (digestLength > maxDigestLength) {
            return maxDigestLength
        }
        return digestLength
    }

    private fun log2(x: Double): Double {
        return Math.log(x) / Math.log(2.0)
    }

}

data class ChecksumHeader(val chunkCount: Int,
                          val blockLength: Int,
                          val digestLength: Int,
                          val remainder: Int
) {
    val isNewFile = blockLength == 0
}

data class RollingChecksumChunk(
        val checksum: Int
)

data class LongChecksumChunk(
        val checksum: ByteArray
)

data class ChecksumChunk(
        val chunkIndex: Int,
        val rollingChecksumChunk: RollingChecksumChunk,
        val longChecksumChunk: LongChecksumChunk
)

class Checksum(val header: ChecksumHeader) {

    private val chunks = ArrayList<ChecksumChunk>()

    operator fun plusAssign(chunk: ChecksumChunk) {
        chunks.add(chunk)
    }
}

class ChecksumMatcher(private val checksum: Checksum) {

    fun getMatches(rollingChecksumChunk: Int,
                   length: Int,
                   preferredChunkIndex: Int): List<ChecksumChunk> {
        TODO()
    }
}
