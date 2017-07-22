package jetbrains.rsynk.command

import jetbrains.rsynk.exitvalues.ArgsParingException
import jetbrains.rsynk.files.FileResolver
import jetbrains.rsynk.files.ThingThatCopiesTrackedFileMeaningfulPiece
import jetbrains.rsynk.files.TrackedFilesStorage
import jetbrains.rsynk.process.RsyncServerSendProcess
import jetbrains.rsynk.request.RsyncServerSendRequestParser
import jetbrains.rsynk.settings.RsyncSettings
import mu.KLogging
import java.io.InputStream
import java.io.OutputStream

internal class RsyncServerSendCommand(trackedFiles: TrackedFilesStorage,
                                      private val rsyncSettings: RsyncSettings
) : RsyncCommand {

    companion object : KLogging()

    private val fileResolver = FileResolver(trackedFiles)
    private val thingThatCopiesTrackedFile = ThingThatCopiesTrackedFileMeaningfulPiece(rsyncSettings)

    override fun execute(args: List<String>, stdIn: InputStream, stdOut: OutputStream, stdErr: OutputStream) {
        val request = try {
            RsyncServerSendRequestParser.parse(args)
        } catch (t: Throwable) {
            logger.info({ "Cannot parse request args: ${t.message}" })
            throw ArgsParingException(t.message ?: "Cannot parse arguments")
        }

        val rsynkFiles = fileResolver.resolve(request.files)

        try {
            thingThatCopiesTrackedFile.setupFilesForAction(rsynkFiles) { files ->
                RsyncServerSendProcess(rsyncSettings).execute(files, stdIn, stdOut, stdErr)
            }
        } catch(t: Throwable) {
            logger.error(t, { "Failed to execute rsync server send command: ${t.message}" })
        }
    }

    override fun matchArguments(args: List<String>): Boolean {
        if (args.size < 3) {
            return false
        }
        if (args.any { it == "--daemon" || it == "daemon" }) {
            return false
        }
        return args[1] == "--server" && args[2] == "--sender"
    }
}
