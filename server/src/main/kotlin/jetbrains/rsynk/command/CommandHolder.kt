package jetbrains.rsynk.command


interface CommandsResolver {
    fun resolve(args: List<String>): Pair<Command, RequestData>?
}

class AllCommandsResolver : CommandsResolver {

    private val commandHolders = listOf(
            RsyncCommandsResolver()
    )

    override fun resolve(args: List<String>): Pair<Command, RequestData> {
        commandHolders.forEach { holder ->
            val command = holder.resolve(args)
            if (command != null) {
                return command
            }
        }
        throw CommandNotFoundException("Zero or more than one command match given args " +
                args.joinToString(prefix = "[", postfix = "]", separator = ", "))
    }

}

class RsyncCommandsResolver : CommandsResolver {

    private val commands: Map<RsyncCommand, (RequestOptions) -> Boolean> = mapOf(
            RsyncServerSendCommand() to { options ->
                options.server && options.sender && !options.daemon
            }
    )

    override fun resolve(args: List<String>): Pair<Command, RequestData>? {
        val requestData = try {
            RsyncRequestDataParser.parse(args)
        } catch (t: Throwable) {
            throw InvalidArgumentsException("Cannot parse request arguments: $args")
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
class InvalidArgumentsException(message: String) : RuntimeException(message)
