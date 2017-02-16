package jetbrains.rsynk.io

import java.io.IOException
import java.io.InputStream

class SynchronousReadingIO(private val intput: InputStream) : ReadingIO {

  private val buf = ByteArray(4096)

  /**
   * Blocks until exactly {@code len} bytes are received
   * */
  override fun readBytes(len: Int): ByteArray {
    val bufferToUse = if (len > buf.size) ByteArray(len) else buf
    val read = intput.read(bufferToUse, 0, len)
    if (read <= 0 || read != len) {
      throw IOException("Cannot read requested amount of data: only $read bytes of $len were read")
    }
    return bufferToUse.sliceArray(0..len - 1)
  }
}
