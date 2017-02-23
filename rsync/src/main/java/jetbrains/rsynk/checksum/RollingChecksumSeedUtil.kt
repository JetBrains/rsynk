package jetbrains.rsynk.checksum

import java.util.*

object RollingChecksumSeedUtil {

  private val seed = System.currentTimeMillis()

  fun nextSeed(): Int {
    val random = Random(seed)
    return Math.abs(random.nextInt())
  }
}
