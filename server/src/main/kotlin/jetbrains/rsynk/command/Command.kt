package jetbrains.rsynk.command

import jetbrains.rsynk.checksum.Checksum
import jetbrains.rsynk.io.ReadingIO
import jetbrains.rsynk.io.WriteIO
import jetbrains.rsynk.options.RequestOptions

interface Command {
    fun execute(requestData: RequestData,
                input: ReadingIO,
                output: WriteIO,
                error: WriteIO)
}

data class RequestData(
        val options: RequestOptions,
        val filePaths: List<String>,
        val checksumSeed: Int = Checksum.newSeed()
)

interface RsyncCommand : Command
