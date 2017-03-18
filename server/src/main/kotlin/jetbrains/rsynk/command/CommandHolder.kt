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

private data class RsyncCommandArgs(val args: List<String>) {
    fun match(args: List<String>): Boolean {
        if (args.isEmpty()) {
            return false
        }
        if (args.first() != "rsync") {
            return false
        }
        return this.args.zip(args).all { it.first == it.second }
    }
}

class RsyncCommandsResolver : CommandsResolver {

    private val commands: Map<RsyncCommand, RsyncCommandArgs> = mapOf(
            RsyncServerSendCommand() to RsyncCommandArgs(listOf("rsync", "--server", "--sender"))
    )

    override fun resolve(args: List<String>): Pair<Command, RequestData>? {
        val (command, commandArgs) = commands.map { (k, v) -> Pair(k, v) }
                .singleOrNull { (_, cmdArgs) -> cmdArgs.match(args) } ?: return null
        val requestData = parseRequestData(commandArgs, args)
        return Pair(command, requestData)
    }

    private fun parseRequestData(commandArgs: RsyncCommandArgs, args: List<String>): RequestData {
        val nonCommandArgs = args.removeCommandArgs(commandArgs)
        throw UnsupportedOperationException()
    }

    private fun List<String>.removeCommandArgs(commandArgs: RsyncCommandArgs): List<String> {
        return this.zip(commandArgs.args).dropWhile { it.first == it.second }.unzip().first
    }
}

class CommandNotFoundException(message: String) : RuntimeException(message)
