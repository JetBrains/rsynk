package jetbrains.rsynk.extensions

fun String.dropNewLine(): String {
  if (!this.endsWith('\n')) {
    throw Error("Line $this not ends with new line symbol")
  }
  return this.dropLast(1)
}

fun String.dropNullTerminal(): String {
  if (!this.endsWith('\u0000')) {
    throw Error("Line $this not ends with null terminal symbol")
  }
  return this.dropLast(1)
}

