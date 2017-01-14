package jetbrains.rsynk.protocol

enum class Option(val textValue: String) {
  COPY_LINKS("L"),
  CVS_EXCLUDE("C"),
  ITEMSIZE_CHANGES("i"),
  FILTER("f"),
  ONE_FILE_SYSTEM("x"),
  PROTECT_ARGS("s")
}