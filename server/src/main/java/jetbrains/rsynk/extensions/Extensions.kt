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
 * Since bytes for int read from the end to begin.
 * Reversed array should be sent, pretending rsynk
 * casts bytes to int same way
 */
fun Int.toReversedByteArray(): ByteArray = ByteBuffer.allocate(4).putInt(this).array().reversedArray()


fun ByteArray.reverseAndCastToInt(): Int {
  if (this.size != 4) {
    throw IllegalArgumentException("Cannot convert array of ${this.size} to Int")
  }
  return ByteBuffer.wrap(this.reversedArray()).int
}
