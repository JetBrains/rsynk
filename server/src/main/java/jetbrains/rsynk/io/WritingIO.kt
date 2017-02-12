package jetbrains.rsynk.io

interface WritingIO {

  fun writeBytes(bytes: ByteArray, offset: Int, len: Int)

  fun writeBytes(bytes: ByteArray) {
    writeBytes(bytes, 0, bytes.size)
  }
}
