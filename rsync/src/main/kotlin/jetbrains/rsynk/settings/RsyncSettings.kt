package jetbrains.rsynk.settings

import java.io.File

interface RsyncSettings {
    val rsyncPath: String
    val tempDirectory: File
}
