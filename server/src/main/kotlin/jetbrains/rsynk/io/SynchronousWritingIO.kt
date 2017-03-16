package jetbrains.rsynk.io

import java.io.OutputStream

class SynchronousWritingIO(private val output: OutputStream) : WritingIO {

  /**
   * Writes {@code bytes} buffer with given {@code offset} and
   * {@len} and forces them to be written
   */
  override fun writeBytes(bytes: ByteArray, offset: Int, len: Int) {
    output.write(bytes, offset, len)
    output.flush()
  }
}
