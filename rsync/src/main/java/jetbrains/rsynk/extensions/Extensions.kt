package jetbrains.rsynk.extensions

fun String.dropNewLine(): String {
  if (!this.endsWith('\n')) {
    throw Error("Line $this not ends with new line")
  }
  return this.drop(1)
}