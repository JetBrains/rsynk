package jetbrains.rsynk.files

import java.io.File

object FileResolver {

    private val wildcardsInPathPattern = Regex(".*[\\[*?].*")

    fun resolve(path: String): File {
        if (wildcardsInPathPattern.matches(path)) {
            throw UnsupportedOperationException("Cannot expand $path path (not supported)")
        }
        return File(path)
    }
}
