package jetbrains.rsynk.process

import jetbrains.rsynk.files.RequestedAndRealPath
import jetbrains.rsynk.settings.RsyncSettings
import java.io.InputStream
import java.io.OutputStream

class RsyncServerSendProcess(private val rsyncSettings: RsyncSettings) {
    fun execute(files: List<RequestedAndRealPath>,
                stdin: InputStream,
                stdout: OutputStream,
                stderr: OutputStream) {
        throw UnsupportedOperationException("not implemented")
    }
}
