package jetbrains.rsynk.io

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

class IOSession(private val input: InputStream, private val output: OutputStream) {

  fun writeInt(int: Int) {
    val bytes = ByteBuffer.allocate(4).putInt(int).array()
    writeBytes(bytes, 0, 4)
  }

  fun readInt(): Int {
    val bytes = ByteArray(4)
    readBytes(bytes, 0, bytes.size)
    return ByteBuffer.wrap(bytes).int
  }

  fun writeByte(byte: Byte) {
    output.write(byteArrayOf(byte), 0, 1)
  }

  fun readByte(): Byte {
    val bytes = ByteArray(1)
    val read = readBytes(bytes, 0, 1)
    if (read < 1) {
      throw IOException("Expected to read 1 byte, but read $read")
    }
    return bytes[0]
  }

  fun writeBytes(bytes: ByteArray, offset: Int, length: Int) {
    output.write(bytes, offset, length)
    output.flush()
  }

  fun readBytes(): ByteArray {
    val available = input.available()
    if (available == 0) {
      return byteArrayOf()
    }
    val bytes = ByteArray(available)
    val read = readBytes(bytes, 0, bytes.size)
    if (read < available) {
      return bytes.sliceArray(0..read - 1)
    }
    return bytes
  }

  fun readBytes(size: Int): ByteArray {
    val bytes = ByteArray(size)
    readBytes(bytes, 0, size)
    return bytes
  }

  fun writeString(str: String) {
    val bytes = str.toByteArray()
    writeBytes(bytes, 0, bytes.size)
  }

  fun readString(): String = String(readBytes(), Charsets.UTF_8)

  private fun readBytes(bytes: ByteArray, offset: Int, len: Int): Int {
    return input.read(bytes, offset, len)
  }
}