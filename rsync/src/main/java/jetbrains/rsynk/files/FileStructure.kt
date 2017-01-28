package jetbrains.rsynk.files

import java.io.File

data class FileStructure(val mode: Int,   /* The item's type and permissions */
                         val flags: Int,  /* The FLAG_* bits for this item */
                         val file: File) {
  fun mode(newMode: Int): FileStructure {
    return FileStructure(newMode, flags, file)
  }

  fun flags(newFlags: Int): FileStructure {
    return FileStructure(mode, newFlags, file)
  }
}
