package jetbrains.rsynk.protocol

enum class Option(val textValue: String) {
  COPY_LINKS("L"),
  CVS_EXCLUDE("C"),
  ITEMSIZE_CHANGES("i"),
  FILTER("f"),
  ONE_FILE_SYSTEM("x"),
  PROTECT_ARGS("s"),
  SENDER("sender"),
  SERVER("server");

  companion object {
    fun find(textValue: String): Option? = Option.values().firstOrNull { it.textValue == textValue }
  }
}