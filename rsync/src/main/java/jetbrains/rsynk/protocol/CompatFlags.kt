package jetbrains.rsynk.protocol

sealed class CompatFlags(val value: Int) {
  object CF_INC_RECURSE : CompatFlags(1)
  object CF_SYMLINK_TIMES : CompatFlags(2)
  object CF_SYMLINK_ICONV : CompatFlags(4)
  object CF_SAFE_FLIST : CompatFlags(8)
  object CF_AVOID_XATTR_OPTIM : CompatFlags(16)
  object CF_CHKSUM_SEED_FIX : CompatFlags(32)
}

fun Set<CompatFlags>.encode(): Byte {
  return this.fold(0, { value, flag -> value.or(flag.value) }).toByte()
}

fun Byte.decode(): Set<CompatFlags> {
  val thisIntValue = this.toInt()
  return listOf(CompatFlags.CF_INC_RECURSE,
          CompatFlags.CF_SYMLINK_TIMES,
          CompatFlags.CF_SYMLINK_ICONV,
          CompatFlags.CF_SAFE_FLIST,
          CompatFlags.CF_AVOID_XATTR_OPTIM,
          CompatFlags.CF_CHKSUM_SEED_FIX)
          .filter { flag -> thisIntValue.and(flag.value) == flag.value }
          .toSet()
}