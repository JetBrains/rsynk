package jetbrains.rsynk.flags


sealed class CompatFlag(val value: Int) {
    object IncRecurse : CompatFlag(1)
    object SymlincTimes : CompatFlag(2)
    object SymlinkIconv : CompatFlag(4)
    object SafeFileList : CompatFlag(8)
    object AvoidXattrOptimization : CompatFlag(16)
    object FixChecksumSeed : CompatFlag(32)
}

fun Set<CompatFlag>.encode(): Byte {
    return this.fold(0, { value, flag -> value.or(flag.value) }).toByte()
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
