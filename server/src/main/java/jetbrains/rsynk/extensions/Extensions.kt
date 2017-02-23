package jetbrains.rsynk.extensions

import java.nio.ByteBuffer


fun Int.toByteArray(): ByteArray = ByteBuffer.allocate(4).putInt(this).array()

fun ByteArray.toInt(): Int {
  if (this.size != 4) {
    throw IllegalArgumentException("Cannot convert array of ${this.size} to Int")
  }
  return ByteBuffer.wrap(this).int
}
