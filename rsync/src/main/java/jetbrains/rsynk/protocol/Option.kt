package jetbrains.rsynk.protocol

enum class Option(val textValue: String) {
  APPEND("append"),
  COPY_DEST("copy_dest"),
  COMPARE_DEST("compare_dest"),
  COPY_LINKS("L"),
  CVS_EXCLUDE("C"),
  DEVICES("D"),
  FILTER("f"),
  FUZZY_BASIS("fuzzy"),
  GROUP("g"),
  ICONV("iconv"),
  ITEMSIZE_CHANGES("i"),
  INC_RECURSE("inc-recursive"),
  INPLACE("inplace"),
  LINKS("l"),
  ONE_FILE_SYSTEM("x"),
  OWNER("o"),
  PERMS("p"),
  PRESERVE_ACLS("acls"),
  PRESERVE_XATTRS("xattrs"),
  PROTECT_ARGS("s"),
  PRUNE_EMPTY_DIRS("m"), //also prune-empty-dirs  //TODO: check both long and short names for each option
  RECURSIVE("r"),
  RSH("e"),
  SENDER("sender"),
  SERVER("server"),
  TIMEOUT("timeout"),
  TIMES("t"),
  VERBOSE("v");

  companion object {
    fun find(textValue: String): Option? = Option.values().firstOrNull { it.textValue == textValue }
  }
}