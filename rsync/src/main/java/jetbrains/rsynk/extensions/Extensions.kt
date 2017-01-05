package jetbrains.rsynk.extensions

fun String.dropNewLine(): String {
  if (!this.endsWith('\n')) {
    throw Error("String not ends with new line symbol: $this")
  }
  return this.dropLast(1)
}

fun String.dropNullTerminal(): String {
  if (!this.endsWith('\u0000')) {
    throw Error("String not ends with null terminal symbol: $this")
  }
  return this.dropLast(1)
}

