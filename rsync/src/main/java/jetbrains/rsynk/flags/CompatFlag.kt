package jetbrains.rsynk.flags

import java.util.*

sealed class CompatFlag(override val value: Int) : Flag {
  object IncRecurse : CompatFlag(1)
  object SymlincTimes : CompatFlag(2)
  object SymlinkIconv : CompatFlag(4)
  object SafeFileList : CompatFlag(8)
  object AvoidXattrOptimization : CompatFlag(16)
  object FixChecksumSeed : CompatFlag(32)
}

fun Set<CompatFlag>.encode(): Int {
  val flagSet = HashSet<Flag>()
  flagSet.addAll(this)
  return flagSet.encode()
}

fun Byte.decodeCompatFlags(): Set<CompatFlag> {
  val thisIntValue = this.toInt()
  return listOf(CompatFlag.IncRecurse,
          CompatFlag.SymlincTimes,
          CompatFlag.SymlinkIconv,
          CompatFlag.SafeFileList,
          CompatFlag.AvoidXattrOptimization,
          CompatFlag.FixChecksumSeed)
          .filter { flag -> thisIntValue.and(flag.value) == flag.value }
          .toSet()
}
