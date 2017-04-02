package jetbrains.rsynk.extensions

import java.nio.ByteBuffer


fun Int.toLittleEndianBytes(): ByteArray = ByteBuffer.allocate(4).putInt(this).array().reversedArray()

fun ByteArray.littleEndianToInt(): Int {
    if (this.size != 4) {
        throw IllegalArgumentException("Cannot convert array of ${this.size} to Int")
    }
    return ByteBuffer.wrap(this.reversedArray()).int
}

val Int.twoLowestBytes: ByteArray
    get() {
        val bytes = this.toLittleEndianBytes()
        return byteArrayOf(bytes[0], bytes[1])
    }

fun Long.toLittleEndianBytes(): ByteArray = ByteBuffer.allocate(8).putLong(this).array().reversedArray()

val Byte.Companion.MAX_VALUE_UNSIGNED: Int
    get() = Byte.MAX_VALUE * 2 + 1

