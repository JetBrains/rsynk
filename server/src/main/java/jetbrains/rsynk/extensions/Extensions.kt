package jetbrains.rsynk.extensions

import java.nio.ByteBuffer

fun String.dropNewLine(): String {
  if (!endsWith('\n')) {
    throw Error("String not ends with new line symbol: $this")
  }
  return dropLast(1)
}


fun String.endsWithNullTerminal(): Boolean = endsWith('\u0000')

fun String.dropNullTerminal(): String {
  if (!endsWithNullTerminal()) {
    throw Error("String not ends with null terminal symbol: $this")
  }
  return dropLast(1)
}

fun Int.toByteArray(): ByteArray = ByteBuffer.allocate(4).putInt(this).array()
