package jetbrains.rsynk.checksum

object RollingChecksumSeedUtil {
  fun nextSeed(): Int {
    val time = System.currentTimeMillis().toInt()
    val thread = Thread.currentThread().id.toInt()
    return time.xor(thread.shl(6))
  }
}