package jetbrains.rsynk.files

import java.io.File
import java.nio.file.Path

object FileResolver {

    private val wildcardsInPathPattern = Regex(".*[\\[*?].*")

    fun resolve(path: String): Path {
        if (wildcardsInPathPattern.matches(path)) {
            throw UnsupportedOperationException("Cannot expand $path path (not supported)")
        }
        return File(path).toPath().normalize()
    }
}
