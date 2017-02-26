package jetbrains.rsynk.extensions

import java.nio.ByteBuffer


/**
 * Rsync transforms bytes to int like this:
 * {@code
 *      union {
 *	  	    const uchar *b;
 *	    	const uint32 *num;
 *	    } u;
 * }
 * Since reading a number works from the end to begin
 * we should send reversed array, pretending rsynk
 * casts bytes to int same way
 */
fun Int.toReversedByteArray(): ByteArray = ByteBuffer.allocate(4).putInt(this).array().reversedArray()


/**
 * Same as @{code Int.toReversedByteArray}
 * Rsync sends reversed 4 bytes when it sends int.
 * Received bytes should be reversed to reconstruct the sent integer.
 * */
fun ByteArray.reverseAndCastToInt(): Int {
  if (this.size != 4) {
    throw IllegalArgumentException("Cannot convert array of ${this.size} to Int")
  }
  return ByteBuffer.wrap(this.reversedArray()).int
}

/**
 * Converts an integer to 2 byte array as follows:
 * bytes[0] - lowest byte
 * bytes[1] - second lowest byte
 * */
val Int.twoLowestBytes: ByteArray
  get() {
    val b1 = this.toByte()
    val b2 = this.ushr(8).toByte()
    return byteArrayOf(b1, b2)
  }

/**
 * Rsync transforms bytes to int like this:
 * {@code
 *     union {
 *      char *b;
 *      int64 *num;
 *     } u;
 * }
 * Since reading a number works from the end to begin
 * we should send reversed array, pretending rsynk
 * casts bytes to long same way
 */
fun Long.toReversedByteArray(): ByteArray = ByteBuffer.allocate(8).putLong(this).array().reversedArray()

val Byte.Companion.MAX_VALUE_UNSIGNED: Int
  get() = Byte.MAX_VALUE * 2 + 1
