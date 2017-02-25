package jetbrains.rsynk.protocol.flags

sealed class CompatFlag(val value: Int) {
  object CF_INC_RECURSE : CompatFlag(1)
  object CF_SYMLINK_TIMES : CompatFlag(2)
  object CF_SYMLINK_ICONV : CompatFlag(4)
  object CF_SAFE_FLIST : CompatFlag(8)
  object CF_AVOID_XATTR_OPTIM : CompatFlag(16)
  object CF_CHKSUM_SEED_FIX : CompatFlag(32)
}

fun Set<CompatFlag>.encode(): Byte {
  return this.fold(0, { value, flag -> value.or(flag.value) }).toByte()
}

fun Byte.decode(): Set<CompatFlag> {
  val thisIntValue = this.toInt()
  return listOf(CompatFlag.CF_INC_RECURSE,
          CompatFlag.CF_SYMLINK_TIMES,
          CompatFlag.CF_SYMLINK_ICONV,
          CompatFlag.CF_SAFE_FLIST,
          CompatFlag.CF_AVOID_XATTR_OPTIM,
          CompatFlag.CF_CHKSUM_SEED_FIX)
          .filter { flag -> thisIntValue.and(flag.value) == flag.value }
          .toSet()
}