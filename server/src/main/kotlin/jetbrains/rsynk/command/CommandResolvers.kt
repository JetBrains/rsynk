package jetbrains.rsynk.command

import jetbrains.rsynk.files.FileInfoReader
import jetbrains.rsynk.options.RequestOptions
import jetbrains.rsynk.server.RsyncRequestDataParser


interface CommandsResolver {
    fun resolve(args: List<String>): Pair<Command, RequestData>?
}

class AllCommandsResolver(fileInfoReader: FileInfoReader) : CommandsResolver {

    private val commandHolders = listOf(
            RsyncCommandsResolver(fileInfoReader)
    )

    override fun resolve(args: List<String>): Pair<Command, RequestData> {
        commandHolders.forEach { holder ->
            holder.resolve(args)?.let {
                return it
            }
        }
        throw CommandNotFoundException("Zero or more than one command match given args " +
                args.joinToString(prefix = "[", postfix = "]", separator = ", "))
    }

}

class RsyncCommandsResolver(fileInfoReader: FileInfoReader) : CommandsResolver {

    private val commands: Map<RsyncCommand, (RequestOptions) -> Boolean> = mapOf(
            RsyncServerSendCommand(fileInfoReader) to { options ->
                options.server && options.sender && !options.daemon
            }
    )

    override fun resolve(args: List<String>): Pair<Command, RequestData>? {
        val requestData = try {
            RsyncRequestDataParser.parse(args)
        } catch (t: Throwable) {
            throw InvalidArgumentsException("Cannot parse request arguments: $args.", t)
        }
        for ((command, predicate) in commands) {
            if (predicate(requestData.options)) {
                return Pair(command, requestData)
            }
        }
        return null
    }
}

class CommandNotFoundException(message: String) : RuntimeException(message)
class InvalidArgumentsException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
