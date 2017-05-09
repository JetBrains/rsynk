package jetbrains.rsynk.data

import java.security.MessageDigest
import java.util.*

object ChecksumUtil {

    private val seed = System.currentTimeMillis()
    private val minDigestLength = 10
    private val maxDigestLength = 12

    fun newSeed(): Int {
        val random = Random(seed)
        return Math.abs(random.nextInt())
    }

    fun rollingChecksum(data: ByteArray, begin: Int, end: Int): Int {
        var s1 = 0
        @Suppress("LoopToCallChain")
        for (i in begin..end) {
            s1 += data[i]
        }
        var s2 = 0
        var pointer = begin
        for (i in begin..(end - 4) step 4) {
            pointer += 4
            s2 += 4 * (s1 + data[i]) +
                    3 * data[i + 1] +
                    2 * data[i + 2] +
                    1 * data[i + 3]
        }
        for (i in pointer..end) {
            s2 += s1
        }
        TODO("make sure it's correct")
        return (s1 and 0xFFFF or s2.ushr(16))
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

    fun newMessageDigestInstance() = MessageDigest.getInstance("md5")

    fun rollBack(checksum: Int, blockLength: Int, value: Byte): Int {
        TODO()
    }

    fun rollForward(checksum: Int, value: Byte): Int {
        TODO()
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
        val hash: Int
)

data class LongChecksumChunk(
        val hash: ByteArray
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
