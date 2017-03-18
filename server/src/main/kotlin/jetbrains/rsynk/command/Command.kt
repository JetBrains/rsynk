package jetbrains.rsynk.command

import jetbrains.rsynk.checksum.Checksum
import jetbrains.rsynk.io.ReadingIO
import jetbrains.rsynk.io.WritingIO
import jetbrains.rsynk.options.Option

internal interface Command {
    fun execute(requestData: RequestData,
                input: ReadingIO,
                output: WritingIO,
                error: WritingIO)
}

internal class RequestOptions(val options: Set<Option>) {
    val delete: Boolean
        get() = options.contains(Option.Delete)
    //TODO: finish implementation
}


internal class RequestData(val options: RequestOptions,
                  val files: List<String>) {
    val checskumSeed = Checksum.nextSeed()
}

internal interface RsyncCommand : Command
