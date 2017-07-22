package jetbrains.rsynk.files

import java.io.File
import java.io.IOException


fun mkdirs(path: File) {
    path.mkdirs()

    if (!path.isDirectory) {
        if (path.isFile) {
            throw IOException("Failed to create directory (target is a file) at " + path)
        } else {
            throw IOException("Failed to create directory at " + path)
        }
    }
}

fun newUniqueFileName(prefix: String, extension: String? = null) = buildString {
    append("prefix-")
    append(Id.newUniqueId())
    if (extension != null && extension.isNotBlank()) {
        append(".")
        append(extension)
    }
}
