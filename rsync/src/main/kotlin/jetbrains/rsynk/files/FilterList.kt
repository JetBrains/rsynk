package jetbrains.rsynk.files

import java.nio.file.Files
import java.nio.file.Path

class FilterList {

    //TODO
    fun include(file: Path): Boolean {
        if (Files.isDirectory(file)) {
            throw IllegalArgumentException("Use 'includeDir' method for directories")
        }
        return true
    }

    //TODO
    fun includeDir(dir: Path): Boolean {
        if (!Files.isDirectory(dir)) {
            throw IllegalArgumentException("Use 'include' method for files")
        }
        return true
    }
}
