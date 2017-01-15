package jetbrains.rsynk.protocol

enum class Option(val textValue: String) {
  APPEND("append"),
  COPY_DEST("copy_dest"),
  COMPARE_DEST("compare_dest"),
  COPY_LINKS("L"),
  CVS_EXCLUDE("C"),
  ITEMSIZE_CHANGES("i"),
  INC_RECURSIVE("inc-recursive"),
  INPLACE("inplace"),
  FILTER("f"),
  FUZZY_BASIS("fuzzy"),
  ONE_FILE_SYSTEM("x"),
  PRESERVE_ACLS("acls"),
  PRESERVE_XATTRS("xattrs"),
  PROTECT_ARGS("s"),
  PRUNE_EMPTY_DIRS("m"), //also prune-empty-dirs  //TODO: check both long and short names for each option
  SENDER("sender"),
  SERVER("server"),
  TIMEOUT("timeout");

  companion object {
    fun find(textValue: String): Option? = Option.values().firstOrNull { it.textValue == textValue }
  }
}