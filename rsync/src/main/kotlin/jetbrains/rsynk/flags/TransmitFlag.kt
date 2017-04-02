package jetbrains.rsynk.flags

sealed class TransmitFlag(val value: Int) {
    object TopDirectory : TransmitFlag(1)
    object SameMode : TransmitFlag(2)
    object ExtendedFlags : TransmitFlag(4)
    object SameUserId : TransmitFlag(8)
    object SameGroupId : TransmitFlag(16)
    object SameName : TransmitFlag(32)
    object SameLongName : TransmitFlag(64)
    object SameLastModifiedTime : TransmitFlag(128)
    object SameRdevMajor : TransmitFlag(256)
    object NoContentDirs : TransmitFlag(256)
    object HardLinked : TransmitFlag(512)
    object UserNameFollows : TransmitFlag(1024)
    object GroupNameFollows : TransmitFlag(2048)
    object HardLinksFirst : TransmitFlag(4096)
    object IoErrorEndList : TransmitFlag(4096)
    object ModNsec : TransmitFlag(8192)
}

fun Set<TransmitFlag>.encode(): Int {
    return this.fold(0, { value, flag -> value.or(flag.value) })
}


