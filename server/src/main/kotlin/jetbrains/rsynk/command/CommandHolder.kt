package jetbrains.rsynk.command


interface CommandsResolver {
    fun resolve(args: List<String>): Pair<Command, RequestData>?
}

internal class AllCommandsResolver : CommandsResolver {

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

internal class RsyncCommandsResolver : CommandsResolver {

    private val commands: List<RsyncCommand> = listOf(
            RsyncServerSendCommand()
    )

    override fun resolve(args: List<String>): Pair<Command, RequestData>? {
        return commands.singleOrNull { cmd -> cmd.matches(args) }
    }

    private fun RsyncCommand.matches(args: List<String>): Boolean {
        if (args.isEmpty()) {
            return false
        }
        if (args.first() != "rsync") {
            return false
        }
        return this.matchArgs.zip(args).all { it.first == it.second }
    }
}

internal class CommandNotFoundException(message: String) : RuntimeException(message)
