package jetbrains.rsynk.checksum

import java.util.*

object Checksum {

    private val seed = System.currentTimeMillis()

    fun nextSeed(): Int {
        val random = Random(seed)
        return Math.abs(random.nextInt())
    }

    fun rollingChecksum(data: CharArray, begin: Int, end: Int): Long {
        var s1 = 0L
        @Suppress("LoopToCallChain")
        for (i in begin..end) {
            s1 += data[i].toLong()
        }
        var s2 = 0L
        var pointer = begin
        for (i in begin..(end - 4) step 4) {
            pointer += 4
            s2 += 4 * (s1 + data[i].toLong()) + 3 * data[i + 1].toLong() + 2 * data[i + 2].toLong() + data[i + 3].toLong()
        }
        for (i in pointer..end) {
            s2 += s1
        }
        return (s1.and(0xFFFF) + s2.ushr(16)) % 0xFFFFFFFF
    }

    fun md5(data: CharArray, begin: Int, end: Int): Long {
        TODO()
    }
}
