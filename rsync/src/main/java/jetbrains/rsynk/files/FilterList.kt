package jetbrains.rsynk.files

import java.io.File

class FilterList {

  //TODO
  fun include(file: File): Boolean {
    if (file.isDirectory) {
      throw IllegalArgumentException("Use 'includeDir' method for directories")
    }
    return true
  }

  //TODO
  fun includeDir(dir: File): Boolean {
    if (!dir.isDirectory) {
      throw IllegalArgumentException("Use 'include' method for files")
    }
    return true
  }
}
