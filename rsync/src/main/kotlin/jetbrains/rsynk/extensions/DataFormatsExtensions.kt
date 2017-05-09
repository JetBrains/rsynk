package jetbrains.rsynk.extensions

val Byte.Companion.MAX_VALUE_UNSIGNED: Int
    get() = Byte.MAX_VALUE * 2 + 1

fun Int.toLittleEndianBytes(): ByteArray {
    return byteArrayOf(this.ushr(0).toByte(),
                       this.ushr(8).toByte(),
                       this.ushr(16).toByte(),
                       this.ushr(24).toByte())
}
