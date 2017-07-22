package jetbrains.rsynk.files

import java.io.File

data class RsynkFileBoundaries(val offset: Long,
                               val length: Long)

data class RsynkFile(val file: File,
                     val getBoundariesCallback: () -> RsynkFileBoundaries)

data class RequestedAndRealPath(val requestedPath: String,
                                val realPath: String)
