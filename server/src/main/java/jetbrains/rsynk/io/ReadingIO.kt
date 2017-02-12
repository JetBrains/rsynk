package jetbrains.rsynk.io

import java.nio.ByteBuffer

interface ReadingIO {

  fun readBytes(len: Int): ByteArray

  fun readInt(): Int {
    val bytes = readBytes(4)
    return ByteBuffer.wrap(bytes).int
  }
}
