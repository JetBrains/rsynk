package jetbrains.rsynk.extensions

import java.nio.ByteBuffer

fun Long.toLittleEndianBytes(): ByteArray = ByteBuffer.allocate(8).putLong(this).array().reversedArray()

val Byte.Companion.MAX_VALUE_UNSIGNED: Int
    get() = Byte.MAX_VALUE * 2 + 1

