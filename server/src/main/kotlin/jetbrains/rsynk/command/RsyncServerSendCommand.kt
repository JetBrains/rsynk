package jetbrains.rsynk.command

import jetbrains.rsynk.files.FileInfoReader
import jetbrains.rsynk.files.TrackedFilesProvider
import java.io.InputStream
import java.io.OutputStream

internal class RsyncServerSendCommand(fileInfoReader: FileInfoReader,
                                      trackedFiles: TrackedFilesProvider
) : RsyncCommand {

    override fun execute(args: List<String>, stdIn: InputStream, stdOut: OutputStream, stdErr: OutputStream) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun matchArguments(args: List<String>): Boolean {
        if (args.size < 4) {
            return false
        }
        if (args.any { it == "--daemon" || it == "daemon" }) {
            return false
        }
        return args[1] == "--server" && args[2] == "--sender"
    }
}
