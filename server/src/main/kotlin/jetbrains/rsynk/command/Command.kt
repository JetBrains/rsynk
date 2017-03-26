package jetbrains.rsynk.command

import jetbrains.rsynk.checksum.Checksum
import jetbrains.rsynk.io.ReadingIO
import jetbrains.rsynk.io.WritingIO
import jetbrains.rsynk.options.RequestOptions

interface Command {
    fun execute(requestData: RequestData,
                input: ReadingIO,
                output: WritingIO,
                error: WritingIO)
}

data class RequestData(
        val options: RequestOptions,
        val files: List<String>,
        val checksumSeed: Int = Checksum.newSeed()
)

interface RsyncCommand : Command
